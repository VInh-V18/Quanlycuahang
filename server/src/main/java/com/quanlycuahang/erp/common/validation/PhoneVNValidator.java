package com.quanlycuahang.erp.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

/**
 * Regex cho so di dong Viet Nam sau chuan hoa dau so 2018 (10 so, bat dau 03/05/07/08/09). Cho phep
 * nhap dang +84 hoac 0. Rong (null/blank) coi la hop le — dung @NotBlank rieng neu bat buoc nhap.
 */
public class PhoneVNValidator implements ConstraintValidator<ValidPhoneVN, String> {

  private static final Pattern PATTERN =
      Pattern.compile("^(\\+84|0)(3[2-9]|5[25689]|7[06-9]|8[1-9]|9[0-9])[0-9]{7}$");

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    if (value == null || value.isBlank()) {
      return true;
    }
    return PATTERN.matcher(value.trim()).matches();
  }
}
