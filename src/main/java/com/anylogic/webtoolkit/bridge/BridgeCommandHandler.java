package com.anylogic.webtoolkit.bridge;

/**
 * Handles a named command received from JavaScript over the bridge.
 *
 * <p>Implement this interface to process a specific JS command registered via
 * {@link WebBridge#registerCommand}. The handler must call exactly one of
 * {@link BridgeCallback#success} or {@link BridgeCallback#failure} to resolve
 * the JavaScript {@code Promise} returned by {@code AnyLogic.call()}.
 */
@FunctionalInterface
public interface BridgeCommandHandler {

    /**
     * Handles the incoming command.
     *
     * @param args     positional arguments deserialized from JSON; strings map to
     *                 {@code String}, numbers to {@code Double}, booleans to
     *                 {@code Boolean}, objects to {@code LinkedHashMap}, arrays to
     *                 {@code ArrayList}
     * @param callback used to respond to the JavaScript caller
     */
    void handle(Object[] args, BridgeCallback callback);
}
