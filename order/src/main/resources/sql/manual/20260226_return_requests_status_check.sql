-- Run once on PostgreSQL for order service DB
ALTER TABLE return_requests DROP CONSTRAINT IF EXISTS return_requests_status_check;

ALTER TABLE return_requests
    ADD CONSTRAINT return_requests_status_check
    CHECK (status IN (
        'RETURN_REQUESTED',
        'RETURN_SELLER_APPROVED',
        'RETURN_SHIPPED',
        'RETURN_RECEIVED',
        'RETURN_APPROVED',
        'RETURN_COMPLETED',
        'RETURN_REJECTED_BEFORE_SHIPMENT',
        'RETURN_REJECTED_AFTER_SHIPMENT',
        'RETURN_REJECTED'
    ));
