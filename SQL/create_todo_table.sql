
--uuid 확장 설치
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
--실테 테이블 생성
CREATE TABLE IF NOT EXISTS todo (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
--인덱스 설정
CREATE INDEX IF NOT EXISTS idx_todo ON todo (completed);
