package com.anylogic.webtoolkit.core;

import java.io.File;

/**
 * Immutable configuration for the Web Toolkit runtime and browser windows.
 *
 * <p>Use {@link #builder()} to construct instances, or {@link #defaults()} for
 * a zero-configuration setup with auto-detected CEF binaries.
 *
 * <pre>{@code
 * WebConfig config = WebConfig.builder()
 *     .devMode(true)
 *     .internetAccess(false)
 *     .defaultSize(1024, 768)
 *     .build();
 * WebRuntime.getInstance().initialize(config);
 * }</pre>
 */
public class WebConfig {

    private File runtimeDir;
    private boolean devMode = false;
    private boolean internetAccess = true;
    private int defaultWidth  = 1200;
    private int defaultHeight = 800;

    private WebConfig() {}

    /** @return the explicit CEF runtime directory, or {@code null} if auto-detection is used */
    public File getRuntimeDir()      { return runtimeDir; }
    /** @return {@code true} if DevTools and verbose logging are enabled */
    public boolean isDevMode()       { return devMode; }
    /** @return {@code true} if outbound internet access is permitted */
    public boolean isInternetAccess(){ return internetAccess; }
    /** @return the default browser window width in pixels */
    public int getDefaultWidth()     { return defaultWidth; }
    /** @return the default browser window height in pixels */
    public int getDefaultHeight()    { return defaultHeight; }

    /** Returns a new {@link Builder}. */
    public static Builder builder() { return new Builder(); }

    /** Builder for {@link WebConfig}. */
    public static class Builder {
        private final WebConfig c = new WebConfig();

        /**
         * Sets the directory containing the CEF native binaries.
         * If omitted, the runtime is auto-detected next to the JAR or in
         * {@code ~/.webtoolkit/runtime}.
         *
         * @param dir the CEF install directory
         */
        public Builder runtimeDir(File dir)       { c.runtimeDir = dir;       return this; }

        /**
         * Enables or disables DevTools access and verbose logging.
         *
         * @param v {@code true} to enable
         */
        public Builder devMode(boolean v)          { c.devMode = v;            return this; }

        /**
         * Allows or blocks outbound internet access from the embedded browser.
         * When {@code false}, only {@code http://webtoolkit/} URLs are permitted.
         *
         * @param v {@code true} to allow internet access (default)
         */
        public Builder internetAccess(boolean v)   { c.internetAccess = v;     return this; }

        /**
         * Sets the default size for new browser windows.
         *
         * @param w width in pixels
         * @param h height in pixels
         */
        public Builder defaultSize(int w, int h)   { c.defaultWidth = w; c.defaultHeight = h; return this; }

        /** Builds and returns the {@link WebConfig}. */
        public WebConfig build() { return c; }
    }

    /**
     * Returns a default configuration with auto-detection of the CEF runtime directory,
     * internet access enabled, and a 1200×800 default window size.
     */
    public static WebConfig defaults() { return builder().build(); }
}
