package com.quanlycuahang.erp.report.excel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Function;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * Xuat 1 bang bao cao ra file .xlsx (Apache POI, Phase 10) — dung chung cho moi loai bao cao dang
 * bang (khong viet rieng tung report 1 class export).
 *
 * <p>SXSSFWorkbook (streaming, khong phai XSSFWorkbook) — Prompt #7 (P2, hieu nang): du cac bao cao
 * tong hop (doanh thu/top san pham...) chi vai tram dong, InventoryController.export() dung CHUNG
 * ham nay de xuat NGUYEN bang ton kho toi da 10.000 dong/lan (PageRequest.of(0, 10_000)) —
 * XSSFWorkbook giu toan bo model trong RAM nen ton bo nho ti le thuan so dong; SXSSF chi giu 1 cua
 * so nho (100 dong mac dinh) trong RAM roi flush xuong file tam, giu RAM on dinh bat ke so dong.
 * Danh doi: KHONG the autoSizeColumn nua (can doc lai dong da flush) - thay bang do rong cot uoc
 * luong theo do dai tieu de, du dung cho da so bao cao (khong quan trong bang autoSizeColumn tuyet
 * doi chinh xac).
 */
@Component
public class ReportExcelExporter {

  private static final int MIN_COLUMN_WIDTH_CHARS = 12;
  private static final int MAX_COLUMN_WIDTH_CHARS = 40;
  private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
  private static final DateTimeFormatter DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(APP_ZONE);

  public <T> byte[] export(
      String sheetName, List<String> headers, List<T> rows, Function<T, Object[]> rowMapper) {
    // try-with-resources: SXSSFWorkbook.close() (POI 5.x) tu don file tam dung de flush dong,
    // khong can goi dispose() rieng (da bi deprecated, gop chung vao close()).
    try (SXSSFWorkbook workbook = new SXSSFWorkbook(100)) {
      Sheet sheet = workbook.createSheet(sheetName);

      Font headerFont = workbook.createFont();
      headerFont.setBold(true);
      CellStyle headerStyle = workbook.createCellStyle();
      headerStyle.setFont(headerFont);

      Row headerRow = sheet.createRow(0);
      for (int i = 0; i < headers.size(); i++) {
        Cell cell = headerRow.createCell(i);
        cell.setCellValue(headers.get(i));
        cell.setCellStyle(headerStyle);
        int widthChars =
            Math.min(
                MAX_COLUMN_WIDTH_CHARS,
                Math.max(MIN_COLUMN_WIDTH_CHARS, headers.get(i).length() + 4));
        sheet.setColumnWidth(i, widthChars * 256);
      }

      int rowIndex = 1;
      for (T row : rows) {
        Row dataRow = sheet.createRow(rowIndex++);
        Object[] values = rowMapper.apply(row);
        for (int col = 0; col < values.length; col++) {
          setCellValue(dataRow.createCell(col), values[col]);
        }
      }

      ByteArrayOutputStream out = new ByteArrayOutputStream();
      workbook.write(out);
      return out.toByteArray();
    } catch (IOException e) {
      throw new UncheckedIOException("Xuat Excel that bai", e);
    }
  }

  /**
   * Boc byte[] .xlsx thanh ResponseEntity tai ve — truoc day 3 Controller (Inventory/Order/Report)
   * moi noi tu viet lai y het doan nay rieng, chi ReportController co san 1 ham private trung ten
   * (phat hien khi rieng soat) — gop ve 1 cho duy nhat de ca 3 dung chung.
   */
  public ResponseEntity<byte[]> toXlsxResponse(byte[] content, String fileName) {
    return ResponseEntity.ok()
        .contentType(
            MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
        .body(content);
  }

  private void setCellValue(Cell cell, Object value) {
    if (value == null) {
      cell.setBlank();
    } else if (value instanceof BigDecimal bigDecimal) {
      cell.setCellValue(bigDecimal.doubleValue());
    } else if (value instanceof Instant instant) {
      // Instant.toString() mac dinh ra gio UTC dang ky thuat (vd "2026-07-16T07:15:30Z") — quy ve
      // gio Viet Nam, dinh dang nguoi dung binh thuong doc duoc, dung "chung 1 cho" cho MOI cho
      // xuat
      // Excel co truong Instant thay vi tung noi tu xu ly rieng (phat hien khi rieng soat don
      // hang).
      cell.setCellValue(DATE_TIME_FORMATTER.format(instant));
    } else if (value instanceof LocalDate localDate) {
      cell.setCellValue(DATE_FORMATTER.format(localDate));
    } else if (value instanceof Number number) {
      cell.setCellValue(number.doubleValue());
    } else {
      cell.setCellValue(String.valueOf(value));
    }
  }
}
