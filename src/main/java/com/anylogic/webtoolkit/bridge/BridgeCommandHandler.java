package com.anylogic.webtoolkit.bridge;

/** Implement to handle a command received from JavaScript. */
@FunctionalInterface
public interface BridgeCommandHandler {
    /**
     * @param args  Arguments sent from JS (basic Java types + Map/List)
     * @param callback  Respond with success(result) or failure(code, msg)
     */
    void handle(Object[] args, BridgeCallback callback);
}
