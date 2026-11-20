-- Kitchen Display System (KDS) workflow columns on pos_orders
ALTER TABLE pos_orders ADD COLUMN kitchen_status VARCHAR(24) NOT NULL DEFAULT 'PENDING';
ALTER TABLE pos_orders ADD COLUMN kitchen_started_at TIMESTAMP NULL;
ALTER TABLE pos_orders ADD COLUMN kitchen_ready_at TIMESTAMP NULL;
ALTER TABLE pos_orders ADD COLUMN kitchen_completed_at TIMESTAMP NULL;
ALTER TABLE pos_orders ADD COLUMN kitchen_updated_by VARCHAR(80) NULL;

CREATE INDEX idx_pos_orders_kitchen_queue
    ON pos_orders (status, kitchen_status, created_at);

-- Backfill voided orders and historical orders older than today as SERVED
-- so old receipts do not flood the live Kitchen Display queue.
UPDATE pos_orders
SET kitchen_status = 'SERVED',
    kitchen_completed_at = COALESCE(kitchen_completed_at, created_at)
WHERE status = 'VOIDED'
   OR CAST(created_at AS DATE) < CURRENT_DATE;
