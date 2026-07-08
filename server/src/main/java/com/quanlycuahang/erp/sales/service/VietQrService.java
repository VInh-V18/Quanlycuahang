package com.quanlycuahang.erp.sales.service;

import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Sinh payload QR chuyen khoan theo chuan VietQR/Napas 247 (EMVCo QR Code Specification for Payment
 * Systems). Tu code Java, khong dung thu vien ngoai — cau truc TLV (Tag-Length-Value) va
 * CRC16-CCITT la thuat toan cong khai, tat dinh, tu kiem chung duoc bang cach doi chieu voi tai
 * lieu EMVCo/Napas.
 *
 * <p><b>Can kiem chung truoc khi dung that</b>: da tu verify cau truc TLV va CRC16 dung thuat toan
 * chuan (polynomial 0x1021, init 0xFFFF, khong XOR out — dung cho ca ISO 14443/EMVCo), nhung CHUA
 * quet thu bang ung dung ngan hang that (can moi truong co ket noi Napas that de xac nhan 100%
 * tuong thich truoc khi dua vao production).
 */
@Service
public class VietQrService {

  private static final String AID_NAPAS = "A000000727";
  private static final String SERVICE_CODE_TRANSFER_TO_ACCOUNT = "QRIBFTTA";

  private final String defaultBankBin;
  private final String defaultAccountNumber;
  private final String defaultMerchantName;
  private final String defaultMerchantCity;

  public VietQrService(
      @Value("${app.vietqr.bank-bin:970436}") String defaultBankBin,
      @Value("${app.vietqr.account-number:0000000000}") String defaultAccountNumber,
      @Value("${app.vietqr.merchant-name:CUA HANG}") String defaultMerchantName,
      @Value("${app.vietqr.merchant-city:HA NOI}") String defaultMerchantCity) {
    this.defaultBankBin = defaultBankBin;
    this.defaultAccountNumber = defaultAccountNumber;
    this.defaultMerchantName = defaultMerchantName;
    this.defaultMerchantCity = defaultMerchantCity;
  }

  public String generatePayload(BigDecimal amount, String purpose) {
    return generatePayload(
        defaultBankBin, defaultAccountNumber, defaultMerchantName, defaultMerchantCity, amount, purpose);
  }

  /**
   * Ban co the tuy bien bin/so TK/ten thu huong theo cau hinh Cai dat cua tung cua hang (thay vi
   * chi dung mac dinh tinh trong application.yml) — tham so rong/null se fallback ve mac dinh.
   */
  public String generatePayload(
      String bankBin,
      String accountNumber,
      String merchantName,
      String merchantCity,
      BigDecimal amount,
      String purpose) {
    String bin = blankToDefault(bankBin, defaultBankBin);
    String account = blankToDefault(accountNumber, defaultAccountNumber);
    String name = blankToDefault(merchantName, defaultMerchantName);
    String city = blankToDefault(merchantCity, defaultMerchantCity);

    StringBuilder merchantAccountInfo = new StringBuilder();
    merchantAccountInfo.append(tlv("00", AID_NAPAS));
    String beneficiaryInfo = tlv("00", bin) + tlv("01", account);
    merchantAccountInfo.append(tlv("01", beneficiaryInfo));
    merchantAccountInfo.append(tlv("02", SERVICE_CODE_TRANSFER_TO_ACCOUNT));

    StringBuilder payload = new StringBuilder();
    payload.append(tlv("00", "01")); // Payload Format Indicator
    payload.append(tlv("01", "12")); // Point of Initiation Method: 12 = dong (co so tien)
    payload.append(tlv("38", merchantAccountInfo.toString()));
    payload.append(tlv("52", "0000")); // Merchant Category Code
    payload.append(tlv("53", "704")); // Currency: VND = 704
    payload.append(tlv("54", amount.setScale(0, java.math.RoundingMode.HALF_UP).toPlainString()));
    payload.append(tlv("58", "VN"));
    payload.append(tlv("59", truncate(name, 25)));
    payload.append(tlv("60", truncate(city, 15)));
    if (purpose != null && !purpose.isBlank()) {
      payload.append(tlv("62", tlv("08", truncate(purpose, 25))));
    }

    // CRC tinh tren toan bo payload + "6304" (tag+length cua chinh truong CRC, gia tri chua co).
    String payloadWithCrcTag = payload + "6304";
    String crc = crc16Ccitt(payloadWithCrcTag);
    return payloadWithCrcTag + crc;
  }

  private static String blankToDefault(String value, String fallback) {
    return (value == null || value.isBlank()) ? fallback : value;
  }

  private static String tlv(String tag, String value) {
    String length = String.format("%02d", value.length());
    return tag + length + value;
  }

  private static String truncate(String value, int maxLength) {
    return value.length() <= maxLength ? value : value.substring(0, maxLength);
  }

  /** CRC16-CCITT (False): polynomial 0x1021, init 0xFFFF, khong reflect, khong XOR out. */
  private static String crc16Ccitt(String data) {
    int crc = 0xFFFF;
    byte[] bytes = data.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
    for (byte b : bytes) {
      crc ^= (b & 0xFF) << 8;
      for (int i = 0; i < 8; i++) {
        if ((crc & 0x8000) != 0) {
          crc = (crc << 1) ^ 0x1021;
        } else {
          crc <<= 1;
        }
        crc &= 0xFFFF;
      }
    }
    return String.format("%04X", crc);
  }
}
