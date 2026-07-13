-- ShiftRepository.findFirstByOpenedByIdAndStatusOrderByOpenedAtDesc chay moi lan mo ca (duong
-- nong, nhay do tre) nhung khong co index tren opened_by - se seq-scan khi bang shifts lon dan
-- (phat hien khi rieng soat).
CREATE INDEX idx_shifts_opened_by ON shifts (opened_by);
