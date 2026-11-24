ALTER TABLE handover_items
    ADD COLUMN original_item_id bigint REFERENCES handover_items(id),
    ADD CONSTRAINT handover_item_distinct_original CHECK (original_item_id IS DISTINCT FROM id);

CREATE UNIQUE INDEX uq_handover_item_original
    ON handover_items (handover_id, original_item_id)
    WHERE original_item_id IS NOT NULL;

ALTER TABLE contract_terms
    ADD COLUMN lessor_name varchar(150) NOT NULL DEFAULT '',
    ADD COLUMN lessor_identity varchar(40) NOT NULL DEFAULT '',
    ADD COLUMN lessor_phone varchar(30) NOT NULL DEFAULT '',
    ADD COLUMN lessor_address varchar(350) NOT NULL DEFAULT '',
    ADD COLUMN tenant_address varchar(350) NOT NULL DEFAULT '';
