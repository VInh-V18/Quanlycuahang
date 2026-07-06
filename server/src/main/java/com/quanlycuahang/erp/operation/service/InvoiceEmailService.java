package com.quanlycuahang.erp.operation.service;

import com.quanlycuahang.erp.operation.dto.InvoiceDetailResponse;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
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

  public InvoiceEmailService(
      JavaMailSender mailSender,
      TemplateEngine templateEngine,
      @Value("${app.email.enabled:true}") boolean enabled,
      @Value("${app.email.from}") String fromAddress) {
    this.mailSender = mailSender;
    this.templateEngine = templateEngine;
    this.enabled = enabled;
    this.fromAddress = fromAddress;
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
