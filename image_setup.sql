-- ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
-- IMAGE SUPPORT — Run this in Supabase SQL Editor
-- ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

-- 1. Add question_image_url column (if not exists)
ALTER TABLE public.questions ADD COLUMN IF NOT EXISTS question_image_url TEXT;

-- 2. Create storage bucket for question images
-- ⚠️ Do this MANUALLY in Supabase Dashboard:
--   → Storage → New Bucket → Name: "question-images" → Public: ON

-- 3. Storage policy — allow authenticated uploads, public reads
INSERT INTO storage.buckets (id, name, public)
VALUES ('question-images', 'question-images', true)
ON CONFLICT (id) DO UPDATE SET public = true;

-- Allow authenticated users to upload
CREATE POLICY "Allow authenticated uploads"
ON storage.objects FOR INSERT
TO authenticated
WITH CHECK (bucket_id = 'question-images');

-- Allow authenticated users to update their uploads
CREATE POLICY "Allow authenticated updates"
ON storage.objects FOR UPDATE
TO authenticated
USING (bucket_id = 'question-images');

-- Allow public reads (for displaying images in app)
CREATE POLICY "Allow public reads"
ON storage.objects FOR SELECT
TO public
USING (bucket_id = 'question-images');

-- Verify:
-- SELECT * FROM storage.buckets WHERE id = 'question-images';
