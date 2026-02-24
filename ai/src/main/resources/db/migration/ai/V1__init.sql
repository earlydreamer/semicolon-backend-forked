-- V1__init.sql (ai)
-- 최소 스키마: ai_memory (pgvector 컬럼 포함)

-- pgvector 확장 설치 (Postgres 클러스터에서 권한 필요)
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS ai_memory (
  id serial PRIMARY KEY,
  uuid uuid NOT NULL,
  user_uuid uuid NOT NULL,
  memory_type varchar(50) NOT NULL,
  sub_type varchar(50) NOT NULL,
  content text NOT NULL,
  embedding vector(384),
  importance_score double precision,
  confidence_score double precision,
  access_count integer DEFAULT 0,
  source_message_id varchar(255),
  created_at timestamptz,
  updated_at timestamptz
);

CREATE INDEX IF NOT EXISTS idx_ai_memory_user_uuid ON ai_memory(user_uuid);
-- ivfflat 인덱스는 approximate 검색용이며 운영 설정에 따라 조정 필요
CREATE INDEX IF NOT EXISTS idx_ai_memory_embedding ON ai_memory USING ivfflat (embedding) WITH (lists=100);
