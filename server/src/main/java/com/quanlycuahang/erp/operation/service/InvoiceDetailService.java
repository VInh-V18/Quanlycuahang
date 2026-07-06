package com.quanlycuahang.erp.operation.service;

import com.quanlycuahang.erp.common.exception.ResourceNotFoundException;
import com.quanlycuahang.erp.operation.dto.InvoiceDetailResponse;
import com.quanlycuahang.erp.operation.entity.Invoice;
import com.quanlycuahang.erp.operation.invoice.InvoiceDetailAssembler;
import com.quanlycuahang.erp.operation.repository.InvoiceRepository;
import com.quanlycuahang.erp.sales.entity.Order;
import com.quanlycuahang.erp.sales.repository.OrderItemRepository;
import com.quanlycuahang.erp.sales.repository.OrderPaymentRepository;
import com.quanlycuahang.erp.system.service.SettingsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Doc du lieu hoa don day du de tra JSON (Phase 9) — Backend khong render HTML/PDF (B3). */
@Service
public class InvoiceDetailService {

  private final InvoiceRepository invoiceRepository;
  private final OrderItemRepository orderItemRepository;
  private final OrderPaymentRepository orderPaymentRepository;
  private final SettingsService settingsService;

  public InvoiceDetailService(
      InvoiceRepository invoiceRepository,
      OrderItemRepository orderItemRepository,
      OrderPaymentRepository orderPaymentRepository,
      SettingsService settingsService) {
    this.invoiceRepository = invoiceRepository;
    this.orderItemRepository = orderItemRepository;
    this.orderPaymentRepository = orderPaymentRepository;
    this.settingsService = settingsService;
  }

  @Transactional(readOnly = true)
  public InvoiceDetailResponse getById(Long id) {
    Invoice invoice =
        invoiceRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay hoa don"));
    return buildResponse(invoice);
  }

  @Transactional(readOnly = true)
  public InvoiceDetailResponse getByLookupCode(String lookupCode) {
    Invoice invoice =
        invoiceRepository
            .findByLookupCode(lookupCode)
            .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay hoa don"));
    return buildResponse(invoice);
  }

  private InvoiceDetailResponse buildResponse(Invoice invoice) {
    Order order = invoice.getOrder();
    return InvoiceDetailAssembler.assemble(
        invoice,
        order,
        orderItemRepository.findByOrderId(order.getId()),
        orderPaymentRepository.findByOrderId(order.getId()),
        settingsService.getValue(null, SettingsService.KEY_STORE_NAME, ""),
        settingsService.getValue(null, SettingsService.KEY_STORE_TAX_CODE, ""),
        order.getCashier().getFullName(),
        order.getCustomer() == null ? null : order.getCustomer().getName(),
        order.getCustomer() == null ? null : order.getCustomer().getPhone(),
        order.getCustomer() == null ? null : order.getCustomer().getEmail());
  }
}
