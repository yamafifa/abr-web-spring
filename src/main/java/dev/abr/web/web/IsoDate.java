package dev.abr.web.web;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

/**
 * YYYY-MM-DD の日付か（null は通す）。「2026-02-30」のような無い日付も、「2026/01/01」のような別の書き方も通さない
 *
 * <p>Bean Validation には日付の形を見る決まりが無いので、自分で作った。
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = IsoDate.Validator.class)
public @interface IsoDate {

  String message() default "日付の形式が正しくありません（YYYY-MM-DD）";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};

  class Validator implements ConstraintValidator<IsoDate, String> {

    /** STRICT にすると、2 月 30 日などを繰り上げずにエラーにする（uuuu は「年」。yyyy だと紀元の指定が要る） */
    private static final DateTimeFormatter FORMAT =
        DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT);

    /** YYYY-MM-DD を日付にする（null は null） */
    public static LocalDate parse(String s) {
      return s == null ? null : LocalDate.parse(s, FORMAT);
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
      if (value == null) {
        return true;
      }
      try {
        parse(value);
        return true;
      } catch (DateTimeParseException e) {
        return false;
      }
    }
  }
}
