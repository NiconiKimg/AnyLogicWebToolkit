package com.anylogic.webtoolkit.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Almacén de estado thread-safe para sincronizar datos entre AnyLogic y JS.
 */
public class StateSync {

    private final Map<String, Object> state = new ConcurrentHashMap<>();
    private final Map<String, Consumer<Object>> changeListeners = new ConcurrentHashMap<>();
    private java.util.function.BiConsumer<String, Object> anyChangeListener;

    public void onAnyChange(java.util.function.BiConsumer<String, Object> listener) {
        this.anyChangeListener = listener;
    }

    /**
     * Establece un valor en el estado.
     */
    public void set(String key, Object value) {
        if (value == null) {
            state.remove(key);
        } else {
            state.put(key, value);
        }
        
        Consumer<Object> listener = changeListeners.get(key);
        if (listener != null) {
            listener.accept(value);
        }
        
        if (anyChangeListener != null) {
            anyChangeListener.accept(key, value);
        }
    }

    /**
     * Obtiene un valor del estado.
     */
    public Object get(String key) {
        return state.get(key);
    }

    /**
     * Obtiene un valor con tipo fuerte.
     */
    @SuppressWarnings("unchecked")
    public <T> T getAs(String key, Class<T> clazz) {
        Object val = state.get(key);
        if (clazz.isInstance(val)) {
            return (T) val;
        }
        return null;
    }

    /**
     * Registra un listener que se ejecuta cuando el valor de una clave cambia en JS o Java.
     */
    public void onChange(String key, Consumer<Object> listener) {
        changeListeners.put(key, listener);
    }

    /**
     * Elimina un listener de cambio.
     */
    public void removeListener(String key) {
        changeListeners.remove(key);
    }
}
