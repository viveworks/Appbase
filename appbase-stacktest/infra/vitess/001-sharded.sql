-- ==================================== --
-- Appbase StackTest Schema
-- ==================================== --

DROP TABLE IF EXISTS stacktest_order;
DROP TABLE IF EXISTS stacktest_customer;

CREATE TABLE IF NOT EXISTS stacktest_customer(
  customer_id BIGINT NOT NULL,
  email VARCHAR(128),
  photoId TEXT,
  PRIMARY KEY(customer_id)
) ENGINE=InnoDB;



CREATE TABLE IF NOT EXISTS stacktest_order(
  order_id BIGINT NOT NULL,
  customer_id BIGINT,
  sku VARCHAR(128),
  price DOUBLE,
  PRIMARY KEY(order_id),
  CONSTRAINT stacktest_order_customer_id_fk
    FOREIGN KEY (customer_id)
    REFERENCES stacktest_customer (customer_id) ON DELETE CASCADE
) ENGINE=InnoDB;
