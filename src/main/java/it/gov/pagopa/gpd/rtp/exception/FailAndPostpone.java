package it.gov.pagopa.gpd.rtp.exception;

public class FailAndPostpone extends AppException {

  public FailAndPostpone(AppError appError, Object... args) {
    super(appError, args);
  }
}
