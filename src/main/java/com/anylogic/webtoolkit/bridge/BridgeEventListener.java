package com.anylogic.webtoolkit.bridge;

/** Listener para eventos emitidos desde Java hacia JavaScript. */
@FunctionalInterface
public interface BridgeEventListener {
    void onEvent(String event, Object data);
}
