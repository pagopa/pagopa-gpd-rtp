package it.gov.pagopa.gpd.rtp.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum AppError {
    INTERNAL_SERVER_ERROR(
            HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "Something was wrong"),
    BAD_REQUEST(HttpStatus.INTERNAL_SERVER_ERROR, "Bad Request", "%s"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Unauthorized", "Error during authentication"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "Forbidden", "This method is forbidden"),
    RESPONSE_NOT_READABLE(
            HttpStatus.BAD_GATEWAY, "Response Not Readable", "The response body is not readable"),
    RTP_MESSAGE_NOT_SENT(HttpStatus.INTERNAL_SERVER_ERROR, "RTP message not sent", "The RTP message has not been sent to eventhub"),
    PAYMENT_POSITION_STATUS_NOT_VALID_FOR_RTP(HttpStatus.INTERNAL_SERVER_ERROR, "Payment position status not valid for RTP", "Payment option filtered by paymentPositionStatus"),
    TAX_CODE_NOT_VALID_FOR_RTP(HttpStatus.INTERNAL_SERVER_ERROR, "Tax code not valid for RTP", "Payment option filtered by tax code"),
    EC_NOT_ENABLED_FOR_RTP(HttpStatus.INTERNAL_SERVER_ERROR, "Creditor Institution not enabled for RTP", "Payment option filtered because flag opt-in for Creditor Institution is not enabled"),
    TRANSFER_NOT_VALID_FOR_RTP(HttpStatus.INTERNAL_SERVER_ERROR, "Transfers' category not valid for RTP", "Payment option filtered by transfer category"),
    CDC_OPERATION_NOT_VALID_FOR_RTP(HttpStatus.INTERNAL_SERVER_ERROR, "CDC's operation not valid for RTP", "The CDC's type of operation is not valid for RTP"),
    DB_REPLICA_NOT_UPDATED(HttpStatus.INTERNAL_SERVER_ERROR, "DB replica not updated", "Postgres' DB replica is not in sync with primary DB"),
    UNKNOWN(null, null, null);

    public final HttpStatus httpStatus;
    public final String title;
    public final String details;

    AppError(HttpStatus httpStatus, String title, String details) {
        this.httpStatus = httpStatus;
        this.title = title;
        this.details = details;
    }
}
