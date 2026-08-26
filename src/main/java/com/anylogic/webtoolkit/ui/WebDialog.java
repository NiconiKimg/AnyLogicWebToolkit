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

    public WebDialog(String resourcePath) { this.resourcePath = resourcePath; }

    public WebDialog setTitle(String t)      { this.title = t;        return this; }
    public WebDialog setSize(int w, int h)   { width = w; height = h; return this; }
    public WebDialog setModal(boolean v)     { this.modal = v;        return this; }
    public WebDialog setResizable(boolean v) { this.resizable = v;    return this; }

    public WebBridge      getBridge()      { return bridge; }
    public WebPermissions getPermissions() { return permissions; }
    public StateSync      getStateSync()   { return stateSync; }
    public WebAppState    getState()       { return state; }
    public boolean        isOpen()         { return dialog != null && dialog.isVisible(); }

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

    public void reload() {
        if (browser != null) SwingUtilities.invokeLater(browser::reload);
    }

    public void show() {
        if (dialog != null) SwingUtilities.invokeLater(() -> dialog.setVisible(true));
    }

    public void hide() {
        if (dialog != null) SwingUtilities.invokeLater(() -> dialog.setVisible(false));
    }

    public void toggle() {
        if (dialog != null) SwingUtilities.invokeLater(() -> dialog.setVisible(!dialog.isVisible()));
    }

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

        String url = "http://webtoolkit/" + resourcePath;
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

    private void setupBridge() {
        bridge.registerCommand("__dialog_close__", (args, cb) -> { close(); cb.success(null); });
        
        // Send events to JS when state changes
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