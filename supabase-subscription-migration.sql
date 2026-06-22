-- =============================================
-- SUBSCRIPTION MIGRATION
-- Run in Supabase SQL Editor
-- =============================================

ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS subscribed_until DATE;
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS plan_name VARCHAR(50);

-- When admin sets someone as premium, also set their expiry.
-- Example: manually grant 1 month premium to a user:
-- UPDATE public.profiles
-- SET is_premium = true,
--     subscribed_until = CURRENT_DATE + INTERVAL '30 days',
--     plan_name = '1 Month'
-- WHERE id = '<user-uuid>';

-- Auto-expire premium: set is_premium = false when subscribed_until has passed.
-- You can run this as a scheduled job in Supabase (pg_cron extension):
-- SELECT cron.schedule('expire-premium', '0 2 * * *', $$
--   UPDATE public.profiles
--   SET is_premium = false
--   WHERE is_premium = true
--     AND subscribed_until IS NOT NULL
--     AND subscribed_until < CURRENT_DATE;
-- $$);
