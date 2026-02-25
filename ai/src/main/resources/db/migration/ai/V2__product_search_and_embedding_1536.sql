-- V2__product_search_and_embedding_1536.sql
-- 1) PGroonga + product_search 테이블 생성 (하이브리드 검색용)
-- 2) ai_memory.embedding 차원 384 → 1536 변경

-- === product_search 테이블 ===
CREATE EXTENSION IF NOT EXISTS pgroonga;

CREATE TABLE IF NOT EXISTS product_search (
    id UUID PRIMARY KEY,
    content TEXT NOT NULL,
    metadata JSONB,
    embedding vector(1536)
);

CREATE INDEX IF NOT EXISTS idx_product_search_content
    ON product_search USING pgroonga (content);
CREATE INDEX IF NOT EXISTS idx_product_search_embedding
    ON product_search USING hnsw (embedding vector_cosine_ops);

-- === ai_memory.embedding 차원 변경 ===
-- 기존 384차원 데이터는 호환 불가하므로 NULL 처리 후 타입 변경
DROP INDEX IF EXISTS idx_ai_memory_embedding;
UPDATE ai_memory SET embedding = NULL WHERE embedding IS NOT NULL;
ALTER TABLE ai_memory ALTER COLUMN embedding TYPE vector(1536);
CREATE INDEX idx_ai_memory_embedding
    ON ai_memory USING hnsw (embedding vector_cosine_ops);
