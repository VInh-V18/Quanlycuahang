package com.quanlycuahang.erp.operation.service;

import com.quanlycuahang.erp.auth.security.TenantSessionBinder;
import com.quanlycuahang.erp.operation.dto.InvoiceDetailResponse;
import jakarta.mail.internet.MimeMessage;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Gui email hoa don qua template Thymeleaf (Phase 9) — luon goi SAU KHI transaction ban hang da
 * commit (xem InvoiceEmailListener), khong bao gio lam that bai/rollback don hang chi vi loi SMTP.
 * Moi loi deu bat va log lai, khong nem tiep len tren.
 */
@Service
public class InvoiceEmailService {

  private static final Logger log = LoggerFactory.getLogger(InvoiceEmailService.class);

  private final JavaMailSender mailSender;
  private final TemplateEngine templateEngine;
  private final boolean enabled;
  private final String fromAddress;
  private final InvoiceDetailService invoiceDetailService;
  private final TenantSessionBinder tenantSessionBinder;

  public InvoiceEmailService(
      JavaMailSender mailSender,
      TemplateEngine templateEngine,
      @Value("${app.email.enabled:true}") boolean enabled,
      @Value("${app.email.from}") String fromAddress,
      InvoiceDetailService invoiceDetailService,
      TenantSessionBinder tenantSessionBinder) {
    this.mailSender = mailSender;
    this.templateEngine = templateEngine;
    this.enabled = enabled;
    this.fromAddress = fromAddress;
    this.invoiceDetailService = invoiceDetailService;
    this.tenantSessionBinder = tenantSessionBinder;
  }

  /**
   * Diem vao CHAY NEN cho InvoiceEmailListener — truoc day listener goi thang
   * InvoiceDetailService.getById()+sendInvoiceEmail() dong bo NGAY TREN thread xu ly request
   * checkout, nghia la khach phai cho them thoi gian SMTP tra loi (co the vai giay hoac treo neu
   * mail server cham) truoc khi thay ket qua thanh toan (phat hien khi rieng soat hieu nang).
   *
   * <p>@Async chi co hieu luc khi goi tu BEAN KHAC (proxy Spring), vi vay logic nay nam o day chu
   * khong phai ngay trong InvoiceEmailListener (goi noi bo qua "this" se bo qua proxy va van chay
   * dong bo). tenantId phai duoc tham so hoa (khong doc lai TenantContext.get() trong ham nay) vi
   * ham chay tren THREAD MOI cua executor — ThreadLocal cua thread goc khong tu ke thua sang day.
   */
  @Async
  public void sendInvoiceEmailAsync(Long invoiceId, Long tenantId) {
    EntityManager entityManager = tenantSessionBinder.bind(tenantId);
    try {
      InvoiceDetailResponse invoice = invoiceDetailService.getByIdForSystemEmail(invoiceId);
      if (invoice.getCustomerEmail() == null || invoice.getCustomerEmail().isBlank()) {
        return;
      }
      sendInvoiceEmail(invoice, invoice.getCustomerEmail());
    } catch (Exception ex) {
      log.error(
          "Xu ly gui email hoa don nen that bai cho invoiceId={}: {}",
          invoiceId,
          ex.getMessage(),
          ex);
    } finally {
      tenantSessionBinder.unbind(entityManager);
    }
  }

  public void sendInvoiceEmail(InvoiceDetailResponse invoice, String toEmail) {
    if (!enabled) {
      log.info(
          "Gui email hoa don dang tat (app.email.enabled=false) — bo qua {}",
          invoice.getInvoiceNumber());
      return;
    }
    try {
      Context context = new Context();
      context.setVariable("invoice", invoice);

      String html = templateEngine.process("invoice-email", context);

      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
      helper.setFrom(fromAddress);
      helper.setTo(toEmail);
      helper.setSubject("Hoa don " + invoice.getInvoiceNumber() + " - " + invoice.getStoreName());
      helper.setText(html, true);

      mailSender.send(message);
      log.info("Da gui email hoa don {} toi {}", invoice.getInvoiceNumber(), toEmail);
    } catch (Exception ex) {
      log.error(
          "Gui email hoa don {} that bai: {}", invoice.getInvoiceNumber(), ex.getMessage(), ex);
    }
  }
}
