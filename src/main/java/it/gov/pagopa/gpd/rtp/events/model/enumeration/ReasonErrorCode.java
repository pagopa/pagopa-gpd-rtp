package it.gov.pagopa.gpd.rtp.events.model.enumeration;

public enum ReasonErrorCode {

    ERROR_PDV_IO(800),
    ERROR_PDV_UNEXPECTED(801),
    ;

    private final int code;

    ReasonErrorCode(int code) {
        this.code = code;
    }

    public int getCode() {
        return this.code;
    }
}
