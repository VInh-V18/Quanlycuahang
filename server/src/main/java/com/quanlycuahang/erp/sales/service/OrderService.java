package com.quanlycuahang.erp.sales.service;

import static com.quanlycuahang.erp.common.util.Instants.toInstant;

import com.quanlycuahang.erp.auth.security.BranchAccessGuard;
import com.quanlycuahang.erp.auth.security.CurrentUserProvider;
import com.quanlycuahang.erp.auth.security.TenantContext;
import com.quanlycuahang.erp.common.dto.ApiResponse;
import com.quanlycuahang.erp.common.exception.BusinessRuleException;
import com.quanlycuahang.erp.common.exception.ResourceNotFoundException;
import com.quanlycuahang.erp.common.metrics.BusinessMetrics;
import com.quanlycuahang.erp.common.sequence.NumberSequenceService;
import com.quanlycuahang.erp.common.web.IdempotencyService;
import com.quanlycuahang.erp.inventory.entity.Inventory;
import com.quanlycuahang.erp.inventory.entity.InventoryTransaction;
import com.quanlycuahang.erp.inventory.repository.InventoryRepository;
import com.quanlycuahang.erp.inventory.repository.InventoryTransactionRepository;
import com.quanlycuahang.erp.operation.entity.Invoice;
import com.quanlycuahang.erp.operation.entity.Shift;
import com.quanlycuahang.erp.operation.repository.InvoiceRepository;
import com.quanlycuahang.erp.operation.repository.ShiftRepository;
import com.quanlycuahang.erp.partner.entity.Customer;
import com.quanlycuahang.erp.partner.entity.Debt;
import com.quanlycuahang.erp.partner.repository.CustomerRepository;
import com.quanlycuahang.erp.partner.repository.DebtRepository;
import com.quanlycuahang.erp.product.entity.Product;
import com.quanlycuahang.erp.product.repository.ProductRepository;
import com.quanlycuahang.erp.promotion.entity.Voucher;
import com.quanlycuahang.erp.promotion.service.VoucherService;
import com.quanlycuahang.erp.promotion.service.VoucherValidationResult;
import com.quanlycuahang.erp.sales.dto.OrderCreateRequest;
import com.quanlycuahang.erp.sales.dto.OrderItemResponse;
import com.quanlycuahang.erp.sales.dto.OrderListItemResponse;
import com.quanlycuahang.erp.sales.dto.OrderPaymentResponse;
import com.quanlycuahang.erp.sales.dto.OrderResponse;
import com.quanlycuahang.erp.sales.entity.Order;
import com.quanlycuahang.erp.sales.entity.OrderItem;
import com.quanlycuahang.erp.sales.pricing.OrderLineInput;
import com.quanlycuahang.erp.sales.pricing.OrderPricingRequest;
import com.quanlycuahang.erp.sales.pricing.OrderPricingResult;
import com.quanlycuahang.erp.sales.pricing.OrderPricingService;
import com.quanlycuahang.erp.sales.repository.OrderItemRepository;
import com.quanlycuahang.erp.sales.repository.OrderPaymentRepository;
import com.quanlycuahang.erp.sales.repository.OrderRepository;
import com.quanlycuahang.erp.sales.statemachine.OrderStatus;
import com.quanlycuahang.erp.system.entity.Branch;
import com.quanlycuahang.erp.system.repository.BranchRepository;
import com.quanlycuahang.erp.system.service.SettingsService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ban hang POS (UC-04, QUAN TRONG NHAT) — createOrder() chay trong 1 @Transactional duy nhat, dieu
 * phoi 4 collaborator tach ra o Prompt #2 (refactor god-class, xem docs/PROJECT_STATE.md muc
 * "Prompt #2" cho so do truoc/sau + bang trach nhiem/dong/dependency day du):
 *
 * <ul>
 *   <li>{@link OrderValidationService} — moi dieu kien chan/validate (quyen chi nhanh, ton kho gop
 *       theo productId, CK vuot subtotal, gia FE vs BE, no khong khach hang/vuot han muc).
 *   <li>{@link InventoryDeductionService} — ghi OrderItem + tru ton kho (@Version) +
 *       InventoryTransaction.
 *   <li>{@link OrderPaymentService} — ghi OrderPayment (kem VietQR) + ghi Debt khi ban no.
 *   <li>{@link OrderFinalizationService} — ghi nhan da dung voucher + xuat Invoice (kem VietQR).
 * </ul>
 *
 * <p>Ranh gioi @Transactional GIU NGUYEN o day (orchestrator) — khong collaborator nao tu mo
 * transaction rieng, dam bao tinh nguyen tu cua 1 lan checkout dung nhu truoc khi refactor.
 */
@Service
public class OrderService {

  private final OrderRepository orderRepository;
  private final OrderItemRepository orderItemRepository;
  private final OrderPaymentRepository orderPaymentRepository;
  private final ProductRepository productRepository;
  private final InventoryRepository inventoryRepository;
  private final InventoryTransactionRepository inventoryTransactionRepository;
  private final DebtRepository debtRepository;
  private final BranchRepository branchRepository;
  private final CustomerRepository customerRepository;
  private final ShiftRepository shiftRepository;
  private final InvoiceRepository invoiceRepository;
  private final VoucherService voucherService;
  private final SettingsService settingsService;
  private final CurrentUserProvider currentUserProvider;
  private final IdempotencyService idempotencyService;
  private final NumberSequenceService numberSequenceService;
  private final BranchAccessGuard branchAccessGuard;
  private final OrderValidationService orderValidationService;
  private final InventoryDeductionService inventoryDeductionService;
  private final OrderPaymentService orderPaymentService;
  private final OrderFinalizationService orderFinalizationService;
  private final BusinessMetrics businessMetrics;

  public OrderService(
      OrderRepository orderRepository,
      OrderItemRepository orderItemRepository,
      OrderPaymentRepository orderPaymentRepository,
      ProductRepository productRepository,
      InventoryRepository inventoryRepository,
      InventoryTransactionRepository inventoryTransactionRepository,
      DebtRepository debtRepository,
      BranchRepository branchRepository,
      CustomerRepository customerRepository,
      ShiftRepository shiftRepository,
      InvoiceRepository invoiceRepository,
      VoucherService voucherService,
      SettingsService settingsService,
      CurrentUserProvider currentUserProvider,
      IdempotencyService idempotencyService,
      NumberSequenceService numberSequenceService,
      BranchAccessGuard branchAccessGuard,
      OrderValidationService orderValidationService,
      InventoryDeductionService inventoryDeductionService,
      OrderPaymentService orderPaymentService,
      OrderFinalizationService orderFinalizationService,
      BusinessMetrics businessMetrics) {
    this.orderRepository = orderRepository;
    this.orderItemRepository = orderItemRepository;
    this.orderPaymentRepository = orderPaymentRepository;
    this.productRepository = productRepository;
    this.inventoryRepository = inventoryRepository;
    this.inventoryTransactionRepository = inventoryTransactionRepository;
    this.debtRepository = debtRepository;
    this.branchRepository = branchRepository;
    this.customerRepository = customerRepository;
    this.shiftRepository = shiftRepository;
    this.invoiceRepository = invoiceRepository;
    this.voucherService = voucherService;
    this.settingsService = settingsService;
    this.currentUserProvider = currentUserProvider;
    this.idempotencyService = idempotencyService;
    this.numberSequenceService = numberSequenceService;
    this.branchAccessGuard = branchAccessGuard;
    this.orderValidationService = orderValidationService;
    this.inventoryDeductionService = inventoryDeductionService;
    this.orderPaymentService = orderPaymentService;
    this.orderFinalizationService = orderFinalizationService;
    this.businessMetrics = businessMetrics;
  }

  @Transactional
  public OrderResponse createOrder(OrderCreateRequest request, String idempotencyKey) {
    io.micrometer.core.instrument.Timer.Sample checkoutTimer = businessMetrics.startCheckoutTimer();
    orderValidationService.assertBranchAccess(request.getBranchId());
    Branch branch =
        branchRepository
            .findById(request.getBranchId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chi nhánh"));

    Customer customer = null;
    if (request.getCustomerId() != null) {
      customer =
          customerRepository
              .findById(request.getCustomerId())
              .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách hàng"));
    }

    Shift shift = null;
    if (request.getShiftId() != null) {
      shift =
          shiftRepository
              .findById(request.getShiftId())
              .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ca làm việc"));
    }

    // Nap Product + Inventory hien tai theo lo (khong query rieng tung dong — tranh N+1 khi don
    // co nhieu dong) — Backend la nguon gia/VAT/gia von duy nhat, khong tin FE.
    boolean priceIncludesVat =
        settingsService.getBoolean(
            branch.getId(), SettingsService.KEY_PRICE_INCLUDES_VAT_DEFAULT, true);
    boolean allowNegativeStock =
        settingsService.getBoolean(branch.getId(), SettingsService.KEY_ALLOW_NEGATIVE_STOCK, false);
    BigDecimal roundingUnit =
        settingsService.getBigDecimal(
            branch.getId(), SettingsService.KEY_ROUNDING_UNIT, BigDecimal.valueOf(1000));

    List<Long> requestedProductIds =
        request.getLines().stream().map(l -> l.getProductId()).distinct().toList();
    Map<Long, Product> productsById =
        productRepository.findAllById(requestedProductIds).stream()
            .collect(java.util.stream.Collectors.toMap(Product::getId, p -> p));
    Map<Long, Inventory> inventoriesByProductId =
        inventoryRepository
            .findByBranchIdAndProductIdIn(branch.getId(), requestedProductIds)
            .stream()
            .collect(
                java.util.stream.Collectors.toMap(inv -> inv.getProduct().getId(), inv -> inv));

    List<OrderLineInput> pricingLines =
        orderValidationService.buildPricingLinesAndAssertStock(
            request.getLines(), productsById, inventoriesByProductId, allowNegativeStock);

    BigDecimal roughSubtotal =
        pricingLines.stream()
            .map(
                l -> l.getUnitPrice().multiply(l.getQuantity()).subtract(l.getLineDiscountAmount()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    Voucher voucher = null;
    BigDecimal voucherAmount = BigDecimal.ZERO;
    if (request.getVoucherCode() != null && !request.getVoucherCode().isBlank()) {
      VoucherValidationResult validation =
          voucherService.validate(request.getVoucherCode(), roughSubtotal);
      voucher = validation.voucher();
      voucherAmount = validation.discountAmount();
    }

    BigDecimal orderLevelReduction = request.getOrderDiscountAmount().add(voucherAmount);
    orderValidationService.assertOrderReductionWithinSubtotal(orderLevelReduction, roughSubtotal);

    OrderPricingRequest pricingRequest =
        new OrderPricingRequest(
            pricingLines,
            request.getOrderDiscountAmount(),
            voucherAmount,
            priceIncludesVat,
            roundingUnit,
            request.getCashReceived());
    OrderPricingResult pricing = OrderPricingService.calculate(pricingRequest);

    // Phi ship cong them SAU khi OrderPricingService tinh xong hang hoa (khong qua CK/VAT/lam
    // tron cua hang hoa, chi cong phang vao tong cuoi cung khach phai tra — Phase FH-hoa-don-v2).
    BigDecimal shippingFee =
        request.getShippingFee() == null ? BigDecimal.ZERO : request.getShippingFee();
    BigDecimal grandTotal = pricing.getTotalAmount().add(shippingFee);

    orderValidationService.assertPricingMatchesExpected(
        grandTotal, request.getExpectedTotalAmount());

    BigDecimal totalPaid =
        request.getPayments().stream()
            .map(com.quanlycuahang.erp.sales.dto.OrderPaymentRequest::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal unpaid = grandTotal.subtract(totalPaid);
    orderValidationService.assertUnpaidRequiresCustomer(unpaid, customer);
    if (unpaid.compareTo(BigDecimal.ZERO) > 0 && customer != null) {
      orderValidationService.assertWithinDebtLimit(customer, unpaid);
    }

    BigDecimal changeAmount =
        request.getCashReceived() == null ? null : request.getCashReceived().subtract(grandTotal);

    Order order = new Order();
    order.setOrderNumber(generateOrderNumber());
    order.setBranch(branch);
    order.setCustomer(customer);
    order.setShift(shift);
    order.setVoucher(voucher);
    order.setStatus(OrderStatus.COMPLETED.getValue());
    order.setSubtotalAmount(pricing.getSubtotalAmount());
    order.setDiscountAmount(pricing.getDiscountAmount());
    order.setVatAmount(pricing.getVatAmount());
    order.setRoundingAdjustment(pricing.getRoundingAdjustment());
    order.setTotalAmount(grandTotal);
    order.setCashReceived(request.getCashReceived());
    order.setChangeAmount(changeAmount);
    order.setShippingFee(shippingFee);
    order.setNote(request.getNote());
    currentUserProvider.getCurrentUser().ifPresent(order::setCashier);
    order = orderRepository.save(order);

    // Tu day tro xuong dieu phoi 4 collaborator (Prompt #2) theo dung thu tu buoc nhu truoc khi
    // tach: tru kho + dong don, thanh toan, cong no, voucher, hoa don.
    List<OrderItemResponse> itemResponses =
        inventoryDeductionService.deductStockAndCreateItems(
            order, pricing, productsById, inventoriesByProductId);
    OrderPaymentService.PaymentCapture payments =
        orderPaymentService.capturePayments(order, request.getPayments());
    orderPaymentService.recordDebtIfUnpaid(order, customer, unpaid);
    orderFinalizationService.recordVoucherUsageIfAny(voucher, order);
    Invoice invoice =
        orderFinalizationService.issueInvoice(
            order, generateInvoiceNumber(), grandTotal, payments.hasBankTransfer());

    OrderResponse response = new OrderResponse();
    response.setId(order.getId());
    response.setOrderNumber(order.getOrderNumber());
    response.setStatus(order.getStatus());
    response.setBranchId(branch.getId());
    response.setCustomerId(customer == null ? null : customer.getId());
    response.setSubtotalAmount(pricing.getSubtotalAmount());
    response.setDiscountAmount(pricing.getDiscountAmount());
    response.setVatAmount(pricing.getVatAmount());
    response.setRoundingAdjustment(pricing.getRoundingAdjustment());
    response.setTotalAmount(grandTotal);
    response.setChangeAmount(changeAmount);
    response.setShippingFee(shippingFee);
    response.setNote(order.getNote());
    response.setItems(itemResponses);
    response.setPayments(payments.responses());
    response.setInvoiceId(invoice.getId());
    response.setInvoiceNumber(invoice.getInvoiceNumber());
    response.setQrPayload(invoice.getQrPayload());
    response.setLookupCode(invoice.getLookupCode());
    response.setCreatedAt(order.getCreatedAt());

    if (idempotencyKey != null && !idempotencyKey.isBlank()) {
      idempotencyService.complete(idempotencyKey, String.valueOf(order.getId()));
    }

    businessMetrics.stopCheckoutTimer(checkoutTimer, TenantContext.get());
    businessMetrics.recordOrderCreated(TenantContext.get());
    return response;
  }

  @Transactional(readOnly = true)
  public OrderResponse getById(Long id) {
    Order order =
        orderRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));
    branchAccessGuard.assertAccess(order.getBranch().getId());
    return toResponse(order);
  }

  /**
   * Huy don da hoan tat (order:void) — phuc dung tu ban truoc Prompt #2 (bi xoa nham la dead code
   * vi khong con FE nao goi, xem docs/PROJECT_STATE.md/permission-matrix.md). Chi huy duoc don TAO
   * TRONG NGAY (Asia/Ho_Chi_Minh) — tranh sua nguoc lich su bao cao tai chinh cac ky truoc. Hoan
   * kho theo (quantity - returnedQuantity) tung dong (khong hoan lai phan da tung tra hang truoc
   * do). Chan huy neu cong no lien quan da duoc thu MOT PHAN (khong co cach doi chieu an toan mot
   * khoan da thu do voi don khong con hieu luc) — con no chua thu gi thi zero luon (khong xoa dong
   * Debt, giu vet lich su).
   */
  @Transactional
  public OrderResponse cancelOrder(Long id) {
    Order order =
        orderRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));
    branchAccessGuard.assertAccess(order.getBranch().getId());

    OrderStatus current = OrderStatus.fromValue(order.getStatus());
    if (!OrderStatus.canTransition(current, OrderStatus.CANCELLED)) {
      throw new BusinessRuleException(
          "ORDER_CANCEL_NOT_ALLOWED",
          "Không thể hủy đơn ở trạng thái hiện tại: " + order.getStatus());
    }

    ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");
    LocalDate orderDate = order.getCreatedAt().atZone(zone).toLocalDate();
    LocalDate today = OffsetDateTime.now(zone).toLocalDate();
    if (!orderDate.equals(today)) {
      throw new BusinessRuleException(
          "ORDER_CANCEL_WINDOW_EXPIRED", "Chỉ được hủy đơn trong ngày tạo đơn");
    }

    List<Debt> orderDebts =
        debtRepository.findByReferenceTypeAndReferenceIdOrderByIdAsc("order", order.getId());
    boolean hasPartialPayment =
        orderDebts.stream().anyMatch(d -> d.getAmount().compareTo(d.getOriginalAmount()) != 0);
    if (hasPartialPayment) {
      throw new BusinessRuleException(
          "ORDER_CANCEL_DEBT_ALREADY_PAID",
          "Không thể hủy đơn vì công nợ liên quan đã được thu một phần, vui lòng xử lý công nợ"
              + " trước");
    }

    List<InventoryTransaction> stockMovements = new ArrayList<>();
    var canceller = currentUserProvider.getCurrentUser();
    for (OrderItem item : orderItemRepository.findByOrderId(order.getId())) {
      BigDecimal remainingQty = item.getQuantity().subtract(item.getReturnedQuantity());
      if (remainingQty.compareTo(BigDecimal.ZERO) <= 0) {
        continue;
      }
      Inventory inventory =
          inventoryRepository
              .findByProductIdAndBranchId(item.getProduct().getId(), order.getBranch().getId())
              .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tồn kho"));
      inventory.setStock(inventory.getStock().add(remainingQty));
      inventoryRepository.saveAndFlush(inventory);

      InventoryTransaction transaction = new InventoryTransaction();
      transaction.setProduct(item.getProduct());
      transaction.setBranch(order.getBranch());
      transaction.setType("cancel");
      transaction.setQuantity(remainingQty);
      transaction.setUnitCost(item.getCostPriceSnapshot());
      transaction.setReferenceType("order");
      transaction.setReferenceId(order.getId());
      canceller.ifPresent(transaction::setCreatedBy);
      stockMovements.add(transaction);
    }
    inventoryTransactionRepository.saveAll(stockMovements);

    for (Debt debt : orderDebts) {
      debt.setAmount(BigDecimal.ZERO);
      debtRepository.save(debt);
    }

    order.setStatus(OrderStatus.CANCELLED.getValue());
    orderRepository.save(order);
    return toResponse(order);
  }

  private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
  private static final Map<String, String> PAYMENT_METHOD_LABELS =
      Map.of("cash", "Tiền mặt", "bank_transfer", "Chuyển khoản", "card", "Thẻ");

  /** Danh sach don hang co loc (FH-9) — dung cho trang Don hang. */
  @Transactional(readOnly = true)
  public ApiResponse<List<OrderListItemResponse>> list(
      Long branchId,
      LocalDate from,
      LocalDate to,
      String status,
      Long cashierId,
      String search,
      Pageable pageable) {
    branchAccessGuard.assertAccess(branchId);
    OffsetDateTime fromDateTime =
        from == null ? null : from.atStartOfDay(APP_ZONE).toOffsetDateTime();
    OffsetDateTime toDateTime =
        to == null ? null : to.plusDays(1).atStartOfDay(APP_ZONE).toOffsetDateTime();
    Page<Object[]> page =
        orderRepository.search(
            branchId,
            fromDateTime,
            toDateTime,
            status,
            cashierId,
            search == null ? "" : search.trim(),
            TenantContext.get(),
            pageable);
    Page<OrderListItemResponse> mapped = page.map(OrderService::toListItem);
    return ApiResponse.page(mapped);
  }

  private static OrderListItemResponse toListItem(Object[] row) {
    OrderListItemResponse response = new OrderListItemResponse();
    response.setId(((Number) row[0]).longValue());
    response.setOrderNumber((String) row[1]);
    response.setCreatedAt(toInstant(row[2]));
    response.setStatus((String) row[3]);
    response.setTotalAmount((BigDecimal) row[4]);
    response.setCustomerName((String) row[5]);
    response.setCustomerPhone((String) row[6]);
    response.setCashierName((String) row[7]);
    response.setHasDebt((Boolean) row[8]);
    String rawMethods = (String) row[9];
    response.setPaymentMethods(
        rawMethods == null || rawMethods.isBlank()
            ? List.of()
            : Arrays.stream(rawMethods.split(","))
                .map(m -> PAYMENT_METHOD_LABELS.getOrDefault(m, m))
                .toList());
    return response;
  }

  private OrderResponse toResponse(Order order) {
    OrderResponse response = new OrderResponse();
    response.setId(order.getId());
    response.setOrderNumber(order.getOrderNumber());
    response.setStatus(order.getStatus());
    response.setBranchId(order.getBranch().getId());
    response.setCustomerId(order.getCustomer() == null ? null : order.getCustomer().getId());
    response.setSubtotalAmount(order.getSubtotalAmount());
    response.setDiscountAmount(order.getDiscountAmount());
    response.setVatAmount(order.getVatAmount());
    response.setRoundingAdjustment(order.getRoundingAdjustment());
    response.setTotalAmount(order.getTotalAmount());
    response.setShippingFee(order.getShippingFee());
    response.setNote(order.getNote());
    response.setCreatedAt(order.getCreatedAt());

    List<OrderItemResponse> items =
        orderItemRepository.findByOrderId(order.getId()).stream()
            .map(
                item -> {
                  OrderItemResponse itemResponse = new OrderItemResponse();
                  itemResponse.setId(item.getId());
                  itemResponse.setProductId(item.getProduct().getId());
                  itemResponse.setProductName(item.getProductNameSnapshot());
                  itemResponse.setUnitPrice(item.getUnitPriceSnapshot());
                  itemResponse.setQuantity(item.getQuantity());
                  itemResponse.setDiscountAmount(item.getDiscountAmount());
                  itemResponse.setVatAmount(item.getVatAmount());
                  itemResponse.setLineTotal(item.getLineTotal());
                  itemResponse.setReturnedQuantity(item.getReturnedQuantity());
                  return itemResponse;
                })
            .toList();
    response.setItems(items);

    List<OrderPaymentResponse> payments =
        orderPaymentRepository.findByOrderId(order.getId()).stream()
            .map(
                payment -> {
                  OrderPaymentResponse paymentResponse = new OrderPaymentResponse();
                  paymentResponse.setMethod(payment.getMethod());
                  paymentResponse.setAmount(payment.getAmount());
                  return paymentResponse;
                })
            .toList();
    response.setPayments(payments);

    invoiceRepository
        .findByOrderId(order.getId())
        .ifPresent(
            invoice -> {
              response.setInvoiceId(invoice.getId());
              response.setInvoiceNumber(invoice.getInvoiceNumber());
              response.setQrPayload(invoice.getQrPayload());
              response.setLookupCode(invoice.getLookupCode());
            });

    return response;
  }

  private String generateOrderNumber() {
    String prefix = settingsService.getValue(null, SettingsService.KEY_ORDER_NUMBER_PREFIX, "HD-");
    long sequence = numberSequenceService.nextValue(NumberSequenceService.ORDER_NUMBER_SEQ);
    return prefix + String.format("%06d", sequence);
  }

  private String generateInvoiceNumber() {
    String prefix =
        settingsService.getValue(null, SettingsService.KEY_INVOICE_NUMBER_PREFIX, "INV-");
    long sequence = numberSequenceService.nextValue(NumberSequenceService.INVOICE_NUMBER_SEQ);
    return prefix + String.format("%06d", sequence);
  }
}
