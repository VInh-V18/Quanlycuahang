package com.quanlycuahang.erp.ai.security;

import com.quanlycuahang.erp.common.exception.BusinessRuleException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Ma hoa/giai ma khoa API cua nha cung cap AI (tung tenant tu cau hinh) luc luu vao bang settings
 * (Prompt #11) — AES-256-GCM (co xac thuc, chong sua doi ban ma hoa) voi 1 khoa goc (master key)
 * cho toan he thong (KHONG phai khoa rieng tung tenant — khoa goc chi dung de bao ve khoa API cua
 * tenant luc luu tru, tenant khong bao gio thay/nhap khoa goc nay). Day la tien le DAU TIEN trong
 * codebase can ma hoa 1 gia tri settings (VietQR/SMTP truoc gio deu luu chuoi thuong, xem
 * SettingsService) — chi ap dung cho khoa API AI, KHONG dung lai cho cac setting khac chua co nhu
 * cau ma hoa.
 *
 * <p>Dinh dang chuoi luu tru: base64(iv[12 byte] || ciphertext-and-tag). IV ngau nhien MOI LAN ma
 * hoa (bat buoc voi GCM — tai su dung IV voi cung khoa se lam lo ban ro).
 */
@Component
public class AiKeyEncryptionService {

  private static final String ALGORITHM = "AES/GCM/NoPadding";
  private static final int IV_LENGTH_BYTES = 12;
  private static final int TAG_LENGTH_BITS = 128;

  private final SecretKeySpec masterKey;
  private final SecureRandom secureRandom = new SecureRandom();

  public AiKeyEncryptionService(
      @Value("${app.ai.settings-encryption-key:}") String base64MasterKey) {
    this.masterKey = base64MasterKey.isBlank() ? null : parseMasterKey(base64MasterKey);
  }

  private static SecretKeySpec parseMasterKey(String base64Key) {
    byte[] keyBytes;
    try {
      keyBytes = Base64.getDecoder().decode(base64Key);
    } catch (IllegalArgumentException ex) {
      throw new IllegalStateException(
          "AI_SETTINGS_ENCRYPTION_KEY phai la chuoi base64 hop le (32 byte sau khi giai ma cho"
              + " AES-256)",
          ex);
    }
    if (keyBytes.length != 32) {
      throw new IllegalStateException(
          "AI_SETTINGS_ENCRYPTION_KEY phai giai ma ra dung 32 byte (AES-256), hien tai la "
              + keyBytes.length
              + " byte. Sinh khoa moi: openssl rand -base64 32");
    }
    return new SecretKeySpec(keyBytes, "AES");
  }

  private void requireConfigured() {
    if (masterKey == null) {
      throw new BusinessRuleException(
          "AI_ENCRYPTION_NOT_CONFIGURED",
          "Chưa cấu hình khoá mã hoá cho tính năng AI (AI_SETTINGS_ENCRYPTION_KEY) — liên hệ quản"
              + " trị hệ thống");
    }
  }

  public String encrypt(String plaintext) {
    requireConfigured();
    try {
      byte[] iv = new byte[IV_LENGTH_BYTES];
      secureRandom.nextBytes(iv);
      Cipher cipher = Cipher.getInstance(ALGORITHM);
      cipher.init(Cipher.ENCRYPT_MODE, masterKey, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
      byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
      byte[] combined = new byte[iv.length + ciphertext.length];
      System.arraycopy(iv, 0, combined, 0, iv.length);
      System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);
      return Base64.getEncoder().encodeToString(combined);
    } catch (GeneralSecurityException ex) {
      throw new IllegalStateException("Ma hoa khoa API AI that bai", ex);
    }
  }

  public String decrypt(String encoded) {
    requireConfigured();
    try {
      byte[] combined = Base64.getDecoder().decode(encoded);
      byte[] iv = new byte[IV_LENGTH_BYTES];
      System.arraycopy(combined, 0, iv, 0, IV_LENGTH_BYTES);
      byte[] ciphertext = new byte[combined.length - IV_LENGTH_BYTES];
      System.arraycopy(combined, IV_LENGTH_BYTES, ciphertext, 0, ciphertext.length);
      Cipher cipher = Cipher.getInstance(ALGORITHM);
      cipher.init(Cipher.DECRYPT_MODE, masterKey, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
      return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
    } catch (GeneralSecurityException | IllegalArgumentException ex) {
      throw new IllegalStateException(
          "Giai ma khoa API AI that bai - co the khoa goc (AI_SETTINGS_ENCRYPTION_KEY) da bi doi"
              + " sau khi luu, hoac du lieu bi hong",
          ex);
    }
  }

  public boolean isConfigured() {
    return masterKey != null;
  }
}
