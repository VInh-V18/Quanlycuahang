package com.quanlycuahang.erp.common.upload;

import com.quanlycuahang.erp.common.dto.ApiResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Upload/serve anh (D3) — endpoint rieng, chi tra byte anh, khong thuc thi duoc. */
@RestController
@RequestMapping("/api/v1/uploads")
public class FileUploadController {

  private final FileStorageService fileStorageService;

  public FileUploadController(FileStorageService fileStorageService) {
    this.fileStorageService = fileStorageService;
  }

  @PostMapping
  @PreAuthorize(
      "hasAuthority('product:create') or hasAuthority('product:update') or hasAuthority('settings:update')")
  public ResponseEntity<ApiResponse<UploadResponse>> upload(
      @RequestParam("file") MultipartFile file) {
    String storedName = fileStorageService.store(file);
    UploadResponse response = new UploadResponse(storedName, "/api/v1/uploads/" + storedName);
    return ResponseEntity.ok(ApiResponse.success(response));
  }

  @GetMapping("/{fileName}")
  public ResponseEntity<Resource> serve(@PathVariable String fileName) throws IOException {
    Path path = fileStorageService.resolve(fileName);
    if (!Files.exists(path)) {
      return ResponseEntity.notFound().build();
    }
    Resource resource = new UrlResource(path.toUri());
    String contentType = Files.probeContentType(path);
    return ResponseEntity.ok()
        .contentType(
            contentType != null
                ? MediaType.parseMediaType(contentType)
                : MediaType.APPLICATION_OCTET_STREAM)
        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
        .header("X-Content-Type-Options", "nosniff")
        .body(resource);
  }
}
