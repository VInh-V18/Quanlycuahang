package com.quanlycuahang.erp.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** So dien thoai Viet Nam hop le: dau so 0 hoac +84, theo sau la dau hieu nha mang + 8 chu so. */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PhoneVNValidator.class)
public @interface ValidPhoneVN {

  String message() default "So dien thoai khong dung dinh dang Viet Nam";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
