package com.quanlycuahang.erp.report.excel;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;

/**
 * Prompt #7 (P2, hieu nang): sau khi doi XSSFWorkbook (in-memory) sang SXSSFWorkbook (streaming),
 * test nay dam bao file .xlsx sinh ra VAN mo doc lai duoc binh thuong (dung dinh dang OOXML) va noi
 * dung (tieu de + du lieu) van dung, khong bi mat gi khi bo autoSizeColumn.
 */
class ReportExcelExporterTest {

  private record Row2(String label, BigDecimal amount) {}

  @Test
  void exportProducesReadableXlsxWithHeaderAndRows() throws Exception {
    ReportExcelExporter exporter = new ReportExcelExporter();
    List<Row2> rows =
        List.of(new Row2("Thang 1", BigDecimal.valueOf(1_000_000)), new Row2("Thang 2", null));

    byte[] file =
        exporter.export(
            "Doanh thu",
            List.of("Nhóm", "Số tiền"),
            rows,
            r -> new Object[] {r.label(), r.amount()});

    assertThat(file).isNotEmpty();

    try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(file))) {
      Sheet sheet = workbook.getSheet("Doanh thu");
      assertThat(sheet).isNotNull();

      Row header = sheet.getRow(0);
      assertThat(header.getCell(0).getStringCellValue()).isEqualTo("Nhóm");
      assertThat(header.getCell(1).getStringCellValue()).isEqualTo("Số tiền");

      Row dataRow1 = sheet.getRow(1);
      assertThat(dataRow1.getCell(0).getStringCellValue()).isEqualTo("Thang 1");
      assertThat(dataRow1.getCell(1).getNumericCellValue()).isEqualTo(1_000_000d);

      Row dataRow2 = sheet.getRow(2);
      assertThat(dataRow2.getCell(0).getStringCellValue()).isEqualTo("Thang 2");
      assertThat(dataRow2.getCell(1).getCellType())
          .isEqualTo(org.apache.poi.ss.usermodel.CellType.BLANK);
    }
  }

  @Test
  void exportHandlesLargeRowCountWithoutError() throws Exception {
    ReportExcelExporter exporter = new ReportExcelExporter();
    List<Row2> rows =
        java.util.stream.IntStream.range(0, 500)
            .mapToObj(i -> new Row2("SKU-" + i, BigDecimal.valueOf(i)))
            .toList();

    byte[] file =
        exporter.export(
            "Ton kho", List.of("SKU", "Gia tri"), rows, r -> new Object[] {r.label(), r.amount()});

    try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(file))) {
      Sheet sheet = workbook.getSheet("Ton kho");
      assertThat(sheet.getLastRowNum()).isEqualTo(500); // header (0) + 500 dong du lieu
    }
  }
}
