package com.anylogic.webtoolkit.core;

/**
 * Base unchecked exception for toolkit errors, carrying a short machine-readable error code.
 */
public class WebToolkitException extends RuntimeException {

    private final String code;

    /**
     * @param code    short machine-readable identifier (e.g. {@code "RUNTIME_INIT_FAILED"})
     * @param message human-readable description
     */
    public WebToolkitException(String code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * @param code    short machine-readable identifier
     * @param message human-readable description
     * @param cause   the underlying cause
     */
    public WebToolkitException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    /** @return the short error code */
    public String getCode() { return code; }

    @Override
    public String toString() {
        return "[" + code + "] " + getMessage();
    }
}
