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
    JSON_NOT_PROCESSABLE(HttpStatus.INTERNAL_SERVER_ERROR, "JSON not processable", "Payment option message is not a processable JSON"),
    RTP_MESSAGE_NOT_SENT(HttpStatus.INTERNAL_SERVER_ERROR, "RTP message not sent", "The RTP message has not been sent to eventhub"),
    PAYMENT_POSITION_STATUS_NOT_VALID_FOR_RTP(HttpStatus.INTERNAL_SERVER_ERROR, "Payment position status not valid for RTP", "Payment option filtered by paymentPositionStatus"),
    TAX_CODE_NOT_VALID_FOR_RTP(HttpStatus.INTERNAL_SERVER_ERROR, "Tax code not valid for RTP", "Payment option filtered by tax code"),
    EC_NOT_ENABLED_FOR_RTP(HttpStatus.INTERNAL_SERVER_ERROR, "Creditor Institution not enabled for RTP", "Payment option filtered because flag opt-in for Creditor Institution is not enabled"),
    TRANSFERS_CATEGORIES_NOT_VALID_FOR_RTP(HttpStatus.INTERNAL_SERVER_ERROR, "Transfers' category not valid for RTP", "Payment option filtered by transfer category"),
    TRANSFERS_TOTAL_AMOUNT_NOT_MATCHING(HttpStatus.INTERNAL_SERVER_ERROR, "Transfers' total amount not matching", "The transfer's combined total amount is not matching with the payment option amount"),
    CDC_OPERATION_NOT_VALID_FOR_RTP(HttpStatus.INTERNAL_SERVER_ERROR, "CDC's operation not valid for RTP", "The CDC's type of operation is not valid for RTP"),
    DB_REPLICA_NOT_UPDATED(HttpStatus.INTERNAL_SERVER_ERROR, "DB replica not updated", "Postgres' DB replica is not in sync with primary DB"),
    PAYMENT_OPTION_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, "Payment option not found", "The payment option is not present on the DB"),
    ACKNOWLEDGMENT_NOT_PRESENT(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error", "Acknowledgment not found in message header, unable to process messages"),
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
