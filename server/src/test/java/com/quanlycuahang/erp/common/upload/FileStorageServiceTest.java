package com.quanlycuahang.erp.common.upload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.quanlycuahang.erp.common.exception.BusinessRuleException;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

/**
 * Prompt #4 (audit upload — mục 6): xác nhận magic-byte thật (không tin Content-Type client khai
 * báo), chống path traversal, và định dạng tên file lưu ra đĩa hợp lệ trước khi resolve() (lớp
 * phòng thủ bổ sung phát hiện khi audit lần này).
 */
class FileStorageServiceTest {

  private FileStorageService service;

  @BeforeEach
  void setUp(@TempDir Path tempDir) {
    service = new FileStorageService(tempDir.toString());
  }

  private static final byte[] PNG_MAGIC_BYTES = {
    (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0
  };

  @Test
  void storesValidPngAndReturnsUuidNamedFile() {
    MockMultipartFile file =
        new MockMultipartFile("file", "anh-that.png", "image/png", PNG_MAGIC_BYTES);

    String storedName = service.store(file);

    assertThat(storedName).endsWith(".png");
    assertThat(storedName).doesNotContain("anh-that");
  }

  @Test
  void rejectsContentClaimingPngButWithoutRealPngMagicBytes() {
    MockMultipartFile fakeFile =
        new MockMultipartFile("file", "malware.png", "image/png", "khong phai anh that".getBytes());

    assertThatThrownBy(() -> service.store(fakeFile))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("không khớp");
  }

  @Test
  void rejectsDisallowedMimeType() {
    MockMultipartFile file =
        new MockMultipartFile("file", "script.svg", "image/svg+xml", "<svg></svg>".getBytes());

    assertThatThrownBy(() -> service.store(file))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("jpg/png/webp");
  }

  @Test
  void resolveRejectsPathTraversalAttempt() {
    assertThatThrownBy(() -> service.resolve("../../etc/passwd"))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void resolveRejectsNameNotMatchingStoredUuidFormat() {
    // Ten file KHONG dung dinh dang UUID+duoi anh ma store() luon sinh ra - vd 1 fileName tuy y
    // client tu go vao URL, ke ca khi khong con ky tu path traversal nao ("../").
    assertThatThrownBy(() -> service.resolve("khong-phai-uuid.png"))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void resolveAcceptsValidStoredNameFormat() {
    Path resolved = service.resolve("11111111-1111-1111-1111-111111111111.png");
    assertThat(resolved.toString()).endsWith("11111111-1111-1111-1111-111111111111.png");
  }
}
