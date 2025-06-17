package it.gov.pagopa.gpd.rtp.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Constants {
  public static final String LOG_PREFIX = "[GPDxRTP]";
  public static final String HEADER_REQUEST_ID = "X-Request-Id";
  public static final String HEADER_SUBSCRIPTION_KEY = "Ocp-Apim-Subscription-Key";
  public static final String STREAM_KEY = "rtp-event-stream:action";
  public static final String GROUP_NAME = "notification-group";
  public static final String CONSUMER_NAME = "consumer-notifications-1";
}
