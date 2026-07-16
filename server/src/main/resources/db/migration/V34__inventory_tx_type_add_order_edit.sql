-- Sua don da hoan tat (OrderEditService, tinh nang moi) ghi InventoryTransaction voi
-- type='order_edit' cho phan chenh lech kho khi tang/giam so luong dong hang - gia tri nay chua
-- co trong whitelist chk_inventory_tx_type (V1), phat hien khi test that: INSERT that bai voi loi
-- "violates check constraint chk_inventory_tx_type" (23514).
ALTER TABLE inventory_transactions DROP CONSTRAINT chk_inventory_tx_type;

ALTER TABLE inventory_transactions ADD CONSTRAINT chk_inventory_tx_type CHECK (
    type IN ('purchase', 'sale', 'supplier_return', 'customer_return', 'stock_take', 'transfer', 'cancel', 'order_edit')
);
