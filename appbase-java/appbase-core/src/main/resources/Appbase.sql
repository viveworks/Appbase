CREATE TABLE `ExceptionMessage` (
    `id` INTEGER NOT NULL,
    `discriminator` VARCHAR(255) NOT NULL,
    `message` VARCHAR(1024) NOT NULL,
    `sourceTypeName` VARCHAR(1024) NOT NULL,
    `stackTrace` VARCHAR(2048),
    `stackTraceHash` INTEGER NOT NULL,
    `threadName` VARCHAR(1024) NOT NULL,
    `throwableMessage` VARCHAR(1024),
    `exceptionTypeName` VARCHAR(255),
    `exceptionTimestamp` BIGINT,
    PRIMARY KEY (id));
