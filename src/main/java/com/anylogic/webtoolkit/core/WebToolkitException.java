package com.anylogic.webtoolkit.core;

/** Excepcion base del toolkit. */
public class WebToolkitException extends RuntimeException {
    private final String code;

    public WebToolkitException(String code, String message) {
        super(message);
        this.code = code;
    }

    public WebToolkitException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() { return code; }

    @Override
    public String toString() {
        return "[" + code + "] " + getMessage();
    }
}
