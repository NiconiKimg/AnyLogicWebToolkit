package com.anylogic.webtoolkit.bridge;

/** Listener for events emitted from Java to JavaScript. */
@FunctionalInterface
public interface BridgeEventListener {
    void onEvent(String event, Object data);
}
