package com.anylogic.webtoolkit.bridge;

/** Callback para responder a un comando JS. */
public interface BridgeCallback {
    void success(Object result);
    void failure(String errorCode, String message);
}
