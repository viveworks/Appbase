DROP TABLE IF EXISTS stacktest_product;
CREATE TABLE IF NOT EXISTS stacktest_product(
  sku VARCHAR(128),
  description VARCHAR(128),
  price DOUBLE,
  PRIMARY KEY(sku)
) ENGINE=InnoDB;

DROP TABLE IF EXISTS stacktest_customer_seq;
CREATE TABLE IF NOT EXISTS stacktest_customer_seq(
  id INT,
  next_id BIGINT,
  cache BIGINT,
  PRIMARY KEY(id))
  COMMENT 'vitess_sequence';
INSERT INTO stacktest_customer_seq(id, next_id, cache) VALUES(0, 1000, 100);



DROP TABLE IF EXISTS stacktest_order_seq;
CREATE TABLE IF NOT EXISTS stacktest_order_seq(
  id INT,
  next_id BIGINT,
  cache BIGINT,
  PRIMARY KEY(id))
  COMMENT 'vitess_sequence';
INSERT INTO stacktest_order_seq(id, next_id, cache) VALUES(0, 1000, 100);