-- company_users 테이블에 department 컬럼 추가
-- Vertica 데이터베이스용

-- 1. department 컬럼이 없는 경우 추가
ALTER TABLE company_users ADD COLUMN department VARCHAR(100);

-- 2. 기존 사용자들의 department 기본값 설정 (선택사항)
-- UPDATE company_users SET department = '미지정' WHERE department IS NULL;

-- 3. 컬럼 추가 확인
SELECT column_name, data_type, character_maximum_length 
FROM columns 
WHERE table_name = 'company_users' 
ORDER BY ordinal_position;
