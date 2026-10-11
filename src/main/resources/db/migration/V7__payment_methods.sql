ALTER TABLE payment_transactions DROP CONSTRAINT payment_transactions_payment_method_check;
ALTER TABLE payment_transactions ADD CONSTRAINT chk_payment_method CHECK(payment_method IN ('CASH','BANK_TRANSFER_MANUAL','OTHER','DEPOSIT_OFFSET'));
