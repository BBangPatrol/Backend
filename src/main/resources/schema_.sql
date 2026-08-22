-- =============================================================
-- bbangpatrol 로컬 개발용 스키마
-- 2026-07-25
--
-- 사용법: 로컬 DB에서 실행 (기존 테이블 전부 DROP 후 재생성)
--   mysql -u <user> -p <database> < schema.sql
--
-- =============================================================

SET FOREIGN_KEY_CHECKS = 0;

use bbangpatrol;

DROP TABLE IF EXISTS visit_detail;
DROP TABLE IF EXISTS visits;
DROP TABLE IF EXISTS point_history;
DROP TABLE IF EXISTS mission_progress;
DROP TABLE IF EXISTS user_item;
DROP TABLE IF EXISTS bookmark;
DROP TABLE IF EXISTS sig_image;
DROP TABLE IF EXISTS bakery_image;
DROP TABLE IF EXISTS review_like;
DROP TABLE IF EXISTS review_keyword;
DROP TABLE IF EXISTS review_image;
DROP TABLE IF EXISTS review;
DROP TABLE IF EXISTS mission;
DROP TABLE IF EXISTS keyword;
DROP TABLE IF EXISTS item;
DROP TABLE IF EXISTS bakery;
DROP TABLE IF EXISTS user;

SET FOREIGN_KEY_CHECKS = 1;

-- =============================================================
-- user
-- =============================================================
CREATE TABLE user (
                      id              BIGINT       NOT NULL AUTO_INCREMENT,
                      name            VARCHAR(50)  NOT NULL,
                      email           VARCHAR(255) NULL,
                      kakao_id        VARCHAR(100) NOT NULL,
                      `rank`          INT          NOT NULL default 0,
                      point_balance   INT          NOT NULL default 0,
                      role            VARCHAR(10)  NULL,
                      refresh_token   VARCHAR(255) NULL,
                      created_at      DATETIME     NOT NULL	default current_timestamp,
                      deleted_at      DATETIME     NULL,
                      user_image      VARCHAR(255) NULL,
                      PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================
-- bakery
-- =============================================================
CREATE TABLE bakery (
                        id              BIGINT        NOT NULL AUTO_INCREMENT,
                        name            VARCHAR(100)  NOT NULL,
                        region          VARCHAR(255)  NULL,
                        address         VARCHAR(255)  NULL,
                        lat             DECIMAL(10,7) NULL,
                        lng             DECIMAL(10,7) NULL,
                        phone           VARCHAR(20)   NULL,
                        hours           VARCHAR(100)  NULL,
                        avg_rating      DECIMAL(2,1)  NULL,
                        signature_menu  VARCHAR(255)  NULL,
                        created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_at      DATETIME      NULL,
                        deleted_at      DATETIME      NULL,
                        summary         VARCHAR(255)  NULL,
                        content         TEXT          NULL,
                        PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================
-- item
-- =============================================================
CREATE TABLE item (
                      id          BIGINT       NOT NULL AUTO_INCREMENT,
                      name        VARCHAR(100) NOT NULL,
                      image_url   TEXT         NULL,
                      created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                      PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================
-- keyword
-- =============================================================
CREATE TABLE keyword (
                         id     BIGINT   NOT NULL AUTO_INCREMENT,
                         label  VARCHAR(30) NULL,
                         PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================
-- mission
-- =============================================================
CREATE TABLE mission (
                         id            BIGINT       NOT NULL AUTO_INCREMENT,
                         reward_point  INT          NULL,
                         title         VARCHAR(100) NOT NULL,
                         description   TINYTEXT     NULL,
                         mission_type	ENUM('receipt', 'review', 'bakery', 'collection'),
                         region        VARCHAR(255) NOT NULL,
                         target_count  INT          NOT NULL,
                         start_date    DATE         NULL,
                         end_date      DATE         NULL,
                         created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================
-- review
-- =============================================================
CREATE TABLE review (
                        id          BIGINT       NOT NULL AUTO_INCREMENT,
                        rating      tinyint      NOT NULL,
                        content     TINYTEXT     NULL,
                        like_count  INT          NOT NULL,
                        created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        deleted_at  DATETIME     NULL,
                        user_id     BIGINT       NOT NULL,
                        bakery_id   BIGINT       NOT NULL,
                        PRIMARY KEY (id),
                        KEY idx_review_user (user_id),
                        KEY idx_review_bakery (bakery_id),
                        CONSTRAINT fk_review_user FOREIGN KEY (user_id) REFERENCES user (id),
                        CONSTRAINT fk_review_bakery FOREIGN KEY (bakery_id) REFERENCES bakery (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================
-- review_image
-- =============================================================
CREATE TABLE review_image (
                              id         BIGINT   NOT NULL AUTO_INCREMENT,
                              origin     TINYTEXT NULL,
                              image_url  TEXT     NULL,
                              review_id  BIGINT   NOT NULL,
                              PRIMARY KEY (id),
                              KEY idx_review_image_review (review_id),
                              CONSTRAINT fk_review_image_review FOREIGN KEY (review_id) REFERENCES review (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================
-- review_keyword
-- =============================================================
CREATE TABLE review_keyword (
                                id          BIGINT NOT NULL AUTO_INCREMENT,
                                review_id   BIGINT NOT NULL,
                                keyword_id  BIGINT NULL,
                                PRIMARY KEY (id),
                                KEY idx_review_keyword_review (review_id),
                                KEY idx_review_keyword_keyword (keyword_id),
                                CONSTRAINT fk_review_keyword_review FOREIGN KEY (review_id) REFERENCES review (id),
                                CONSTRAINT fk_review_keyword_keyword FOREIGN KEY (keyword_id) REFERENCES keyword (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================
-- review_like
-- =============================================================
CREATE TABLE review_like (
                             id          BIGINT   NOT NULL AUTO_INCREMENT,
                             created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             user_id     BIGINT   NOT NULL,
                             review_id   BIGINT   NOT NULL,
                             PRIMARY KEY (id),
                             UNIQUE KEY uk_review_like_user_review (user_id, review_id),
                             KEY idx_review_like_review (review_id),
                             CONSTRAINT fk_review_like_user FOREIGN KEY (user_id) REFERENCES user (id),
                             CONSTRAINT fk_review_like_review FOREIGN KEY (review_id) REFERENCES review (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================
-- bakery_image
-- =============================================================
CREATE TABLE bakery_image (
                              id         BIGINT   NOT NULL AUTO_INCREMENT,
                              origin     TINYTEXT NULL,
                              image_url  TEXT     NULL,
                              bakery_id  BIGINT   NULL,
                              PRIMARY KEY (id),
                              KEY idx_bakery_image_bakery (bakery_id),
                              CONSTRAINT fk_bakery_image_bakery FOREIGN KEY (bakery_id) REFERENCES bakery (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================
-- sig_image (SignatureImage)
-- =============================================================
CREATE TABLE sig_image (
                           id         BIGINT   NOT NULL AUTO_INCREMENT,
                           origin     TINYTEXT NULL,
                           image_url  TEXT     NULL,
                           bakery_id  BIGINT   NULL,
                           PRIMARY KEY (id),
                           KEY idx_sig_image_bakery (bakery_id),
                           CONSTRAINT fk_sig_image_bakery FOREIGN KEY (bakery_id) REFERENCES bakery (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================
-- bookmark
-- =============================================================
CREATE TABLE bookmark (
                          id          BIGINT   NOT NULL AUTO_INCREMENT,
                          created_at  DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
                          user_id     BIGINT   NOT NULL,
                          bakery_id   BIGINT   NOT NULL,
                          PRIMARY KEY (id),
                          UNIQUE KEY uk_bookmark_user_bakery (user_id, bakery_id),
                          KEY idx_bookmark_bakery (bakery_id),
                          CONSTRAINT fk_bookmark_user FOREIGN KEY (user_id) REFERENCES user (id),
                          CONSTRAINT fk_bookmark_bakery FOREIGN KEY (bakery_id) REFERENCES bakery (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================
-- user_item
-- =============================================================
CREATE TABLE user_item (
                           id           BIGINT   NOT NULL AUTO_INCREMENT,
                           acquired_at  DATETIME NOT NULL,
                           user_id      BIGINT   NOT NULL,
                           item_id      BIGINT   NOT NULL,
                           PRIMARY KEY (id),
                           UNIQUE KEY uk_user_item_user_item (user_id, item_id),
                           KEY idx_user_item_item (item_id),
                           CONSTRAINT fk_user_item_user FOREIGN KEY (user_id) REFERENCES user (id),
                           CONSTRAINT fk_user_item_item FOREIGN KEY (item_id) REFERENCES item (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================
-- mission_progress
-- =============================================================
CREATE TABLE mission_progress (
                                  id            BIGINT       NOT NULL AUTO_INCREMENT,
                                  count         INT          NOT NULL,
                                  status        VARCHAR(255) NOT NULL,
                                  completed_at  DATETIME     NULL,
                                  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  updated_at    DATETIME     NULL,
                                  user_id       BIGINT       NOT NULL,
                                  mission_id    BIGINT       NOT NULL,
                                  PRIMARY KEY (id),
                                  KEY idx_mission_progress_user (user_id),
                                  KEY idx_mission_progress_mission (mission_id),
                                  CONSTRAINT fk_mission_progress_user FOREIGN KEY (user_id) REFERENCES user (id),
                                  CONSTRAINT fk_mission_progress_mission FOREIGN KEY (mission_id) REFERENCES mission (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================
-- point_history
-- =============================================================
CREATE TABLE point_history (
                               id          BIGINT       NOT NULL AUTO_INCREMENT,
                               type        VARCHAR(255) NULL,
                               content     VARCHAR(255) NULL,
                               amount      INT          NOT NULL,
                               created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               user_id     BIGINT       NOT NULL,
                               PRIMARY KEY (id),
                               KEY idx_point_history_user (user_id),
                               CONSTRAINT fk_point_history_user FOREIGN KEY (user_id) REFERENCES user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================
-- visits (Visit 엔티티, 테이블명 복수형 주의)
-- =============================================================
CREATE TABLE visits (
                        id         BIGINT NOT NULL AUTO_INCREMENT,
                        count      INT    NULL,
                        bakery_id  BIGINT NULL,
                        user_id    BIGINT NOT NULL,
                        PRIMARY KEY (id),
                        KEY idx_visits_bakery (bakery_id),
                        KEY idx_visits_user (user_id),
                        CONSTRAINT fk_visits_bakery FOREIGN KEY (bakery_id) REFERENCES bakery (id),
                        CONSTRAINT fk_visits_user FOREIGN KEY (user_id) REFERENCES user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================
-- visit_detail
-- =============================================================
CREATE TABLE visit_detail (
                              id            BIGINT      NOT NULL AUTO_INCREMENT,
                              total_amount  INT         NULL,
                              visited_at    DATE        NULL,
                              image_hash    VARCHAR(64) NULL,
                              created_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              visit_id      BIGINT      NOT NULL,
                              PRIMARY KEY (id),
                              KEY idx_visit_detail_visit (visit_id),
                              CONSTRAINT fk_visit_detail_visit FOREIGN KEY (visit_id) REFERENCES visits (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
