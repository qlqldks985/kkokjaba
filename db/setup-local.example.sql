-- 로컬 MySQL 준비 스크립트 (예시)
-- 1) 이 파일을 db/setup-local.sql 로 복사하고 CHANGE_ME 를 원하는 비밀번호로 바꾸세요. (setup-local.sql 은 git에 올라가지 않습니다)
-- 2) MySQL Workbench 등에서 root 계정으로 한 번만 실행하세요.
-- 앱 전용 계정은 kkokjaba, kkokjaba_test 두 DB에만 권한을 가집니다 (최소 권한).
CREATE DATABASE IF NOT EXISTS kkokjaba      DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS kkokjaba_test DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER IF NOT EXISTS 'kkokjaba'@'localhost' IDENTIFIED BY 'CHANGE_ME';
GRANT ALL PRIVILEGES ON kkokjaba.*      TO 'kkokjaba'@'localhost';
GRANT ALL PRIVILEGES ON kkokjaba_test.* TO 'kkokjaba'@'localhost';
FLUSH PRIVILEGES;
