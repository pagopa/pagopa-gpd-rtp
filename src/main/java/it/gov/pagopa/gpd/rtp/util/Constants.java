package it.gov.pagopa.gpd.rtp.util;

import lombok.experimental.UtilityClass;

import java.util.UUID;

@UtilityClass
public class Constants {

  public static final String HEADER_REQUEST_ID = "X-Request-Id";
  public static final String HEADER_SUBSCRIPTION_KEY = "Ocp-Apim-Subscription-Key";
  public static final String STREAM_KEY = "rtp-event-stream:action";
  public static final String GROUP_NAME = "notification-group".concat(String.valueOf(UUID.randomUUID()));
  public static final String CONSUMER_NAME = "consumer-notifications-1";
  public static final String CUSTOM_EVENT = "RTP_ALERT";
}
