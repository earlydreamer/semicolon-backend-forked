-- Gemini retrieval-document 벡터의 생성 프로필을 기록한다.
-- 기존 content, id, embedding은 보존하며 컬럼은 기존/신규 DB 모두에서 재실행할 수 있다.
ALTER TABLE IF EXISTS public.ai_user_memory ADD COLUMN IF NOT EXISTS embedding_profile varchar(160);
ALTER TABLE IF EXISTS public.product_search ADD COLUMN IF NOT EXISTS embedding_profile varchar(160);