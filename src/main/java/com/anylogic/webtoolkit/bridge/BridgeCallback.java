package com.anylogic.webtoolkit.bridge;

/**
 * Callback used to resolve or reject a JavaScript command invocation.
 * Exactly one of {@link #success} or {@link #failure} must be called per command handling.
 */
public interface BridgeCallback {

    /**
     * Resolves the JS {@code Promise} with the given result.
     *
     * @param result the value to resolve with; may be {@code null}
     */
    void success(Object result);

    /**
     * Rejects the JS {@code Promise} with an error.
     *
     * @param errorCode a short machine-readable error identifier
     * @param message   a human-readable description of the error
     */
    void failure(String errorCode, String message);
}
