-- =============================================
-- GOVTPREP - SCHEMA FIXES & RPC FUNCTIONS
-- =============================================
-- Run this in Supabase SQL Editor
-- Safe to run multiple times. Does NOT touch your exam/question data.

-- Drop existing functions first (required if return type changed)
DROP FUNCTION IF EXISTS public.get_test_questions(UUID);
DROP FUNCTION IF EXISTS public.submit_test_answers(UUID, JSONB, INTEGER);
DROP FUNCTION IF EXISTS public.get_user_stats(UUID);

-- =============================================
-- 1. FIX PROFILES TABLE
-- =============================================
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS full_name VARCHAR(100);
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS email VARCHAR(255);
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS is_admin BOOLEAN DEFAULT false;
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS is_premium BOOLEAN DEFAULT false;
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS target_exam VARCHAR(100);

UPDATE public.profiles SET full_name = name WHERE full_name IS NULL AND name IS NOT NULL;

CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO public.profiles (id, name, full_name, email)
    VALUES (
        NEW.id,
        COALESCE(NEW.raw_user_meta_data->>'full_name', NEW.raw_user_meta_data->>'name', split_part(NEW.email, '@', 1)),
        COALESCE(NEW.raw_user_meta_data->>'full_name', NEW.raw_user_meta_data->>'name', split_part(NEW.email, '@', 1)),
        NEW.email
    );
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();

-- =============================================
-- 2. FIX TEST_SETS TABLE
-- =============================================
ALTER TABLE public.test_sets ADD COLUMN IF NOT EXISTS is_free BOOLEAN DEFAULT true;
UPDATE public.test_sets SET is_free = NOT COALESCE(is_premium, false) WHERE is_free IS NULL;

-- FIX: Drop conflicting CHECK constraint on test_type
-- ssc-structure.sql sets CHECK ('full_mock','subject_wise')
-- ssc-gd-setup.sql needs CHECK ('full_mock','subject','topic','previous_year')
-- This fixes the conflict so all test_type values are accepted
DO $$
DECLARE
    constraint_name TEXT;
BEGIN
    -- Find and drop any CHECK constraint on test_type column
    FOR constraint_name IN
        SELECT con.conname
        FROM pg_constraint con
        JOIN pg_attribute att ON att.attnum = ANY(con.conkey) AND att.attrelid = con.conrelid
        WHERE con.conrelid = 'public.test_sets'::regclass
        AND att.attname = 'test_type'
        AND con.contype = 'c'
    LOOP
        EXECUTE format('ALTER TABLE public.test_sets DROP CONSTRAINT %I', constraint_name);
        RAISE NOTICE 'Dropped constraint: %', constraint_name;
    END LOOP;
END $$;

-- Add unified CHECK that accepts all valid test_type values
ALTER TABLE public.test_sets ADD CONSTRAINT test_sets_test_type_check
    CHECK (test_type IN ('full_mock', 'subject', 'subject_wise', 'topic', 'previous_year', 'practice'));

-- =============================================
-- 3. CREATE get_test_questions RPC
-- =============================================
CREATE OR REPLACE FUNCTION public.get_test_questions(p_test_set_id UUID)
RETURNS TABLE (
    id UUID,
    test_set_id UUID,
    question_number INTEGER,
    question_text TEXT,
    question_text_hindi TEXT,
    question_image_url TEXT,
    options JSONB,
    marks DECIMAL,
    negative_marks DECIMAL,
    topic VARCHAR,
    difficulty VARCHAR
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        q.id, q.test_set_id, q.question_number,
        q.question_text, q.question_text_hindi, q.question_image_url,
        q.options, q.marks, q.negative_marks, q.topic, q.difficulty
    FROM public.questions q
    WHERE q.test_set_id = p_test_set_id
    ORDER BY q.question_number;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- =============================================
-- 4. CREATE submit_test_answers RPC
-- =============================================
CREATE OR REPLACE FUNCTION public.submit_test_answers(
    p_test_set_id UUID,
    p_answers JSONB,
    p_time_spent_seconds INTEGER
)
RETURNS JSONB AS $$
DECLARE
    v_user_id UUID;
    v_correct INTEGER := 0;
    v_incorrect INTEGER := 0;
    v_unattempted INTEGER := 0;
    v_score DECIMAL := 0;
    v_total_marks DECIMAL := 0;
    v_attempt_id UUID;
    v_question RECORD;
    v_selected_id TEXT;
    v_review_questions JSONB := '[]'::JSONB;
BEGIN
    v_user_id := auth.uid();
    IF v_user_id IS NULL THEN
        RAISE EXCEPTION 'Not authenticated';
    END IF;

    FOR v_question IN 
        SELECT q.id, q.question_number, q.question_text, q.question_text_hindi,
               q.question_image_url, q.options, q.correct_option_id,
               q.explanation, q.explanation_hindi, q.marks, q.negative_marks
        FROM public.questions q
        WHERE q.test_set_id = p_test_set_id
        ORDER BY q.question_number
    LOOP
        v_total_marks := v_total_marks + v_question.marks;
        v_selected_id := p_answers->v_question.id::TEXT->>'selectedOptionId';

        IF v_selected_id IS NOT NULL AND v_selected_id != '' THEN
            IF v_selected_id = v_question.correct_option_id THEN
                v_correct := v_correct + 1;
                v_score := v_score + v_question.marks;
            ELSE
                v_incorrect := v_incorrect + 1;
                v_score := v_score - v_question.negative_marks;
            END IF;
        ELSE
            v_unattempted := v_unattempted + 1;
        END IF;

        v_review_questions := v_review_questions || jsonb_build_object(
            'id', v_question.id,
            'question_number', v_question.question_number,
            'question_text', v_question.question_text,
            'question_text_hindi', v_question.question_text_hindi,
            'question_image_url', v_question.question_image_url,
            'options', v_question.options,
            'correct_option_id', v_question.correct_option_id,
            'explanation', v_question.explanation,
            'explanation_hindi', v_question.explanation_hindi,
            'marks', v_question.marks,
            'negative_marks', v_question.negative_marks
        );
    END LOOP;

    IF v_score < 0 THEN v_score := 0; END IF;

    INSERT INTO public.attempts (
        user_id, test_set_id, answers, score, total_marks,
        correct_count, incorrect_count, unattempted_count,
        time_spent_seconds, status, submitted_at
    ) VALUES (
        v_user_id, p_test_set_id, p_answers, v_score, v_total_marks,
        v_correct, v_incorrect, v_unattempted,
        p_time_spent_seconds, 'completed', NOW()
    )
    RETURNING id INTO v_attempt_id;

    RETURN jsonb_build_object(
        'attempt_id', v_attempt_id,
        'correct', v_correct,
        'incorrect', v_incorrect,
        'unattempted', v_unattempted,
        'score', v_score,
        'total_marks', v_total_marks,
        'percentage', CASE WHEN v_total_marks > 0 THEN ROUND((v_score / v_total_marks * 100)::DECIMAL, 2) ELSE 0 END,
        'time_taken', p_time_spent_seconds,
        'questions', v_review_questions
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- =============================================
-- 5. CREATE get_user_stats RPC
-- =============================================
CREATE OR REPLACE FUNCTION public.get_user_stats(p_user_id UUID)
RETURNS TABLE (
    total_attempts BIGINT,
    average_score DECIMAL,
    total_time_minutes BIGINT,
    tests_this_week BIGINT
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        COUNT(*)::BIGINT,
        COALESCE(ROUND(AVG(CASE WHEN a.total_marks > 0 THEN a.score / a.total_marks * 100 ELSE 0 END)::DECIMAL, 2), 0),
        COALESCE(SUM(a.time_spent_seconds) / 60, 0)::BIGINT,
        COUNT(*) FILTER (WHERE a.created_at > NOW() - INTERVAL '7 days')::BIGINT
    FROM public.attempts a
    WHERE a.user_id = p_user_id
    AND a.status = 'completed';
END;
$$ LANGUAGE plpgsql;

-- =============================================
-- 6. VERIFY: Check if test_sets exist for SSC GD
-- =============================================
-- Run this after the migration to check if tests exist:
-- SELECT ts.title, ts.test_type, ts.is_active, s.name as subject_name
-- FROM test_sets ts JOIN subjects s ON ts.subject_id = s.id
-- WHERE s.sub_exam = 'SSC GD Constable';
--
-- If this returns 0 rows, re-run ssc-gd-setup.sql
-- The CHECK constraint is now fixed so it will work.

-- =============================================
-- DONE!
-- =============================================
