package com.anylogic.webtoolkit.bridge;

/** Callback to respond to a JS command. */
public interface BridgeCallback {
    void success(Object result);
    void failure(String errorCode, String message);
}
