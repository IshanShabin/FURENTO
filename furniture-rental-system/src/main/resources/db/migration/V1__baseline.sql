-- Flyway baseline migration for FURENTO
-- Captures the existing schema as of 27 November 2026.
-- Applied automatically to empty databases (Docker, RDS).
-- Existing local databases are baselined at version 1 without running this file.
--
-- Tables are ordered so that referenced tables come before referencing ones.
-- AUTO_INCREMENT starting values are omitted (Flyway does not require them).
-- The employee, items, student and student_details tables are unrelated practice
-- tables from the same local MySQL instance and are intentionally excluded.

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ---------------------------------------------------------------------
-- users  (no FK dependencies)
-- ---------------------------------------------------------------------
CREATE TABLE `users` (
  `id`         bigint       NOT NULL AUTO_INCREMENT,
  `email`      varchar(255) NOT NULL,
  `first_name` varchar(255) NOT NULL,
  `last_name`  varchar(255) NOT NULL,
  `password`   varchar(255) NOT NULL,
  `role`       varchar(255) NOT NULL,
  `username`   varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_r43af9ap4edm43mmtq01oddj6` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------
-- address  (FK → users)
-- ---------------------------------------------------------------------
CREATE TABLE `address` (
  `id`       bigint       NOT NULL AUTO_INCREMENT,
  `city`     varchar(255) DEFAULT NULL,
  `state`    varchar(255) DEFAULT NULL,
  `street`   varchar(255) DEFAULT NULL,
  `zip_code` varchar(255) DEFAULT NULL,
  `user_id`  bigint       DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK6i66ijb8twgcqtetl8eeeed6v` (`user_id`),
  CONSTRAINT `FK6i66ijb8twgcqtetl8eeeed6v`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------
-- category  (no FK dependencies)
-- ---------------------------------------------------------------------
CREATE TABLE `category` (
  `id`          bigint       NOT NULL AUTO_INCREMENT,
  `description` varchar(255) DEFAULT NULL,
  `name`        varchar(255) NOT NULL,
  `image_url`   varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_46ccwnsi9409t36lurvtyljak` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------
-- furniture  (FK → category)
-- ---------------------------------------------------------------------
CREATE TABLE `furniture` (
  `id`                     bigint         NOT NULL AUTO_INCREMENT,
  `available`              bit(1)         NOT NULL,
  `description`            varchar(1000)  DEFAULT NULL,
  `name`                   varchar(255)   NOT NULL,
  `rental_price_per_month` decimal(38,2)  NOT NULL,
  `stock_quantity`         int            NOT NULL,
  `category_id`            bigint         DEFAULT NULL,
  `purchase_price`         decimal(38,2)  DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKj77x761h8pyvwcby4rkd1dnyd` (`category_id`),
  CONSTRAINT `FKj77x761h8pyvwcby4rkd1dnyd`
    FOREIGN KEY (`category_id`) REFERENCES `category` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------
-- furniture_image  (FK → furniture)
-- ---------------------------------------------------------------------
CREATE TABLE `furniture_image` (
  `id`           bigint       NOT NULL AUTO_INCREMENT,
  `image_url`    varchar(255) NOT NULL,
  `furniture_id` bigint       DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKqjr2y6sby7ik6lu1hhfrjf6b` (`furniture_id`),
  CONSTRAINT `FKqjr2y6sby7ik6lu1hhfrjf6b`
    FOREIGN KEY (`furniture_id`) REFERENCES `furniture` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------
-- payment  (no FK dependencies)
-- ---------------------------------------------------------------------
CREATE TABLE `payment` (
  `id`                 bigint        NOT NULL AUTO_INCREMENT,
  `amount`             decimal(38,2) DEFAULT NULL,
  `payment_date`       datetime(6)   DEFAULT NULL,
  `payment_method`     varchar(255)  DEFAULT NULL,
  `status`             varchar(255)  DEFAULT NULL,
  `gateway_order_id`   varchar(255)  DEFAULT NULL,
  `gateway_payment_id` varchar(255)  DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------
-- delivery  (FK → address)
-- Note: address_id is a Hibernate-generated column; it references address
-- even though no screen currently uses the Address entity directly.
-- ---------------------------------------------------------------------
CREATE TABLE `delivery` (
  `id`            bigint       NOT NULL AUTO_INCREMENT,
  `delivery_date` datetime(6)  DEFAULT NULL,
  `pickup_date`   datetime(6)  DEFAULT NULL,
  `status`        varchar(255) DEFAULT NULL,
  `address_id`    bigint       DEFAULT NULL,
  `address_line1` varchar(255) DEFAULT NULL,
  `address_line2` varchar(255) DEFAULT NULL,
  `city`          varchar(255) DEFAULT NULL,
  `state`         varchar(255) DEFAULT NULL,
  `zip_code`      varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKlayueon76a86wr2gbsyannaf7` (`address_id`),
  CONSTRAINT `FKlayueon76a86wr2gbsyannaf7`
    FOREIGN KEY (`address_id`) REFERENCES `address` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------
-- rental  (FK → users, payment, delivery)
-- ---------------------------------------------------------------------
CREATE TABLE `rental` (
  `id`           bigint        NOT NULL AUTO_INCREMENT,
  `order_date`   datetime(6)   DEFAULT NULL,
  `status`       varchar(255)  DEFAULT NULL,
  `total_amount` decimal(38,2) DEFAULT NULL,
  `delivery_id`  bigint        DEFAULT NULL,
  `payment_id`   bigint        DEFAULT NULL,
  `user_id`      bigint        NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_7bkc21mdkuf3sjck0lpw3s9rp` (`delivery_id`),
  UNIQUE KEY `UK_t02axvogqj1j05dllbubk1l0y` (`payment_id`),
  KEY `FKj1ty59tjbwlmx7p4uotyto7lp` (`user_id`),
  CONSTRAINT `FKcs5aqo1hvl6f7ft91myow5g6c`
    FOREIGN KEY (`payment_id`) REFERENCES `payment` (`id`),
  CONSTRAINT `FKj1ty59tjbwlmx7p4uotyto7lp`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKlagcxboa3gwg1kjujx4n6c6hw`
    FOREIGN KEY (`delivery_id`) REFERENCES `delivery` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------
-- rental_item  (FK → rental, furniture)
-- ---------------------------------------------------------------------
CREATE TABLE `rental_item` (
  `id`                   bigint        NOT NULL AUTO_INCREMENT,
  `end_date`             date          DEFAULT NULL,
  `quantity`             int           NOT NULL,
  `rent_amount`          decimal(38,2) DEFAULT NULL,
  `rental_period_months` int           NOT NULL,
  `start_date`           date          DEFAULT NULL,
  `furniture_id`         bigint        DEFAULT NULL,
  `rental_id`            bigint        DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKtgc5fjt55ay3jcrhx66c4v1u9` (`furniture_id`),
  KEY `FKa20tqix3c9aesqmluuq9lkn9b` (`rental_id`),
  CONSTRAINT `FKa20tqix3c9aesqmluuq9lkn9b`
    FOREIGN KEY (`rental_id`) REFERENCES `rental` (`id`),
  CONSTRAINT `FKtgc5fjt55ay3jcrhx66c4v1u9`
    FOREIGN KEY (`furniture_id`) REFERENCES `furniture` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------
-- reviews  (FK → furniture, users)
-- ---------------------------------------------------------------------
CREATE TABLE `reviews` (
  `id`           bigint        NOT NULL AUTO_INCREMENT,
  `comment`      varchar(1000) DEFAULT NULL,
  `created_at`   datetime(6)   NOT NULL,
  `rating`       int           NOT NULL,
  `furniture_id` bigint        NOT NULL,
  `user_id`      bigint        NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK5g0is0hcf34u9g11euu5jsjwi` (`furniture_id`),
  KEY `FKcgy7qjc1r99dp117y9en6lxye` (`user_id`),
  CONSTRAINT `FK5g0is0hcf34u9g11euu5jsjwi`
    FOREIGN KEY (`furniture_id`) REFERENCES `furniture` (`id`),
  CONSTRAINT `FKcgy7qjc1r99dp117y9en6lxye`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------
-- penalty  (FK → rental)
-- Includes the description column added after the original baseline.
-- ---------------------------------------------------------------------
CREATE TABLE `penalty` (
  `id`          bigint        NOT NULL AUTO_INCREMENT,
  `amount`      decimal(38,2) DEFAULT NULL,
  `issued_date` datetime(6)   DEFAULT NULL,
  `paid`        bit(1)        NOT NULL,
  `reason`      varchar(255)  DEFAULT NULL,
  `rental_id`   bigint        DEFAULT NULL,
  `description` varchar(255)  DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK9fyh1day7iohr3t9kwffidl08` (`rental_id`),
  CONSTRAINT `FK9fyh1day7iohr3t9kwffidl08`
    FOREIGN KEY (`rental_id`) REFERENCES `rental` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------
-- revoked_token  (no FK dependencies)
-- Stores JTI values of logged-out JWTs to prevent replay.
-- ---------------------------------------------------------------------
CREATE TABLE `revoked_token` (
  `token_id`   varchar(64)  NOT NULL,
  `expires_at` datetime(6)  NOT NULL,
  PRIMARY KEY (`token_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

SET FOREIGN_KEY_CHECKS = 1;
