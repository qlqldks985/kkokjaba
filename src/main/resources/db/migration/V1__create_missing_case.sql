-- 미아 사건 테이블.
-- 한 번 적용된 이 파일은 절대 고치지 않는다. 구조를 바꿀 때는 V2__...sql을 새로 만든다.
-- (Flyway가 파일마다 체크섬을 저장해 두기 때문에, 고치면 앱이 뜰 때 바로 오류가 난다.)

CREATE TABLE missing_case (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    -- MySQL ENUM이 아니라 VARCHAR로 둔다.
    -- ENUM이면 단계를 하나 추가할 때마다 ALTER TABLE이 필요하고, 순서를 바꾸면 기존 값이 꼬일 수 있다.
    -- 허용 값은 자바의 CaseStatus가 책임진다.
    status         VARCHAR(20)  NOT NULL,
    clothing_color VARCHAR(20)  NOT NULL,
    last_seen_zone VARCHAR(40)  NOT NULL,
    guardian_band  VARCHAR(20)  NULL,
    assigned_staff VARCHAR(40)  NULL,
    created_at     DATETIME(6)  NOT NULL,
    updated_at     DATETIME(6)  NOT NULL,
    version        BIGINT       NOT NULL,
    PRIMARY KEY (id),
    -- 관제실 목록 쿼리(진행 중인 상태만, 최근 순)에 맞춘 인덱스
    INDEX idx_missing_case_status_created (status, created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
