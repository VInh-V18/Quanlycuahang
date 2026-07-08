package com.quanlycuahang.erp.common.upload;

import com.quanlycuahang.erp.common.exception.BusinessRuleException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Luu file upload (anh san pham...) ra thu muc ngoai webroot (D3): whitelist MIME jpg/png/webp, doi
 * ten UUID (khong giu ten goc de tranh path traversal/xung dot), serve lai qua endpoint rieng
 * (FileUploadController) chi tra byte anh, khong thuc thi duoc.
 */
@Service
public class FileStorageService {

  private static final Set<String> ALLOWED_CONTENT_TYPES =
      Set.of("image/jpeg", "image/png", "image/webp");

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
          "UPLOAD_INVALID_MIME_TYPE", "Chi chap nhan file anh jpg/png/webp");
    }
    if (file.isEmpty()) {
      throw new BusinessRuleException("UPLOAD_EMPTY_FILE", "File rong");
    }

    String extension = extensionFor(contentType);
    String storedName = UUID.randomUUID() + extension;
    Path target = uploadDir.resolve(storedName).normalize();
    if (!target.getParent().equals(uploadDir)) {
      // Phong ngua path traversal du ten file da la UUID tu sinh (khong bao gio xay ra, nhung giu
      // lam luoi an toan).
      throw new BusinessRuleException("UPLOAD_INVALID_PATH", "Duong dan file khong hop le");
    }

    try {
      file.transferTo(target);
    } catch (IOException ex) {
      throw new UncheckedIOException("Khong the luu file upload", ex);
    }
    return storedName;
  }

  public Path resolve(String storedName) {
    Path target = uploadDir.resolve(storedName).normalize();
    if (!target.getParent().equals(uploadDir)) {
      throw new BusinessRuleException("UPLOAD_INVALID_PATH", "Duong dan file khong hop le");
    }
    return target;
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
