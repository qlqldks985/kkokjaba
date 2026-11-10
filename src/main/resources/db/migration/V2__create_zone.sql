-- 행사장 구역과 캠 배치.
-- 지금까지 화면 코드(app.js)에만 있던 정보를 DB로 옮겨, 서버가 "없는 구역", "캠 없는 구역의 캠 감지"를 거절할 수 있게 한다.

CREATE TABLE zone (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    name          VARCHAR(40) NOT NULL,
    shelter       BIT(1)      NOT NULL,          -- 미아 보호소 구역인가
    display_order INT         NOT NULL,          -- 접수 폼 드롭다운 순서
    PRIMARY KEY (id),
    UNIQUE KEY uk_zone_name (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- 구역 경계(다각형)의 꼭짓점. seq 순서대로 이으면 다각형이 된다.
CREATE TABLE zone_boundary_point (
    zone_id BIGINT NOT NULL,
    seq     INT    NOT NULL,
    lat     DOUBLE NOT NULL,
    lng     DOUBLE NOT NULL,
    PRIMARY KEY (zone_id, seq),
    CONSTRAINT fk_boundary_zone FOREIGN KEY (zone_id) REFERENCES zone (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- 구역에 설치된 캠. 캠이 한 대도 없는 구역은 캠 감지를 받을 수 없다.
CREATE TABLE zone_camera (
    zone_id BIGINT      NOT NULL,
    code    VARCHAR(20) NOT NULL,
    lat     DOUBLE      NOT NULL,
    lng     DOUBLE      NOT NULL,
    PRIMARY KEY (code),
    CONSTRAINT fk_camera_zone FOREIGN KEY (zone_id) REFERENCES zone (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
