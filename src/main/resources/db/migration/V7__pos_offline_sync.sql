-- Offline-First PWA POS Support: Idempotent UUID deduplication & offline receipt audit trail

ALTER TABLE pos_orders ADD COLUMN client_order_uuid VARCHAR(64);
ALTER TABLE pos_orders ADD COLUMN offline_receipt_number VARCHAR(36);
ALTER TABLE pos_orders ADD COLUMN synced_from_offline BOOLEAN NOT NULL DEFAULT FALSE;

CREATE UNIQUE INDEX uq_pos_orders_client_uuid ON pos_orders (client_order_uuid);
CREATE INDEX idx_pos_orders_offline_ref ON pos_orders (offline_receipt_number);
