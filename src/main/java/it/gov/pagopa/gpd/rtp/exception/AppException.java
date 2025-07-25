package it.gov.pagopa.gpd.rtp.exception;

import jakarta.validation.constraints.NotNull;
import java.util.Formatter;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;

/**
 * Custom exception.
 *
 * <p>See {@link ErrorHandler}
 */
@Validated
@Data
public class AppException extends RuntimeException {

  /** title returned to the response when this exception occurred */
  String title;

  /** http status returned to the response when this exception occurred */
  HttpStatus httpStatus;

  AppError appErrorCode;

  /**
   * @param appError Response template returned to the response
   * @param args {@link Formatter} replaces the placeholders in "details" string of {@link AppError}
   *     with the arguments. If there are more arguments than format specifiers, the extra arguments
   *     are ignored.
   */
  public AppException(@NotNull AppError appError, Object... args) {
    super(formatDetails(appError, args));
    this.httpStatus = appError.httpStatus;
    this.title = appError.title;
    this.appErrorCode = appError;
  }

  /**
   * @param appError Response template returned to the response
   * @param cause The cause of this {@link AppException}
   * @param args Arguments for the details of {@link AppError} replaced by the {@link Formatter}. If
   *     there are more arguments than format specifiers, the extra arguments are ignored.
   */
  public AppException(@NotNull AppError appError, Throwable cause, Object... args) {
    super(formatDetails(appError, args), cause);
    this.httpStatus = appError.httpStatus;
    this.title = appError.title;
    this.appErrorCode = appError;
  }

  /**
   * @param httpStatus HTTP status returned to the response
   * @param title title returned to the response when this exception occurred
   * @param message the detail message returned to the response
   */
  public AppException(
      @NotNull HttpStatus httpStatus, @NotNull String title, @NotNull String message) {
    super(message);
    this.title = title;
    this.httpStatus = httpStatus;
    this.appErrorCode = null;
  }

  private static String formatDetails(AppError appError, Object[] args) {
    return String.format(appError.details, args);
  }

  @Override
  public String toString() {
    return "AppException(" + httpStatus + ", " + title + ")" + super.toString();
  }
}
