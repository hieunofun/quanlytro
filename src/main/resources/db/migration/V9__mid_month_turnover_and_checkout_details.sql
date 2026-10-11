-- A room can have successive tenants within one calendar month.
-- Each contract still has at most one reading of each meter per month.
ALTER TABLE meter_readings DROP CONSTRAINT uq_meter_reading_record;
ALTER TABLE meter_readings ADD CONSTRAINT uq_meter_reading_record
    UNIQUE (contract_id, billing_period, meter_type);

-- Utilities are included in invoice debt. This field is the informational
-- subtotal for the final occupied month and must not be deducted a second time.
UPDATE checkout_records cr
SET final_utility_amount = COALESCE((
    SELECT SUM(ii.subtotal)
    FROM invoices i JOIN invoice_items ii ON ii.invoice_id = i.id
    WHERE i.contract_id = cr.contract_id AND i.status = 'ISSUED'
      AND i.billing_period = TO_CHAR(cr.checkout_date - 1, 'YYYY-MM')
      AND ii.old_index_snapshot IS NOT NULL AND ii.new_index_snapshot IS NOT NULL
), 0);
