package com.anylogic.webtoolkit.core;

import java.io.File;

/** Toolkit configuration. Use WebConfig.builder() to construct. */
public class WebConfig {

    private File runtimeDir;
    private boolean devMode = false;
    private boolean internetAccess = true;
    private int defaultWidth  = 1200;
    private int defaultHeight = 800;

    private WebConfig() {}

    public File getRuntimeDir()      { return runtimeDir; }
    public boolean isDevMode()       { return devMode; }
    public boolean isInternetAccess(){ return internetAccess; }
    public int getDefaultWidth()     { return defaultWidth; }
    public int getDefaultHeight()    { return defaultHeight; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final WebConfig c = new WebConfig();

        /** Directorio donde estan los binarios CEF. Auto-detectado si se omite. */
        public Builder runtimeDir(File dir)       { c.runtimeDir = dir;       return this; }
        /** Activa DevTools y hot reload. */
        public Builder devMode(boolean v)          { c.devMode = v;            return this; }
        /** Permite o bloquea acceso a Internet. */
        public Builder internetAccess(boolean v)   { c.internetAccess = v;     return this; }
        public Builder defaultSize(int w, int h)   { c.defaultWidth = w; c.defaultHeight = h; return this; }

        public WebConfig build() { return c; }
    }

    /** Config por defecto con auto-deteccion de runtime. */
    public static WebConfig defaults() { return builder().build(); }
}
