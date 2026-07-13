package com.quanlycuahang.erp.common.upload;

import com.quanlycuahang.erp.common.exception.BusinessRuleException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Luu file upload (anh san pham...) ra thu muc ngoai webroot (D3): whitelist MIME jpg/png/webp +
 * kiem tra magic-byte thuc su cua noi dung (khong chi tin Content-Type client khai bao), doi ten
 * UUID (khong giu ten goc de tranh path traversal/xung dot), serve lai qua endpoint rieng
 * (FileUploadController) chi tra byte anh, khong thuc thi duoc.
 */
@Service
public class FileStorageService {

  private static final Set<String> ALLOWED_CONTENT_TYPES =
      Set.of("image/jpeg", "image/png", "image/webp");

  /**
   * Ten file luu ra dia LUON dung format UUID + duoi anh (xem store()) - kiem tra dung format nay
   * TRUOC khi resolve() la 1 lop phong thu bo sung (Prompt #4 audit): dam bao khong the request
   * duoc 1 fileName chua ky tu la (vd dau ngoac kep) co the anh huong header Content-Disposition o
   * FileUploadController.serve(), ke ca khi normalize()+kiem tra parent o duoi khong bat duoc.
   */
  private static final Pattern VALID_STORED_NAME =
      Pattern.compile("^[0-9a-fA-F-]{36}\\.(jpg|png|webp)$");

  private final Path uploadDir;

  public FileStorageService(@Value("${app.upload.dir}") String uploadDir) {
    this.uploadDir = Path.of(uploadDir).toAbsolutePath().normalize();
    try {
      Files.createDirectories(this.uploadDir);
    } catch (IOException ex) {
      throw new UncheckedIOException("Khong the tao thu muc upload: " + this.uploadDir, ex);
    }
  }

  /**
   * @return ten file da luu (UUID + duoi mo rong), dung de tra ve URL qua endpoint rieng.
   */
  public String store(MultipartFile file) {
    String contentType = file.getContentType();
    if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
      throw new BusinessRuleException(
          "UPLOAD_INVALID_MIME_TYPE", "Chỉ chấp nhận file ảnh jpg/png/webp");
    }
    if (file.isEmpty()) {
      throw new BusinessRuleException("UPLOAD_EMPTY_FILE", "File rỗng");
    }
    validateMagicBytes(file, contentType);

    String extension = extensionFor(contentType);
    String storedName = UUID.randomUUID() + extension;
    Path target = uploadDir.resolve(storedName).normalize();
    if (!target.getParent().equals(uploadDir)) {
      // Phong ngua path traversal du ten file da la UUID tu sinh (khong bao gio xay ra, nhung giu
      // lam luoi an toan).
      throw new BusinessRuleException("UPLOAD_INVALID_PATH", "Đường dẫn file không hợp lệ");
    }

    try {
      file.transferTo(target);
    } catch (IOException ex) {
      throw new UncheckedIOException("Khong the luu file upload", ex);
    }
    return storedName;
  }

  public Path resolve(String storedName) {
    if (storedName == null || !VALID_STORED_NAME.matcher(storedName).matches()) {
      throw new BusinessRuleException("UPLOAD_INVALID_PATH", "Đường dẫn file không hợp lệ");
    }
    Path target = uploadDir.resolve(storedName).normalize();
    if (!target.getParent().equals(uploadDir)) {
      throw new BusinessRuleException("UPLOAD_INVALID_PATH", "Đường dẫn file không hợp lệ");
    }
    return target;
  }

  /**
   * Kiem tra magic-byte THAT cua noi dung file, khong chi tin vao header Content-Type do client tu
   * khai bao (client co the gui Content-Type: image/png voi noi dung byte bat ky) - phat hien khi
   * rieng soat bao mat. Khong dung ImageIO.read() vi ImageIO khong ho tro giai ma WebP mac dinh (se
   * tu choi nham ca file webp hop le), nen tu kiem chu ky byte dau file cho tung dinh dang.
   */
  private void validateMagicBytes(MultipartFile file, String contentType) {
    byte[] header;
    try (var in = file.getInputStream()) {
      header = in.readNBytes(12);
    } catch (IOException ex) {
      throw new UncheckedIOException("Khong the doc file upload de kiem tra dinh dang", ex);
    }
    boolean valid =
        switch (contentType) {
          case "image/jpeg" ->
              header.length >= 3
                  && (header[0] & 0xFF) == 0xFF
                  && (header[1] & 0xFF) == 0xD8
                  && (header[2] & 0xFF) == 0xFF;
          case "image/png" ->
              header.length >= 8
                  && (header[0] & 0xFF) == 0x89
                  && header[1] == 'P'
                  && header[2] == 'N'
                  && header[3] == 'G'
                  && header[4] == 0x0D
                  && header[5] == 0x0A
                  && header[6] == 0x1A
                  && header[7] == 0x0A;
          case "image/webp" ->
              header.length >= 12
                  && header[0] == 'R'
                  && header[1] == 'I'
                  && header[2] == 'F'
                  && header[3] == 'F'
                  && header[8] == 'W'
                  && header[9] == 'E'
                  && header[10] == 'B'
                  && header[11] == 'P';
          default -> false;
        };
    if (!valid) {
      throw new BusinessRuleException(
          "UPLOAD_CONTENT_MISMATCH", "Nội dung file không khớp định dạng ảnh đã khai báo");
    }
  }

  private String extensionFor(String contentType) {
    return switch (contentType) {
      case "image/jpeg" -> ".jpg";
      case "image/png" -> ".png";
      case "image/webp" -> ".webp";
      default -> "";
    };
  }
}
