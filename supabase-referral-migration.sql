-- =============================================
-- REFERRAL SYSTEM MIGRATION
-- Run in Supabase SQL Editor AFTER the main supabase-migration.sql
-- Safe to run multiple times.
-- =============================================

-- 1. Add referral columns to profiles
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS referral_code VARCHAR(20) UNIQUE;
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS referral_count INTEGER DEFAULT 0;
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS coins INTEGER DEFAULT 0;
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS referred_by VARCHAR(20);

-- 2. Backfill referral codes for existing users (safe, skips any that already have one)
UPDATE public.profiles
SET referral_code = UPPER(SUBSTRING(COALESCE(name, 'USER'), 1, 6))
    || LPAD(FLOOR(RANDOM() * 9000 + 1000)::TEXT, 4, '0')
WHERE referral_code IS NULL;

-- 3. Update handle_new_user trigger to auto-generate referral code on signup
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
DECLARE
    v_name TEXT;
    v_code TEXT;
BEGIN
    v_name := COALESCE(
        NEW.raw_user_meta_data->>'full_name',
        split_part(NEW.email, '@', 1),
        'USER'
    );
    v_code := UPPER(SUBSTRING(v_name, 1, 6))
              || LPAD(FLOOR(RANDOM() * 9000 + 1000)::TEXT, 4, '0');

    INSERT INTO public.profiles (id, name, full_name, email, referral_code)
    VALUES (NEW.id, v_name, v_name, NEW.email, v_code)
    ON CONFLICT (id) DO NOTHING;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 4. Create apply_referral RPC
DROP FUNCTION IF EXISTS public.apply_referral(TEXT);

CREATE OR REPLACE FUNCTION public.apply_referral(p_referral_code TEXT)
RETURNS BOOLEAN AS $$
DECLARE
    v_referrer_id UUID;
    v_caller_id   UUID;
BEGIN
    v_caller_id := auth.uid();
    IF v_caller_id IS NULL THEN RETURN FALSE; END IF;

    -- Find referrer; block self-referral
    SELECT id INTO v_referrer_id
    FROM public.profiles
    WHERE referral_code = UPPER(p_referral_code)
      AND id <> v_caller_id;

    IF v_referrer_id IS NULL THEN RETURN FALSE; END IF;

    -- Mark caller as referred (once only)
    UPDATE public.profiles
    SET referred_by = UPPER(p_referral_code)
    WHERE id = v_caller_id
      AND referred_by IS NULL;

    IF NOT FOUND THEN RETURN FALSE; END IF;  -- already used a code

    -- Reward referrer: +50 coins, +1 referral count
    UPDATE public.profiles
    SET referral_count = referral_count + 1,
        coins          = coins + 50
    WHERE id = v_referrer_id;

    RETURN TRUE;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
