package com.anylogic.webtoolkit.bridge;

/**
 * Listener called when a command is received from JavaScript via the bridge.
 *
 * <p>Unlike {@link BridgeCommandHandler}, this listener is passive — it cannot
 * respond to the JS caller. It is invoked after the registered handler has
 * already handled the command, making it suitable for cross-cutting concerns
 * such as logging or audit trails.
 *
 * @see WebBridge#on(String, BridgeEventListener)
 * @see WebBridge#onAnyEvent(BridgeEventListener)
 */
@FunctionalInterface
public interface BridgeEventListener {

    /**
     * Called after a JS command has been dispatched to its handler.
     *
     * @param event the command name
     * @param data  the full {@code Object[]} args array deserialized from the JS call
     */
    void onEvent(String event, Object data);
}
