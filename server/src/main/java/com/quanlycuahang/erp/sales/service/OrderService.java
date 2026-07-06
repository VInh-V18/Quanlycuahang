package com.quanlycuahang.erp.sales.service;

import com.quanlycuahang.erp.auth.security.BranchAccessGuard;
import com.quanlycuahang.erp.auth.security.CurrentUserProvider;
import com.quanlycuahang.erp.common.dto.ApiResponse;
import com.quanlycuahang.erp.common.exception.BusinessRuleException;
import com.quanlycuahang.erp.common.exception.ResourceNotFoundException;
import com.quanlycuahang.erp.common.sequence.NumberSequenceService;
import com.quanlycuahang.erp.common.web.IdempotencyService;
import com.quanlycuahang.erp.inventory.entity.Inventory;
import com.quanlycuahang.erp.inventory.entity.InventoryTransaction;
import com.quanlycuahang.erp.inventory.repository.InventoryRepository;
import com.quanlycuahang.erp.inventory.repository.InventoryTransactionRepository;
import com.quanlycuahang.erp.operation.entity.Invoice;
import com.quanlycuahang.erp.operation.entity.Shift;
import com.quanlycuahang.erp.operation.invoice.InvoiceCreatedEvent;
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
import com.quanlycuahang.erp.sales.dto.OrderLineRequest;
import com.quanlycuahang.erp.sales.dto.OrderListItemResponse;
import com.quanlycuahang.erp.sales.dto.OrderPaymentResponse;
import com.quanlycuahang.erp.sales.dto.OrderResponse;
import com.quanlycuahang.erp.sales.entity.Order;
import com.quanlycuahang.erp.sales.entity.OrderItem;
import com.quanlycuahang.erp.sales.entity.OrderPayment;
import com.quanlycuahang.erp.sales.pricing.OrderLineInput;
import com.quanlycuahang.erp.sales.pricing.OrderLineResult;
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
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ban hang POS (UC-04, QUAN TRONG NHAT) — createOrder() chay trong 1 @Transactional duy nhat: tinh
 * tien doc lap bang OrderPricingService, chong oversell qua @Version (optimistic locking, B4), ghi
 * day du OrderItem/InventoryTransaction/Invoice/OrderPayment/Debt.
 */
@Service
public class OrderService {

  private final OrderRepository orderRepository;
  private final OrderItemRepository orderItemRepository;
  private final OrderPaymentRepository orderPaymentRepository;
  private final ProductRepository productRepository;
  private final InventoryRepository inventoryRepository;
  private final InventoryTransactionRepository inventoryTransactionRepository;
  private final BranchRepository branchRepository;
  private final CustomerRepository customerRepository;
  private final ShiftRepository shiftRepository;
  private final DebtRepository debtRepository;
  private final InvoiceRepository invoiceRepository;
  private final VoucherService voucherService;
  private final VietQrService vietQrService;
  private final SettingsService settingsService;
  private final CurrentUserProvider currentUserProvider;
  private final IdempotencyService idempotencyService;
  private final NumberSequenceService numberSequenceService;
  private final ApplicationEventPublisher eventPublisher;
  private final BranchAccessGuard branchAccessGuard;

  public OrderService(
      OrderRepository orderRepository,
      OrderItemRepository orderItemRepository,
      OrderPaymentRepository orderPaymentRepository,
      ProductRepository productRepository,
      InventoryRepository inventoryRepository,
      InventoryTransactionRepository inventoryTransactionRepository,
      BranchRepository branchRepository,
      CustomerRepository customerRepository,
      ShiftRepository shiftRepository,
      DebtRepository debtRepository,
      InvoiceRepository invoiceRepository,
      VoucherService voucherService,
      VietQrService vietQrService,
      SettingsService settingsService,
      CurrentUserProvider currentUserProvider,
      IdempotencyService idempotencyService,
      NumberSequenceService numberSequenceService,
      ApplicationEventPublisher eventPublisher,
      BranchAccessGuard branchAccessGuard) {
    this.orderRepository = orderRepository;
    this.orderItemRepository = orderItemRepository;
    this.orderPaymentRepository = orderPaymentRepository;
    this.productRepository = productRepository;
    this.inventoryRepository = inventoryRepository;
    this.inventoryTransactionRepository = inventoryTransactionRepository;
    this.branchRepository = branchRepository;
    this.customerRepository = customerRepository;
    this.shiftRepository = shiftRepository;
    this.debtRepository = debtRepository;
    this.invoiceRepository = invoiceRepository;
    this.voucherService = voucherService;
    this.vietQrService = vietQrService;
    this.settingsService = settingsService;
    this.currentUserProvider = currentUserProvider;
    this.idempotencyService = idempotencyService;
    this.numberSequenceService = numberSequenceService;
    this.eventPublisher = eventPublisher;
    this.branchAccessGuard = branchAccessGuard;
  }

  @Transactional
  public OrderResponse createOrder(OrderCreateRequest request, String idempotencyKey) {
    branchAccessGuard.assertAccess(request.getBranchId());
    Branch branch =
        branchRepository
            .findById(request.getBranchId())
            .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay chi nhanh"));

    Customer customer = null;
    if (request.getCustomerId() != null) {
      customer =
          customerRepository
              .findById(request.getCustomerId())
              .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay khach hang"));
    }

    Shift shift = null;
    if (request.getShiftId() != null) {
      shift =
          shiftRepository
              .findById(request.getShiftId())
              .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay ca lam viec"));
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
        request.getLines().stream().map(OrderLineRequest::getProductId).distinct().toList();
    Map<Long, Product> productsById =
        productRepository.findAllById(requestedProductIds).stream()
            .collect(java.util.stream.Collectors.toMap(Product::getId, p -> p));
    Map<Long, Inventory> inventoriesByProductId =
        inventoryRepository
            .findByBranchIdAndProductIdIn(branch.getId(), requestedProductIds)
            .stream()
            .collect(
                java.util.stream.Collectors.toMap(inv -> inv.getProduct().getId(), inv -> inv));

    List<OrderLineInput> pricingLines = new java.util.ArrayList<>();
    for (OrderLineRequest lineRequest : request.getLines()) {
      Product product = productsById.get(lineRequest.getProductId());
      if (product == null) {
        throw new ResourceNotFoundException("Khong tim thay san pham");
      }
      Inventory inventory = inventoriesByProductId.get(product.getId());
      if (inventory == null) {
        throw new BusinessRuleException(
            "PRODUCT_OUT_OF_STOCK",
            "San pham " + product.getName() + " chua co ton kho tai chi nhanh nay");
      }

      if (!allowNegativeStock && inventory.getStock().compareTo(lineRequest.getQuantity()) < 0) {
        throw new BusinessRuleException(
            "PRODUCT_OUT_OF_STOCK",
            "San pham " + product.getName() + " chi con " + inventory.getStock() + " trong kho");
      }

      pricingLines.add(
          new OrderLineInput(
              product.getId(),
              product.getSellPrice(),
              lineRequest.getQuantity(),
              lineRequest.getLineDiscountAmount(),
              product.getVatRate()));
    }

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

    // Chan tong CK don + voucher vuot subtotal — OrderPricingService la ham thuan, khong tu
    // validate (xem
    // OrderPricingServiceTest#orderDiscountExceedingLineSubtotalProducesNegativeLineTotal),
    // neu khong chan o day don co the co tong tien am, sai lech bao cao doanh thu/loi nhuan gop.
    BigDecimal orderLevelReduction = request.getOrderDiscountAmount().add(voucherAmount);
    if (orderLevelReduction.compareTo(roughSubtotal) > 0) {
      throw new BusinessRuleException(
          "ORDER_DISCOUNT_EXCEEDS_SUBTOTAL",
          "Tong chiet khau (don hang + voucher) khong duoc vuot qua gia tri don hang");
    }

    OrderPricingRequest pricingRequest =
        new OrderPricingRequest(
            pricingLines,
            request.getOrderDiscountAmount(),
            voucherAmount,
            priceIncludesVat,
            roundingUnit,
            request.getCashReceived());
    OrderPricingResult pricing = OrderPricingService.calculate(pricingRequest);

    if (pricing.getTotalAmount().compareTo(request.getExpectedTotalAmount()) != 0) {
      throw new BusinessRuleException(
          "ORDER_PRICE_MISMATCH",
          "So tien tinh toan khong khop, vui long tai lai gio hang va thu lai");
    }

    BigDecimal totalPaid =
        request.getPayments().stream()
            .map(com.quanlycuahang.erp.sales.dto.OrderPaymentRequest::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal unpaid = pricing.getTotalAmount().subtract(totalPaid);
    if (unpaid.compareTo(BigDecimal.ZERO) > 0 && customer == null) {
      throw new BusinessRuleException(
          "ORDER_UNPAID_REQUIRES_CUSTOMER", "Ban no phai chon khach hang cu the");
    }

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
    order.setTotalAmount(pricing.getTotalAmount());
    order.setCashReceived(request.getCashReceived());
    order.setChangeAmount(pricing.getChangeAmount());
    currentUserProvider.getCurrentUser().ifPresent(order::setCashier);
    order = orderRepository.save(order);

    List<OrderItemResponse> itemResponses = new java.util.ArrayList<>();
    for (OrderLineResult lineResult : pricing.getLines()) {
      Product product = productsById.get(lineResult.getProductId());
      Inventory inventory = inventoriesByProductId.get(lineResult.getProductId());

      OrderItem item = new OrderItem();
      item.setOrder(order);
      item.setProduct(product);
      item.setProductNameSnapshot(product.getName());
      item.setUnitPriceSnapshot(lineResult.getUnitPrice());
      item.setCostPriceSnapshot(inventory.getCostPrice());
      item.setVatRateSnapshot(product.getVatRate());
      item.setQuantity(lineResult.getQuantity());
      item.setDiscountAmount(lineResult.getDiscountAmount());
      item.setVatAmount(lineResult.getVatAmount());
      item.setLineTotal(lineResult.getLineTotal());
      orderItemRepository.save(item);

      // Chong oversell: @Version tren Inventory, saveAndFlush de phat hien xung dot NGAY,
      // khong cho doi den commit (B4 — chong oversell 2 lop).
      inventory.setStock(inventory.getStock().subtract(lineResult.getQuantity()));
      inventoryRepository.saveAndFlush(inventory);

      InventoryTransaction transaction = new InventoryTransaction();
      transaction.setProduct(product);
      transaction.setBranch(branch);
      transaction.setType("sale");
      transaction.setQuantity(lineResult.getQuantity().negate());
      transaction.setUnitCost(inventory.getCostPrice());
      transaction.setReferenceType("order");
      transaction.setReferenceId(order.getId());
      currentUserProvider.getCurrentUser().ifPresent(transaction::setCreatedBy);
      inventoryTransactionRepository.save(transaction);

      OrderItemResponse itemResponse = new OrderItemResponse();
      itemResponse.setId(item.getId());
      itemResponse.setProductId(product.getId());
      itemResponse.setProductName(product.getName());
      itemResponse.setUnitPrice(lineResult.getUnitPrice());
      itemResponse.setQuantity(lineResult.getQuantity());
      itemResponse.setDiscountAmount(lineResult.getDiscountAmount());
      itemResponse.setVatAmount(lineResult.getVatAmount());
      itemResponse.setLineTotal(lineResult.getLineTotal());
      itemResponses.add(itemResponse);
    }

    List<OrderPaymentResponse> paymentResponses = new java.util.ArrayList<>();
    boolean hasBankTransfer = false;
    for (var paymentRequest : request.getPayments()) {
      OrderPayment payment = new OrderPayment();
      payment.setOrder(order);
      payment.setMethod(paymentRequest.getMethod());
      payment.setAmount(paymentRequest.getAmount());
      if ("bank_transfer".equals(paymentRequest.getMethod())) {
        hasBankTransfer = true;
        payment.setQrPayload(
            vietQrService.generatePayload(
                paymentRequest.getAmount(), "Thanh toan " + order.getOrderNumber()));
      }
      orderPaymentRepository.save(payment);

      OrderPaymentResponse paymentResponse = new OrderPaymentResponse();
      paymentResponse.setMethod(payment.getMethod());
      paymentResponse.setAmount(payment.getAmount());
      paymentResponses.add(paymentResponse);
    }

    if (unpaid.compareTo(BigDecimal.ZERO) > 0) {
      Debt debt = new Debt();
      debt.setCustomer(customer);
      debt.setDirection("receivable");
      debt.setAmount(unpaid);
      debt.setOriginalAmount(unpaid);
      debt.setReferenceType("order");
      debt.setReferenceId(order.getId());
      debtRepository.save(debt);
    }

    if (voucher != null) {
      try {
        voucherService.recordUsage(voucher, order);
      } catch (org.springframework.dao.OptimisticLockingFailureException ex) {
        // @Version tren Voucher (them cung fix nay) phat hien 2 don dung cung 1 voucher gan het
        // luot cung luc — dich thanh loi ro rang thay vi de GlobalExceptionHandler map chung
        // thanh PRODUCT_OUT_OF_STOCK (sai ngu canh, gay hieu lam la ton kho chu khong phai
        // voucher).
        throw new BusinessRuleException(
            "VOUCHER_INVALID",
            "Voucher vua het luot su dung do co don khac dung cung luc, vui long tai lai gio hang");
      }
    }

    Invoice invoice = new Invoice();
    invoice.setOrder(order);
    invoice.setInvoiceNumber(generateInvoiceNumber());
    invoice.setLookupCode(UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    invoice.setIssuedAt(OffsetDateTime.now());
    if (hasBankTransfer) {
      invoice.setQrPayload(
          vietQrService.generatePayload(
              pricing.getTotalAmount(), "Thanh toan " + order.getOrderNumber()));
    }
    invoice = invoiceRepository.save(invoice);
    eventPublisher.publishEvent(new InvoiceCreatedEvent(invoice.getId()));

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
    response.setTotalAmount(pricing.getTotalAmount());
    response.setChangeAmount(pricing.getChangeAmount());
    response.setItems(itemResponses);
    response.setPayments(paymentResponses);
    response.setInvoiceId(invoice.getId());
    response.setInvoiceNumber(invoice.getInvoiceNumber());
    response.setQrPayload(invoice.getQrPayload());
    response.setLookupCode(invoice.getLookupCode());
    response.setCreatedAt(order.getCreatedAt());

    if (idempotencyKey != null && !idempotencyKey.isBlank()) {
      idempotencyService.complete(idempotencyKey, String.valueOf(order.getId()));
    }

    return response;
  }

  @Transactional(readOnly = true)
  public OrderResponse getById(Long id) {
    Order order =
        orderRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay don hang"));
    branchAccessGuard.assertAccess(order.getBranch().getId());
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

  private static Instant toInstant(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Instant instant) {
      return instant;
    }
    if (value instanceof OffsetDateTime odt) {
      return odt.toInstant();
    }
    if (value instanceof java.sql.Timestamp ts) {
      return ts.toInstant();
    }
    throw new IllegalStateException("Khong the chuyen doi thoi gian: " + value.getClass());
  }

  /**
   * Huy don hoan tat (UC-13): chi trong ngay tao don, hoan kho + cong no trong cung 1 transaction
   * (B4). Quyen han che qua @PreAuthorize o Controller (chi Quan ly/Chu cua hang co order:void —
   * Phase 1.1).
   */
  @Transactional
  public OrderResponse cancelOrder(Long id) {
    Order order =
        orderRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay don hang"));
    branchAccessGuard.assertAccess(order.getBranch().getId());
    OrderStatus current = OrderStatus.fromValue(order.getStatus());
    if (!OrderStatus.canTransition(current, OrderStatus.CANCELLED)) {
      throw new BusinessRuleException(
          "ORDER_CANCEL_NOT_ALLOWED",
          "Khong the huy don o trang thai hien tai: " + order.getStatus());
    }

    java.time.ZoneId zone = java.time.ZoneId.of("Asia/Ho_Chi_Minh");
    java.time.LocalDate orderDate = order.getCreatedAt().atZone(zone).toLocalDate();
    java.time.LocalDate today = java.time.OffsetDateTime.now(zone).toLocalDate();
    if (!orderDate.equals(today)) {
      throw new BusinessRuleException(
          "ORDER_CANCEL_WINDOW_EXPIRED", "Chi duoc huy don trong ngay tao don");
    }

    for (OrderItem item : orderItemRepository.findByOrderId(order.getId())) {
      BigDecimal remainingQty = item.getQuantity().subtract(item.getReturnedQuantity());
      if (remainingQty.compareTo(BigDecimal.ZERO) <= 0) {
        continue;
      }
      Inventory inventory =
          inventoryRepository
              .findByProductIdAndBranchId(item.getProduct().getId(), order.getBranch().getId())
              .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay ton kho"));
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
      currentUserProvider.getCurrentUser().ifPresent(transaction::setCreatedBy);
      inventoryTransactionRepository.save(transaction);
    }

    List<Debt> orderDebts =
        debtRepository.findByReferenceTypeAndReferenceId("order", order.getId());
    // Neu cong no da bi thu 1 phan (amount != originalAmount), khong duoc am tham xoa dau vet so
    // tien da thu bang cach zero thang — bat huy don, de nguoi dung xu ly cong no truoc (vd hoan
    // tien) roi moi huy.
    boolean hasPartialPayment =
        orderDebts.stream().anyMatch(d -> d.getAmount().compareTo(d.getOriginalAmount()) != 0);
    if (hasPartialPayment) {
      throw new BusinessRuleException(
          "ORDER_CANCEL_DEBT_ALREADY_PAID",
          "Khong the huy don vi cong no lien quan da duoc thu mot phan, vui long xu ly cong no truoc");
    }
    for (Debt debt : orderDebts) {
      debt.setAmount(BigDecimal.ZERO);
      debtRepository.save(debt);
    }

    order.setStatus(OrderStatus.CANCELLED.getValue());
    orderRepository.save(order);
    return toResponse(order);
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
