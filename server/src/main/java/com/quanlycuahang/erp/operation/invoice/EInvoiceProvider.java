package com.quanlycuahang.erp.operation.invoice;

import com.quanlycuahang.erp.operation.dto.InvoiceDetailResponse;

/**
 * Hop dong tich hop hoa don dien tu (Nghi dinh 123/2020/ND-CP) voi nha cung cap ngoai (Viettel
 * S-Invoice, MISA meInvoice, VNPT...) trong tuong lai. Chua trien khai that o Phase 9 — cac nha
 * cung cap tren deu can hop dong thuong mai/ma so thue doanh nghiep that de dang ky API sandbox,
 * khong the tich hop va kiem chung that trong pham vi phien lam viec nay. Ben duoi la {@link
 * NoOpEInvoiceProvider} lam bean mac dinh (khong gui di dau, chi log) de he thong bien dich/chay
 * duoc ngay ma khong phu thuoc nha cung cap that.
 *
 * <p>De tich hop that: viet 1 lop @Component moi implement interface nay (vi du
 * ViettelEInvoiceProvider), goi API REST/SOAP cua nha cung cap trong submit(), tra ve
 * EInvoiceSubmissionResult voi providerReferenceId that; dang ky bean do thay the
 * NoOpEInvoiceProvider qua @Primary hoac @ConditionalOnProperty.
 */
public interface EInvoiceProvider {

  EInvoiceSubmissionResult submit(InvoiceDetailResponse invoice);

  record EInvoiceSubmissionResult(boolean success, String providerReferenceId, String message) {}
}
