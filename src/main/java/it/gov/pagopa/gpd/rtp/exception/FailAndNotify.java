package it.gov.pagopa.gpd.rtp.exception;

public class FailAndNotify extends AppException {

  public FailAndNotify(AppError appError, Object... args) {
    super(appError, args);
  }
}
