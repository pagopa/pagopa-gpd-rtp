package it.gov.pagopa.gpd.rtp.exception;

public class FailAndIgnore extends AppException {

  public FailAndIgnore(AppError appError, Object... args) {
    super(appError, args);
  }
}
