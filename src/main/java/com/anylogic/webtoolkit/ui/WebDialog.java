package com.anylogic.webtoolkit.ui;

import java.util.Map;
import com.anylogic.webtoolkit.bridge.WebBridge;
import com.anylogic.webtoolkit.browser.WebSchemeHandler;
import com.anylogic.webtoolkit.core.StateSync;
import com.anylogic.webtoolkit.core.WebAppState;
import com.anylogic.webtoolkit.core.WebRuntime;
import com.anylogic.webtoolkit.core.WebToolkitException;
import com.anylogic.webtoolkit.logging.WebToolkitLogger;
import com.anylogic.webtoolkit.security.Permission;
import com.anylogic.webtoolkit.security.WebPermissions;
import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.browser.CefMessageRouter;
import org.cef.handler.CefDisplayHandlerAdapter;
import org.cef.handler.CefLoadHandlerAdapter;
import org.cef.handler.CefLoadHandler;
import org.cef.handler.CefRequestHandlerAdapter;
import org.cef.handler.CefResourceRequestHandler;
import org.cef.handler.CefResourceRequestHandlerAdapter;
import org.cef.handler.CefResourceHandler;
import org.cef.handler.CefKeyboardHandlerAdapter;
import org.cef.handler.CefKeyboardHandler.CefKeyEvent;
import org.cef.network.CefRequest;
import org.cef.misc.BoolRef;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.UUID;

/**
 * A native window hosting an embedded Chromium browser and the toolkit's
 * {@link WebBridge} for bidirectional Java ↔ JavaScript communication.
 *
 * <p>{@link WebRuntime} must be initialized before calling {@link #open}.
 * All UI operations are dispatched to the Swing EDT internally.
 *
 * <p>Built-in keyboard shortcuts:
 * <ul>
 *   <li><b>F5</b> — reload the page (cache bypass)</li>
 *   <li><b>F12</b> — open Chrome DevTools</li>
 * </ul>
 *
 * <p>Built-in bridge commands registered automatically on open:
 * {@code __dialog_close__}, {@code __get__}, {@code __set__},
 * {@code __fs_open__}, {@code __fs_save__}, {@code __fs_read__}, {@code __fs_write__}.
 */
public class WebDialog {

    private static final WebToolkitLogger LOG = WebToolkitLogger.get("UI");

    private final String appId        = UUID.randomUUID().toString().substring(0, 8);
    private final String resourcePath;
    private final WebBridge bridge    = new WebBridge();
    private final WebPermissions permissions = new WebPermissions();
    private final StateSync stateSync = new StateSync();

    private String  title     = "AnyLogic Web Toolkit";
    private int     width     = 1200;
    private int     height    = 800;
    private boolean modal     = false;
    private boolean resizable = true;

    private JFrame     dialog;
    private CefClient  cefClient;
    private CefBrowser browser;
    private WebAppState state = WebAppState.CREATED;

    /**
     * @param resourcePath path to the HTML entry point, relative to the AnyLogic project
     *                     directory (i.e. {@code System.getProperty("user.dir")})
     */
    public WebDialog(String resourcePath) { this.resourcePath = resourcePath; }

    // --- Builder-style configuration (must be called before open()) ---

    /**
     * Sets the window title.
     *
     * @param t the title string
     * @return {@code this} for chaining
     */
    public WebDialog setTitle(String t)      { this.title = t;        return this; }

    /**
     * Sets the initial window size. Defaults to 1200×800.
     *
     * @param w width in pixels
     * @param h height in pixels
     * @return {@code this} for chaining
     */
    public WebDialog setSize(int w, int h)   { width = w; height = h; return this; }

    /**
     * Sets whether the window is modal. Currently informational; not enforced by the JFrame.
     *
     * @param v {@code true} for modal
     * @return {@code this} for chaining
     */
    public WebDialog setModal(boolean v)     { this.modal = v;        return this; }

    /**
     * Sets whether the user can resize the window. Defaults to {@code true}.
     *
     * @param v {@code true} to allow resizing
     * @return {@code this} for chaining
     */
    public WebDialog setResizable(boolean v) { this.resizable = v;    return this; }

    // --- Accessors ---

    /** @return the {@link WebBridge} for emitting events and registering commands */
    public WebBridge      getBridge()      { return bridge; }

    /** @return the permission set for this dialog */
    public WebPermissions getPermissions() { return permissions; }

    /** @return the shared state store for Java ↔ JS key-value synchronization */
    public StateSync      getStateSync()   { return stateSync; }

    /** @return the current lifecycle state */
    public WebAppState    getState()       { return state; }

    /** @return {@code true} if the window exists and is currently visible */
    public boolean        isOpen()         { return dialog != null && dialog.isVisible(); }

    // --- Lifecycle ---

    /**
     * Creates and displays the browser window.
     *
     * <p>The call returns immediately; actual construction happens on the Swing EDT.
     * The state transitions: {@code CREATED → INITIALIZING → LOADING → READY}.
     *
     * @throws WebToolkitException with code {@code NOT_INITIALIZED} if {@link WebRuntime}
     *                             has not been initialized
     * @throws WebToolkitException with code {@code DIALOG_OPEN_FAILED} if construction fails
     */
    public void open() {
        WebRuntime.getInstance().checkInitialized();
        SwingUtilities.invokeLater(() -> {
            state = WebAppState.INITIALIZING;
            LOG.info("Opening: " + resourcePath);
            try { buildAndShow(); }
            catch (Exception e) {
                state = WebAppState.ERROR;
                throw new WebToolkitException("DIALOG_OPEN_FAILED", e.getMessage(), e);
            }
        });
    }

    /**
     * Closes the browser window and disposes all CEF resources.
     *
     * <p>Safe to call if the window is not open. The call is asynchronous;
     * state transitions to {@code CLOSING} then {@code CLOSED} on the EDT.
     */
    public void close() {
        if (dialog == null) return;
        SwingUtilities.invokeLater(() -> {
            state = WebAppState.CLOSING;
            if (browser != null) browser.close(true);
            dialog.dispose();
            dialog = null;
            state  = WebAppState.CLOSED;
            LOG.info("Closed: " + resourcePath);
        });
    }

    /**
     * Reloads the current page, bypassing the browser cache.
     * No-op if the browser is not yet open.
     */
    public void reload() {
        if (browser != null) SwingUtilities.invokeLater(browser::reload);
    }

    /**
     * Makes the window visible. No-op if the window does not exist.
     */
    public void show() {
        if (dialog != null) SwingUtilities.invokeLater(() -> dialog.setVisible(true));
    }

    /**
     * Hides the window without closing it. No-op if the window does not exist.
     */
    public void hide() {
        if (dialog != null) SwingUtilities.invokeLater(() -> dialog.setVisible(false));
    }

    /**
     * Toggles the window's visibility. No-op if the window does not exist.
     */
    public void toggle() {
        if (dialog != null) SwingUtilities.invokeLater(() -> dialog.setVisible(!dialog.isVisible()));
    }

    // --- Internal ---

    private void buildAndShow() {
        final Path projectRoot = Paths.get(System.getProperty("user.dir"));

        cefClient = CefApp.getInstance().createClient();

        CefMessageRouter msgRouter = CefMessageRouter.create(bridge.createRouterHandler());
        cefClient.addMessageRouter(msgRouter);

        cefClient.addLoadHandler(new CefLoadHandlerAdapter() {
            @Override
            public void onLoadEnd(CefBrowser b, CefFrame f, int statusCode) {
                if (f.isMain()) {
                    state = WebAppState.READY;
                    LOG.info("Ready: " + resourcePath);
                }
            }
            @Override
            public void onLoadError(CefBrowser b, CefFrame f,
                                    CefLoadHandler.ErrorCode code, String desc, String url) {
                state = WebAppState.ERROR;
                LOG.error("Error loading " + url + ": " + desc);
            }
        });

        // Propagate document.title changes to the JFrame title bar
        cefClient.addDisplayHandler(new CefDisplayHandlerAdapter() {
            @Override
            public void onTitleChange(CefBrowser b, String newTitle) {
                if (dialog != null) SwingUtilities.invokeLater(() -> dialog.setTitle(newTitle));
            }
        });

        cefClient.addKeyboardHandler(new CefKeyboardHandlerAdapter() {
            @Override
            public boolean onKeyEvent(CefBrowser b, CefKeyEvent event) {
                if (event.type == CefKeyEvent.EventType.KEYEVENT_RAWKEYDOWN) {
                    if (event.windows_key_code == 116) { // F5
                        b.reloadIgnoreCache();
                        return true;
                    } else if (event.windows_key_code == 123) { // F12
                        SwingUtilities.invokeLater(b::openDevTools);
                        return true;
                    }
                }
                return false;
            }
        });

        cefClient.addRequestHandler(new CefRequestHandlerAdapter() {
            @Override
            public CefResourceRequestHandler getResourceRequestHandler(CefBrowser b, CefFrame f, CefRequest req, boolean isNav, boolean isDownload, String reqInit, BoolRef disNtlm) {
                if (req.getURL().startsWith("http://webtoolkit/")) {
                    return new CefResourceRequestHandlerAdapter() {
                        @Override
                        public CefResourceHandler getResourceHandler(CefBrowser bb, CefFrame ff, CefRequest r) {
                            return new WebSchemeHandler(projectRoot);
                        }
                    };
                }
                return null;
            }

            @Override
            public boolean onBeforeBrowse(CefBrowser b, CefFrame f,
                                          CefRequest req, boolean isUserGesture, boolean isRedirect) {
                String url = req.getURL();
                if (url.startsWith("http://webtoolkit/")) return false;
                if (permissions.isAllowed(Permission.INTERNET)
                        && (url.startsWith("https://") || url.startsWith("http://"))) return false;
                LOG.warn("Blocked: " + url);
                return true;
            }
        });

        String safePath = resourcePath;
        try {
            safePath = new java.io.File(resourcePath).getCanonicalPath().replace('\\', '/');
        } catch (Exception e) {
            LOG.warn("Could not resolve canonical path: " + e.getMessage());
        }

        String url = "http://webtoolkit/" + safePath;
        browser = cefClient.createBrowser(url, false, false);
        bridge.setBrowser(browser);

        dialog = new JFrame(title);
        dialog.setSize(width, height);
        dialog.setResizable(resizable);
        dialog.setLocationRelativeTo(null);
        dialog.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        dialog.addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { close(); }
        });
        dialog.getContentPane().setLayout(new BorderLayout());
        dialog.getContentPane().add(browser.getUIComponent(), BorderLayout.CENTER);
        dialog.setVisible(true);
        browser.getUIComponent().requestFocus();
        state = WebAppState.LOADING;

        setupBridge();
    }

    /** Registers all built-in bridge commands. */
    private void setupBridge() {
        bridge.registerCommand("__dialog_close__", (args, cb) -> { close(); cb.success(null); });

        // Forward StateSync changes to JS as __state_change__ events
        stateSync.onAnyChange((key, val) -> {
            bridge.emit("__state_change__", Map.of("key", key, "value", val));
        });

        bridge.registerCommand("__get__", (args, cb) -> {
            if (args.length < 1) { cb.failure("INVALID_ARGS", "Missing key"); return; }
            cb.success(stateSync.get(String.valueOf(args[0])));
        });

        bridge.registerCommand("__set__", (args, cb) -> {
            if (args.length < 2) { cb.failure("INVALID_ARGS", "Missing key/value"); return; }
            stateSync.set(String.valueOf(args[0]), args[1]);
            cb.success(null);
        });

        bridge.registerCommand("__fs_open__", (args, cb) -> {
            String title = args.length > 0 && args[0] != null ? String.valueOf(args[0]) : null;
            String defaultName = args.length > 1 && args[1] != null ? String.valueOf(args[1]) : null;
            com.anylogic.webtoolkit.core.WebFileSystem.openDialog(title, defaultName).thenAccept(path -> {
                cb.success(path);
            }).exceptionally(ex -> {
                cb.failure("FS_ERROR", ex.getMessage());
                return null;
            });
        });

        bridge.registerCommand("__fs_save__", (args, cb) -> {
            String title = args.length > 0 && args[0] != null ? String.valueOf(args[0]) : null;
            String defaultName = args.length > 1 && args[1] != null ? String.valueOf(args[1]) : null;
            com.anylogic.webtoolkit.core.WebFileSystem.saveDialog(title, defaultName).thenAccept(path -> {
                cb.success(path);
            }).exceptionally(ex -> {
                cb.failure("FS_ERROR", ex.getMessage());
                return null;
            });
        });

        bridge.registerCommand("__fs_read__", (args, cb) -> {
            if (args.length < 1) { cb.failure("INVALID_ARGS", "Missing path"); return; }
            String path = String.valueOf(args[0]);
            boolean isBase64 = args.length > 1 && Boolean.TRUE.equals(args[1]);
            try {
                if (isBase64) cb.success(com.anylogic.webtoolkit.core.WebFileSystem.readBase64(path));
                else cb.success(com.anylogic.webtoolkit.core.WebFileSystem.readText(path));
            } catch (Exception e) {
                cb.failure("FS_ERROR", e.getMessage());
            }
        });

        bridge.registerCommand("__fs_write__", (args, cb) -> {
            if (args.length < 2) { cb.failure("INVALID_ARGS", "Missing path or data"); return; }
            String path = String.valueOf(args[0]);
            String data = String.valueOf(args[1]);
            boolean isBase64 = args.length > 2 && Boolean.TRUE.equals(args[2]);
            try {
                if (isBase64) com.anylogic.webtoolkit.core.WebFileSystem.writeBase64(path, data);
                else com.anylogic.webtoolkit.core.WebFileSystem.writeText(path, data);
                cb.success(true);
            } catch (Exception e) {
                cb.failure("FS_ERROR", e.getMessage());
            }
        });
    }
}