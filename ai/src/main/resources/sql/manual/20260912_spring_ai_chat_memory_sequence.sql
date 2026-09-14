-- Spring AI 2.0.0-M1 -> 2.0.1, PostgreSQL 전용.
-- 기존 AI 인스턴스를 중지한 뒤, 새 버전을 처음 기동하기 전에 AI DB에 실행한다.
-- 기존 대화와 timestamp는 보존한다. 같은 timestamp의 원래 순서는 복원할 수 없다.
-- 신규 DB에는 테이블이 없으므로 생략되며 Spring AI가 기동 시 새 스키마를 만든다.
BEGIN;
SET LOCAL lock_timeout = '15s';
SET LOCAL statement_timeout = '120s';

DO $$
BEGIN
    IF to_regclass('spring_ai_chat_memory') IS NOT NULL THEN
        LOCK TABLE SPRING_AI_CHAT_MEMORY IN ACCESS EXCLUSIVE MODE;

        ALTER TABLE SPRING_AI_CHAT_MEMORY ADD COLUMN IF NOT EXISTS sequence_id BIGINT;

        -- 재실행 시 이미 부여된 순서는 유지한다.
        IF EXISTS (SELECT 1 FROM SPRING_AI_CHAT_MEMORY WHERE sequence_id IS NULL) THEN
            WITH ordered AS (
                SELECT ctid,
                       ROW_NUMBER() OVER (
                           PARTITION BY conversation_id
                           ORDER BY sequence_id NULLS LAST, "timestamp", ctid
                       ) - 1 AS seq
                FROM SPRING_AI_CHAT_MEMORY
            )
            UPDATE SPRING_AI_CHAT_MEMORY AS memory
            SET sequence_id = ordered.seq
            FROM ordered
            WHERE memory.ctid = ordered.ctid;
        END IF;

        ALTER TABLE SPRING_AI_CHAT_MEMORY ALTER COLUMN sequence_id SET NOT NULL;

        -- 구버전은 sequence_id 없이 INSERT하므로 순번 기본값으로 공존을 지원한다.
        -- 2.0.1은 대화별 sequence_id를 직접 지정하므로 이 기본값을 사용하지 않는다.
        CREATE SEQUENCE IF NOT EXISTS spring_ai_chat_memory_legacy_sequence;
        ALTER SEQUENCE spring_ai_chat_memory_legacy_sequence
            OWNED BY SPRING_AI_CHAT_MEMORY.sequence_id;
        PERFORM setval('spring_ai_chat_memory_legacy_sequence', GREATEST(
            COALESCE((SELECT MAX(sequence_id) FROM SPRING_AI_CHAT_MEMORY), 0),
            (SELECT last_value FROM spring_ai_chat_memory_legacy_sequence)
        ), true);
        ALTER TABLE SPRING_AI_CHAT_MEMORY ALTER COLUMN sequence_id
            SET DEFAULT nextval('spring_ai_chat_memory_legacy_sequence');

        CREATE INDEX IF NOT EXISTS SPRING_AI_CHAT_MEMORY_CONVERSATION_ID_SEQUENCE_ID_IDX
            ON SPRING_AI_CHAT_MEMORY(conversation_id, sequence_id);
    END IF;
END
$$;

COMMIT;
