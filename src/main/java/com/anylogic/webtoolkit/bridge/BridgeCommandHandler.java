package com.anylogic.webtoolkit.bridge;

/** Implementar para manejar un comando recibido desde JavaScript. */
@FunctionalInterface
public interface BridgeCommandHandler {
    /**
     * @param args  Argumentos enviados desde JS (tipos Java basicos + Map/List)
     * @param callback  Responder con success(result) o failure(code, msg)
     */
    void handle(Object[] args, BridgeCallback callback);
}
