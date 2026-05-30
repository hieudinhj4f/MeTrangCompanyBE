-- Run manually if spring_backend still fails after entity changes:
--   docker exec -i postgres_db psql -U user_metrang -d metrang_db < backend/scripts/fix-postgres-schema.sql

DROP TABLE IF EXISTS warehouse_receipt_details CASCADE;
DROP TABLE IF EXISTS warehouse_receipts CASCADE;
