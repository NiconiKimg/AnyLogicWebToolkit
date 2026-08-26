package com.anylogic.webtoolkit.core;

/**
 * Lifecycle states of a {@link com.anylogic.webtoolkit.ui.WebDialog} instance.
 *
 * <p>State transitions follow the sequence:
 * {@code CREATED → INITIALIZING → LOADING → READY},
 * with {@code ERROR} reachable from any state on failure, and
 * {@code CLOSING → CLOSED} on shutdown.
 */
public enum WebAppState {
    /** Instance created but {@link com.anylogic.webtoolkit.ui.WebDialog#open()} not yet called. */
    CREATED,
    /** CEF client and browser are being constructed. */
    INITIALIZING,
    /** Browser is navigating to the initial URL. */
    LOADING,
    /** Main frame has finished loading; the bridge is fully operational. */
    READY,
    /** An error occurred during initialization or page load. */
    ERROR,
    /** {@link com.anylogic.webtoolkit.ui.WebDialog#close()} has been called; shutdown in progress. */
    CLOSING,
    /** The window and browser have been fully disposed. */
    CLOSED
}
