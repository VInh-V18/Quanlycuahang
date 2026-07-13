package com.quanlycuahang.erp.ai.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.quanlycuahang.erp.common.exception.BusinessRuleException;
import java.util.Base64;
import org.junit.jupiter.api.Test;

/**
 * Prompt #11 - AES-256-GCM (encrypt/decrypt round-trip, IV ngau nhien moi lan ma hoa, tu choi khi
 * chua cau hinh khoa goc) - day la tien le DAU TIEN trong codebase can ma hoa 1 gia tri settings
 * nen kiem chung ky, khac voi phan lon service khac chi thao tac chuoi thuong.
 */
class AiKeyEncryptionServiceTest {

  private static final String VALID_BASE64_KEY = Base64.getEncoder().encodeToString(new byte[32]);

  @Test
  void encryptThenDecryptReturnsOriginalPlaintext() {
    AiKeyEncryptionService service = new AiKeyEncryptionService(VALID_BASE64_KEY);
    String plaintext = "sk-ant-super-secret-key-12345";

    String encrypted = service.encrypt(plaintext);
    String decrypted = service.decrypt(encrypted);

    assertThat(decrypted).isEqualTo(plaintext);
    assertThat(encrypted).isNotEqualTo(plaintext);
  }

  @Test
  void encryptUsesRandomIvSoRepeatedCallsProduceDifferentCiphertext() {
    AiKeyEncryptionService service = new AiKeyEncryptionService(VALID_BASE64_KEY);
    String plaintext = "sk-ant-super-secret-key-12345";

    String encryptedFirst = service.encrypt(plaintext);
    String encryptedSecond = service.encrypt(plaintext);

    assertThat(encryptedFirst).isNotEqualTo(encryptedSecond);
    assertThat(service.decrypt(encryptedFirst)).isEqualTo(plaintext);
    assertThat(service.decrypt(encryptedSecond)).isEqualTo(plaintext);
  }

  @Test
  void isConfiguredFalseWhenMasterKeyBlank() {
    AiKeyEncryptionService service = new AiKeyEncryptionService("");
    assertThat(service.isConfigured()).isFalse();
  }

  @Test
  void isConfiguredTrueWhenMasterKeySet() {
    AiKeyEncryptionService service = new AiKeyEncryptionService(VALID_BASE64_KEY);
    assertThat(service.isConfigured()).isTrue();
  }

  @Test
  void encryptThrowsBusinessRuleExceptionWhenMasterKeyNotConfigured() {
    AiKeyEncryptionService service = new AiKeyEncryptionService("");
    assertThatThrownBy(() -> service.encrypt("bat ky gia tri gi"))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("AI_SETTINGS_ENCRYPTION_KEY");
  }

  @Test
  void decryptThrowsBusinessRuleExceptionWhenMasterKeyNotConfigured() {
    AiKeyEncryptionService service = new AiKeyEncryptionService("");
    assertThatThrownBy(() -> service.decrypt("bat ky gia tri gi"))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void constructorRejectsKeyOfWrongLength() {
    String tooShortKey = Base64.getEncoder().encodeToString(new byte[16]);
    assertThatThrownBy(() -> new AiKeyEncryptionService(tooShortKey))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("32 byte");
  }

  @Test
  void constructorRejectsInvalidBase64() {
    assertThatThrownBy(() -> new AiKeyEncryptionService("not-valid-base64!!!"))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void decryptTamperedCiphertextFails() {
    AiKeyEncryptionService service = new AiKeyEncryptionService(VALID_BASE64_KEY);
    String encrypted = service.encrypt("gia tri goc");
    byte[] raw = Base64.getDecoder().decode(encrypted);
    raw[raw.length - 1] ^= 0x01; // lat 1 bit trong tag GCM - phai bi phat hien, khong duoc giai ma
    String tampered = Base64.getEncoder().encodeToString(raw);

    assertThatThrownBy(() -> service.decrypt(tampered)).isInstanceOf(IllegalStateException.class);
  }
}
