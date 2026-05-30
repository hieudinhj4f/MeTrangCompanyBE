-- Hibernate cannot auto-cast old receipt columns (e.g. product_id integer -> bigint FK).
-- These tables are recreated by ddl-auto=update on next startup.
DROP TABLE IF EXISTS warehouse_receipt_details CASCADE;
DROP TABLE IF EXISTS warehouse_receipts CASCADE;
