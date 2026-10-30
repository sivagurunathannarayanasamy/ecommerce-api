-- Adds product reviews to an existing ecommerce_db (schema.sql drops the database, so run this instead).
USE ecommerce_db;

CREATE TABLE product_reviews (
    id          BIGINT   NOT NULL AUTO_INCREMENT,
    product_id  BIGINT   NOT NULL,
    customer_id BIGINT   NOT NULL,
    rating      TINYINT  NOT NULL,
    comment     TEXT,
    created_at  DATETIME,
    PRIMARY KEY (id),
    CONSTRAINT fk_product_reviews_product  FOREIGN KEY (product_id)  REFERENCES products (id),
    CONSTRAINT fk_product_reviews_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT chk_product_reviews_rating  CHECK (rating BETWEEN 1 AND 5)
);

CREATE INDEX idx_product_reviews_product ON product_reviews (product_id, created_at);
