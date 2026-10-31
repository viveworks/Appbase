-- ==================================== --
-- appbase-service schema
-- ==================================== --

DROP TABLE IF EXISTS `appbase_UserAccount`;
DROP TABLE IF EXISTS `appbase_UserAccount_appbaseUserId_keyspaceId_lookup`;
DROP TABLE IF EXISTS `appbase_UserAccount_email_keyspaceId_lookup`;

CREATE TABLE IF NOT EXISTS `appbase_UserAccount_appbaseUserId_keyspaceId_lookup`(
    `appbaseUserId`         VARCHAR(255),
    `keyspaceId`            VARBINARY(16),
    CONSTRAINT `UserAccount_appbaseUserId_keyspace_lookup_pk` PRIMARY KEY(`appbaseUserId`)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `appbase_UserAccount_email_keyspaceId_lookup`(
    `email`                 VARCHAR(255),
    `keyspaceId`            VARBINARY(16),
    CONSTRAINT `UserAccount_email_keyspace_lookup_pk` PRIMARY KEY(`email`)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `appbase_UserAccount`(
    `appbaseUserId`             VARCHAR(255),
    `idpUserId`                 VARCHAR(255),
    `email`                     VARCHAR(255),
    `name`                      VARCHAR(255),
    `passwordHashes`            VARCHAR(1024),
    `passwordResetEmailSent`    TINYINT(1),
    `passwordResetCodeHash`     VARCHAR(1024),
    `userTopic`                 VARCHAR(255),
    `userHomeDataCenter`        VARCHAR(255),
    `emailVerified`             TINYINT(1),
    `languageCode`              CHAR(10),
    `addressLine1`              TEXT,
    `addressLine2`              TEXT,
    `city`                      VARCHAR(255),
    `stateProvince`             VARCHAR(255),
    `postalCode`                VARCHAR(32),
    `country`                   VARCHAR(255),
    `latitude`                  VARCHAR(255),
    `longitude`                 VARCHAR(255),
    `deleted`                   TINYINT(1),
    `creationTimestamp`         DATETIME,
    `lastAccessTimestamp`       DATETIME,
    CONSTRAINT `UserAccount_idpUserId_pk` PRIMARY KEY(`idpUserId`),
    CONSTRAINT `UserAccount_appbaseUserId_idx` UNIQUE(`appbaseUserId`),
    CONSTRAINT `UserAccount_email_idx` UNIQUE(`email`)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `appbase_SupportMessage`(
    `messageId`                 VARCHAR(255),
    `email`                     VARCHAR(255),
    `message`                   TEXT,
    `appbaseUserId`             VARCHAR(255),
    `status`                    TEXT,
    `deleted`                   TINYINT(1),
    `creationTimestamp`         DATETIME,
    `lastAccessTimestamp`       DATETIME,
    CONSTRAINT `SupportMessage_messageId_pk` PRIMARY KEY(`messageId`)
) ENGINE=InnoDB;
