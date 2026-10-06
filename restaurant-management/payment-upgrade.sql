USE restaurant_db;

/* Payment + table booking billing upgrade */
ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS booking_charge DOUBLE NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS reservation_number VARCHAR(30) NULL,
    ADD COLUMN IF NOT EXISTS payment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN IF NOT EXISTS payment_method VARCHAR(20) NOT NULL DEFAULT 'NOT_SELECTED',
    ADD COLUMN IF NOT EXISTS payment_time DATETIME NULL;

UPDATE orders
SET booking_charge = 0
WHERE booking_charge IS NULL;

UPDATE orders
SET payment_status = 'PENDING'
WHERE payment_status IS NULL OR payment_status = '';

UPDATE orders
SET payment_method = 'NOT_SELECTED'
WHERE payment_method IS NULL OR payment_method = '';

SELECT
    order_id,
    order_number,
    reservation_number,
    booking_charge,
    payment_status,
    payment_method,
    payment_time,
    total_amount
FROM orders
ORDER BY order_id;
