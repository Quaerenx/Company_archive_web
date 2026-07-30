-- 월별 고객 응대 기록 테이블 생성
CREATE TABLE IF NOT EXISTS monthly_customer_response (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_name VARCHAR(100) NOT NULL COMMENT '작성자',
    response_date DATE NOT NULL COMMENT '응대 날짜',
    customer_name VARCHAR(200) NOT NULL COMMENT '고객명',
    reason VARCHAR(500) NOT NULL COMMENT '응대 사유',
    action_content TEXT COMMENT '조치 내용',
    note TEXT COMMENT '비고',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    INDEX idx_user_date (user_name, response_date),
    INDEX idx_response_date (response_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='월별 고객 응대 기록';

-- 초기 데이터 예시 (선택사항)
-- INSERT INTO monthly_customer_response (user_name, response_date, customer_name, reason, action_content, note)
-- VALUES 
-- ('홍길동', '2026-01-15', 'ABC 회사', '시스템 장애 문의', '원격 접속하여 로그 확인 및 재시작 처리', '정상 작동 확인'),
-- ('홍길동', '2026-01-20', 'XYZ 기업', '성능 개선 요청', '데이터베이스 쿼리 최적화 및 인덱스 추가', '응답 속도 30% 개선');
