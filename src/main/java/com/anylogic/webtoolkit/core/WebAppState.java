package com.anylogic.webtoolkit.core;

/** Ciclo de vida de una WebApp. */
public enum WebAppState {
    CREATED,
    INITIALIZING,
    LOADING,
    READY,
    ERROR,
    CLOSING,
    CLOSED
}
