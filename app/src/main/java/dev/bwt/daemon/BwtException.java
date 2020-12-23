package dev.bwt.daemon;

public class BwtException extends RuntimeException {
    private int code;

    public BwtException(String message) {
        super(message);
    }

    public BwtException(String message, int code) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return this.code;
    }
}