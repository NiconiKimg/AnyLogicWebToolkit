package com.anylogic.webtoolkit.bridge;

/**
 * Catch-all handler invoked when an incoming JS command has no specific
 * {@link BridgeCommandHandler} registered for it.
 *
 * <p>Set via {@link WebBridge#setDefaultCommandHandler}. If no default handler
 * is configured, unregistered commands are rejected with an
 * {@code UNKNOWN_COMMAND} error.
 */
public interface DefaultBridgeCommandHandler {

    /**
     * Handles an unregistered command.
     *
     * @param command  the command name received from JavaScript
     * @param args     deserialized arguments (see {@link BridgeCommandHandler} for type mapping)
     * @param callback used to respond to the JavaScript caller
     */
    void handle(String command, Object[] args, BridgeCallback callback);
}
