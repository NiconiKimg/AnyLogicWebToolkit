package com.anylogic.webtoolkit.core;

import com.anylogic.webtoolkit.logging.WebToolkitLogger;
import me.friwi.jcefmaven.CefAppBuilder;
import me.friwi.jcefmaven.MavenCefAppHandlerAdapter;
import org.cef.CefApp;
import org.cef.CefSettings;

import javax.swing.Timer;
import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Singleton that owns the JCEF {@link CefApp} instance for the entire toolkit.
 *
 * <p>{@link #initialize(WebConfig)} must be called once before creating any
 * {@link com.anylogic.webtoolkit.ui.WebDialog}. On first run, jcefmaven
 * downloads the Chromium native binaries (~180 MB) to
 * {@code ~/.webtoolkit/runtime/} if they are not already present.
 *
 * <p>A JVM shutdown hook is registered automatically to call {@link #shutdown}
 * when the process exits.
 */
public class WebRuntime {

    private static final WebToolkitLogger LOG = WebToolkitLogger.get("RUNTIME");
    private static final WebRuntime INSTANCE = new WebRuntime();

    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private WebConfig config;
    private CefApp cefApp;
    private Timer messageLoopTimer;

    private WebRuntime() {}

    /** Returns the singleton instance. */
    public static WebRuntime getInstance() { return INSTANCE; }

    /**
     * Initializes the CEF runtime with the given configuration.
     *
     * <p>Idempotent — subsequent calls are ignored if already initialized.
     * This method is {@code synchronized} and may block while jcefmaven
     * downloads and installs the native binaries on first run.
     *
     * @param config toolkit configuration
     * @throws WebToolkitException with code {@code RUNTIME_INIT_FAILED} if initialization fails
     */
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

            // Windowless rendering must be disabled to avoid native freezes
            // and conflicts with AnyLogic's JOGL OpenGL context.
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

    /**
     * Initializes the CEF runtime with {@link WebConfig#defaults()}.
     *
     * @throws WebToolkitException with code {@code RUNTIME_INIT_FAILED} if initialization fails
     */
    public void initialize() { initialize(WebConfig.defaults()); }

    /** @return {@code true} if the runtime has been successfully initialized */
    public boolean isInitialized() { return initialized.get(); }

    /** @return the active {@link WebConfig}, or {@code null} before initialization */
    public WebConfig getConfig()   { return config; }

    /**
     * Disposes the CEF runtime and releases all native resources.
     * Safe to call multiple times; subsequent calls are no-ops.
     */
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

    /**
     * Returns the underlying {@link CefApp}.
     *
     * @return the active CEF application
     * @throws WebToolkitException with code {@code NOT_INITIALIZED} if the runtime is not initialized
     */
    public CefApp getCefApp() {
        checkInitialized();
        return cefApp;
    }

    /**
     * Asserts that the runtime has been initialized.
     *
     * @throws WebToolkitException with code {@code NOT_INITIALIZED} if not initialized
     */
    public void checkInitialized() {
        if (!initialized.get())
            throw new WebToolkitException("NOT_INITIALIZED",
                "Call WebRuntime.getInstance().initialize() before using the toolkit");
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