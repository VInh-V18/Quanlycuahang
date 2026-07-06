package com.quanlycuahang.erp.report.excel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.function.Function;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

/**
 * Xuat 1 bang bao cao ra file .xlsx (Apache POI, Phase 10) — dung chung cho moi loai bao cao dang
 * bang (khong viet rieng tung report 1 class export).
 */
@Component
public class ReportExcelExporter {

  public <T> byte[] export(
      String sheetName, List<String> headers, List<T> rows, Function<T, Object[]> rowMapper) {
    try (XSSFWorkbook workbook = new XSSFWorkbook()) {
      XSSFSheet sheet = workbook.createSheet(sheetName);

      Font headerFont = workbook.createFont();
      headerFont.setBold(true);
      CellStyle headerStyle = workbook.createCellStyle();
      headerStyle.setFont(headerFont);

      Row headerRow = sheet.createRow(0);
      for (int i = 0; i < headers.size(); i++) {
        Cell cell = headerRow.createCell(i);
        cell.setCellValue(headers.get(i));
        cell.setCellStyle(headerStyle);
      }

      int rowIndex = 1;
      for (T row : rows) {
        Row dataRow = sheet.createRow(rowIndex++);
        Object[] values = rowMapper.apply(row);
        for (int col = 0; col < values.length; col++) {
          setCellValue(dataRow.createCell(col), values[col]);
        }
      }

      for (int i = 0; i < headers.size(); i++) {
        sheet.autoSizeColumn(i);
      }

      ByteArrayOutputStream out = new ByteArrayOutputStream();
      workbook.write(out);
      return out.toByteArray();
    } catch (IOException e) {
      throw new UncheckedIOException("Xuat Excel that bai", e);
    }
  }

  private void setCellValue(Cell cell, Object value) {
    if (value == null) {
      cell.setBlank();
    } else if (value instanceof BigDecimal bigDecimal) {
      cell.setCellValue(bigDecimal.doubleValue());
    } else if (value instanceof Number number) {
      cell.setCellValue(number.doubleValue());
    } else {
      cell.setCellValue(String.valueOf(value));
    }
  }
}
