package com.anylogic.webtoolkit.core;

import com.anylogic.webtoolkit.logging.WebToolkitLogger;
import me.friwi.jcefmaven.CefAppBuilder;
import me.friwi.jcefmaven.MavenCefAppHandlerAdapter;
import org.cef.CefApp;
import org.cef.CefSettings;

import javax.swing.Timer;
import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

public class WebRuntime {

    private static final WebToolkitLogger LOG = WebToolkitLogger.get("RUNTIME");
    private static final WebRuntime INSTANCE = new WebRuntime();

    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private WebConfig config;
    private CefApp cefApp;
    private Timer messageLoopTimer;

    private WebRuntime() {}

    public static WebRuntime getInstance() { return INSTANCE; }

    public synchronized void initialize(WebConfig config) {
        if (initialized.get()) { LOG.warn("Already initialized"); return; }
        this.config = config;
        LOG.info("Starting WebRuntime...");
        try {
            File runtimeDir = resolveRuntimeDir(config);
            LOG.info("Runtime dir: " + runtimeDir.getAbsolutePath());

            CefAppBuilder builder = new CefAppBuilder();
            builder.setInstallDir(runtimeDir);
            builder.setAppHandler(new MavenCefAppHandlerAdapter() {});
            
            // CRITICAL configuration to avoid native freeze and AnyLogic JOGL conflicts
            CefSettings settings = builder.getCefSettings();
            settings.windowless_rendering_enabled = false;

            cefApp = builder.build();
            initialized.set(true);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                LOG.info("Shutdown - closing Chromium...");
                shutdown();
            }, "webtk-shutdown"));

            LOG.info("WebRuntime ready");
        } catch (Exception e) {
            throw new WebToolkitException("RUNTIME_INIT_FAILED",
                "Could not initialize web engine: " + e.getMessage(), e);
        }
    }

    public void initialize() { initialize(WebConfig.defaults()); }

    public boolean isInitialized() { return initialized.get(); }
    public WebConfig getConfig()   { return config; }

    public synchronized void shutdown() {
        if (!initialized.get()) return;
        LOG.info("Shutting down WebRuntime...");
        if (messageLoopTimer != null) {
            messageLoopTimer.stop();
            messageLoopTimer = null;
        }
        if (cefApp != null) { 
            cefApp.dispose(); 
            cefApp = null; 
        }
        initialized.set(false);
    }

    public CefApp getCefApp() {
        checkInitialized();
        return cefApp;
    }

    public void checkInitialized() {
        if (!initialized.get())
            throw new WebToolkitException("NOT_INITIALIZED",
                "Llamar WebRuntime.getInstance().initialize() antes de usar el toolkit");
    }

    private File resolveRuntimeDir(WebConfig config) {
        if (config.getRuntimeDir() != null) return config.getRuntimeDir();
        File jarDir = getJarDirectory();
        if (jarDir != null) {
            File candidate = new File(jarDir, "runtime/windows-amd64");
            if (candidate.exists()) return candidate;
        }
        return new File(System.getProperty("user.home"), ".webtoolkit/runtime");
    }

    private File getJarDirectory() {
        try {
            var loc = WebRuntime.class.getProtectionDomain().getCodeSource().getLocation();
            if (loc != null) return new File(loc.toURI()).getParentFile();
        } catch (Exception ignored) {}
        return null;
    }
}