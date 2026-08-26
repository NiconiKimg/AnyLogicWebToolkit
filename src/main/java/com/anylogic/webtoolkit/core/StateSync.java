package com.anylogic.webtoolkit.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Thread-safe key-value state store for synchronizing data between AnyLogic and JavaScript.
 *
 * <p>Values written from Java via {@link #set} are automatically pushed to the browser
 * as {@code __state_change__} events, triggering any {@code AnyLogic.state.subscribe}
 * callbacks registered in JavaScript. Values written from JavaScript via
 * {@code AnyLogic.state.set} are stored here and readable via {@link #get}.
 */
public class StateSync {

    private final Map<String, Object> state = new ConcurrentHashMap<>();
    private final Map<String, Consumer<Object>> changeListeners = new ConcurrentHashMap<>();
    private BiConsumer<String, Object> anyChangeListener;

    /**
     * Registers a listener invoked whenever any key changes, from either Java or JavaScript.
     * Replaces any previously registered listener.
     *
     * @param listener receives the changed key and its new value ({@code null} if removed)
     */
    public void onAnyChange(BiConsumer<String, Object> listener) {
        this.anyChangeListener = listener;
    }

    /**
     * Stores a value and notifies any registered listeners.
     * If {@code value} is {@code null}, the key is removed from the store.
     *
     * @param key   the state key
     * @param value the new value, or {@code null} to remove the key
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
     * Returns the value for the given key, or {@code null} if absent.
     *
     * @param key the state key
     * @return the stored value, or {@code null}
     */
    public Object get(String key) {
        return state.get(key);
    }

    /**
     * Returns the value for the given key cast to {@code T}, or {@code null} if the
     * key is absent or the value is not an instance of {@code clazz}.
     *
     * @param <T>   the expected type
     * @param key   the state key
     * @param clazz the expected class
     * @return the cast value, or {@code null}
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
     * Registers a listener invoked when the given key's value changes.
     * Replaces any existing listener for that key.
     *
     * @param key      the state key to watch
     * @param listener receives the new value ({@code null} if the key was removed)
     */
    public void onChange(String key, Consumer<Object> listener) {
        changeListeners.put(key, listener);
    }

    /**
     * Removes the change listener for the given key.
     *
     * @param key the state key whose listener should be removed
     */
    public void removeListener(String key) {
        changeListeners.remove(key);
    }
}
