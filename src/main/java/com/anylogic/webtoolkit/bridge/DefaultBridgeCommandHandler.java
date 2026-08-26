package com.anylogic.webtoolkit.bridge;

public interface DefaultBridgeCommandHandler {
    void handle(String command, Object[] args, BridgeCallback callback);
}
