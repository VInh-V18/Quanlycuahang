package com.quanlycuahang.erp.operation.invoice;

import com.quanlycuahang.erp.operation.dto.InvoiceDetailResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Bean mac dinh khi chua cau hinh nha cung cap hoa don dien tu that (xem {@link EInvoiceProvider}
 * de biet cach thay the). Khong goi API ngoai nao — chi log lai de biet tinh nang nay dang o trang
 * thai "chua tich hop", tranh NullPointerException/loi khoi dong o noi goi.
 */
@Component
public class NoOpEInvoiceProvider implements EInvoiceProvider {

  private static final Logger log = LoggerFactory.getLogger(NoOpEInvoiceProvider.class);

  @Override
  public EInvoiceSubmissionResult submit(InvoiceDetailResponse invoice) {
    log.info(
        "EInvoiceProvider chua duoc cau hinh — bo qua gui hoa don dien tu cho hoa don {}",
        invoice.getInvoiceNumber());
    return new EInvoiceSubmissionResult(false, null, "Chua cau hinh nha cung cap hoa don dien tu");
  }
}
