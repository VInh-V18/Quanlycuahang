package com.quanlycuahang.erp.ai.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Pool ket noi RIENG BIET cho AI (Prompt #11) - dung role Postgres {@code fruithouse_ai_readonly}
 * (V30, chi SELECT tren 4 view whitelist) thay vi DataSource chinh cua ung dung (Hibernate/JPA).
 *
 * <p><b>Vi sao khai bao lai CA DataSource chinh o day (khong chi them 1 bean moi)</b>: Spring Boot
 * tu dong cau hinh 1 {@code DataSource} DUY NHAT tu {@code spring.datasource.*}
 * (DataSourceAutoConfiguration), va dieu kien {@code @ConditionalOnMissingBean(DataSource.class)}
 * cua no se TU BO neu phat hien BAT KY bean DataSource nao khac da duoc dinh nghia (khong phan biet
 * ten/qualifier) - neu chi them 1 {@code @Bean DataSource} moi cho AI ma khong khai bao lai ban
 * chinh, toan bo DataSource chinh cua he thong (va JPA/Hibernate phu thuoc vao no) se BIEN MAT. Day
 * la hanh vi da tai lieu hoa cua Spring Boot cho kich ban "nhieu DataSource" — cach lam dung la
 * khai bao TUONG MINH ca 2 bean trong CUNG 1 nơi, danh dau bean chinh {@code @Primary} (JPA se tu
 * dong dung dung bean nay, khong can cau hinh gi them).
 *
 * <p><b>Vi sao dung {@link DataSourceProperties} thay vi bind thang {@code spring.datasource.*} vao
 * HikariDataSource</b>: da thu cach don gian hon truoc (bind truc tiep) va THAT BAI luc chay thu
 * ("dataSource or dataSourceClassName or jdbcUrl is required") - Boot dung ten thuoc tinh TRUU
 * TUONG ("url") khac ten thuoc tinh THAT SU cua Hikari ("jdbcUrl"), chi {@code
 * DataSourceProperties.initializeDataSourceBuilder()} moi biet cach "dich" dung. Bean {@code
 * primaryDataSource} duoi day vi vay dung dung 2 buoc chuan cua Spring Boot cho kich ban nay: (1)
 * bind {@code spring.datasource.*} (url/username/password) vao DataSourceProperties, (2) dung no de
 * build ra HikariDataSource, ROI @ConfigurationProperties them 1 lan nua tren chinh @Bean method de
 * ap {@code spring.datasource.hikari.*} (pool size, timeout... Prompt #7) len bean da tao - hanh
 * vi/cau hinh pool chinh vi vay KHONG doi gi so voi truoc, chi chuyen tu ngam dinh
 * (auto-configuration) sang tuong minh (khai bao tay).
 *
 * <p><b>Vi sao {@code @Qualifier("aiReadOnlyDataSource")} tren tham so cua {@code
 * aiReadOnlyJdbcTemplate} la BAT BUOC, khong the dua vao khop ten tham so</b>: da gap loi NGHIEM
 * TRONG luc kiem thu ({@code AiReadOnlyPermissionIT} bao permission denied nhung thuc te lai
 * ghi/xoa duoc du lieu that) - build Maven cua repo nay KHONG bat co {@code -parameters} cua javac,
 * nen Spring KHONG doc duoc ten tham so that (`aiReadOnlyDataSource`) tu bytecode de tu khop theo
 * ten bean cung ten; no roi ve autowire-theo-KIEU, va vi co 2 bean cung kieu {@code
 * HikariDataSource} ({@code primaryDataSource} dang danh dau {@code @Primary}), Spring chon NHAM
 * {@code primaryDataSource} (quyen ghi day du) thay vi pool chi-doc du dinh — im lang, khong loi
 * bien dich hay loi khoi dong nao ca. {@code @Qualifier} tuong minh xoa hoan toan phu thuoc vao ten
 * tham so.
 *
 * <p><b>Vi sao phai tu khai bao lai CA bean {@code JdbcTemplate} chinh ({@link #jdbcTemplate})</b>:
 * loi NGHIEM TRONG THU 2 phat hien qua {@code mvn verify} day du (chi loi khi chay ca IT, khong loi
 * o unit test rieng {@code AiReadOnlyPermissionIT}) - them {@code @Bean JdbcTemplate
 * aiReadOnlyJdbcTemplate} lam Boot's {@code JdbcTemplateAutoConfiguration}
 * ({@code @ConditionalOnMissingBean(JdbcTemplate.class)}) TU BO HOAN TOAN, giong het co che
 * {@code @ConditionalOnMissingBean(DataSource.class)} da xu ly o tren cho DataSource - nhung lan
 * nay anh huong 4 Service KHAC dang tiem {@code JdbcTemplate} KHONG @Qualifier ({@code
 * NumberSequenceService}, {@code ReconciliationService}, {@code TenantAdminService}, {@code
 * TenantUserAdminService}): ca 4 vo tinh bi Spring gan NHAM vao {@code aiReadOnlyJdbcTemplate}
 * (bean {@code JdbcTemplate} DUY NHAT con lai trong context) - {@code
 * NumberSequenceService.nextValue()} (sinh so don/hoa don/SKU, dung cho MOI don hang) do do nem
 * "permission denied for table tenant_sequences" ngay khi tao don hang that (phat hien qua 10 IT
 * test that/loi dong loat trong {@code mvn verify}, KHONG phai chi doan doc code). Khai bao lai
 * bean {@code jdbcTemplate} tuong minh + {@code @Primary} khoi phuc dung hanh vi cu cho 4 Service
 * tren, khong can sua gi o phia ho.
 */
@Configuration
public class AiReadOnlyDataSourceConfig {

  @Primary
  @Bean
  @ConfigurationProperties("spring.datasource")
  public DataSourceProperties primaryDataSourceProperties() {
    return new DataSourceProperties();
  }

  @Primary
  @Bean
  @ConfigurationProperties("spring.datasource.hikari")
  public HikariDataSource primaryDataSource(DataSourceProperties primaryDataSourceProperties) {
    return primaryDataSourceProperties
        .initializeDataSourceBuilder()
        .type(HikariDataSource.class)
        .build();
  }

  @Primary
  @Bean
  public JdbcTemplate jdbcTemplate(
      @Qualifier("primaryDataSource") HikariDataSource primaryDataSource) {
    return new JdbcTemplate(primaryDataSource);
  }

  @Bean
  @ConfigurationProperties("app.ai.readonly-datasource")
  public HikariDataSource aiReadOnlyDataSource() {
    return DataSourceBuilder.create().type(HikariDataSource.class).build();
  }

  @Bean
  public JdbcTemplate aiReadOnlyJdbcTemplate(
      @Qualifier("aiReadOnlyDataSource") HikariDataSource aiReadOnlyDataSource) {
    return new JdbcTemplate(aiReadOnlyDataSource);
  }
}
