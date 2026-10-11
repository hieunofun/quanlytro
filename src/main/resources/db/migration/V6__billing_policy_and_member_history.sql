ALTER TABLE contract_members ADD COLUMN effective_from DATE;
ALTER TABLE contract_members ADD COLUMN effective_to DATE;
UPDATE contract_members m SET effective_from=c.start_date FROM contracts c WHERE m.contract_id=c.id;
ALTER TABLE contract_members ALTER COLUMN effective_from SET NOT NULL;
ALTER TABLE contract_members ADD CONSTRAINT chk_member_dates CHECK(effective_to IS NULL OR effective_to>effective_from);
ALTER TABLE properties ADD COLUMN meter_cutoff_day INT NOT NULL DEFAULT 28 CHECK(meter_cutoff_day BETWEEN 1 AND 31);
ALTER TABLE properties ADD COLUMN invoice_issue_day INT NOT NULL DEFAULT 1 CHECK(invoice_issue_day BETWEEN 1 AND 28);
ALTER TABLE properties ADD COLUMN late_fee_enabled BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE properties ADD COLUMN late_fee_percentage NUMERIC(5,2) NOT NULL DEFAULT 0 CHECK(late_fee_percentage BETWEEN 0 AND 100);
