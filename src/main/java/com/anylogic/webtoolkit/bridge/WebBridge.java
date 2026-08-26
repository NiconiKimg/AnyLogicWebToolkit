package com.anylogic.webtoolkit.bridge;

import com.anylogic.webtoolkit.logging.WebToolkitLogger;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefMessageRouterHandlerAdapter;

import javax.swing.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Bidirectional communication bridge between Java and the embedded browser.
 *
 * <p>On the Java side, commands arriving from JavaScript are dispatched to
 * registered {@link BridgeCommandHandler} instances. Events can be pushed to
 * JavaScript at any time via {@link #emit}.
 *
 * <p>On the JavaScript side, the global {@code AnyLogic} object injected by
 * {@link #getApiScript()} exposes {@code AnyLogic.call()}, {@code AnyLogic.events},
 * {@code AnyLogic.state}, {@code AnyLogic.files}, and {@code AnyLogic.dialog}.
 *
 * <p>All methods that interact with the browser dispatch to the Swing EDT internally;
 * callers do not need to be on the EDT.
 */
public class WebBridge {

    private static final WebToolkitLogger LOG = WebToolkitLogger.get("BRIDGE");
    private final Gson gson = new Gson();
    private final Map<String, BridgeCommandHandler> commands = new ConcurrentHashMap<>();
    private final Map<String, CopyOnWriteArrayList<BridgeEventListener>> listeners = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<BridgeEventListener> anyEventListeners = new CopyOnWriteArrayList<>();

    private DefaultBridgeCommandHandler defaultCommandHandler;
    private CefBrowser browser;

    /**
     * Returns the JavaScript source that bootstraps the {@code AnyLogic} API object.
     *
     * <p>This script is served at {@code http://webtoolkit/__webtk_api.js} and must be
     * included in every HTML page that uses the bridge.
     *
     * @return the complete JS source as a string
     */
    public static String getApiScript() {
        return """
            (function() {
              if (window.__WEBTK__) return;
              var _ev = {};
              var _stateCb = {};
              window.__WEBTK__ = {
                dispatch: function(e,d) { 
                    if(e === '__state_change__') {
                        var k = d.key; var v = d.value;
                        (_stateCb[k]||[]).forEach(function(h){try{h(v)}catch(x){console.error(x)}});
                        return;
                    }
                    (_ev[e]||[]).forEach(function(h){try{h(d)}catch(x){console.error(x)}}); 
                }
              };
              window.AnyLogic = {
                call: function(cmd) {
                  var args = Array.prototype.slice.call(arguments, 1);
                  return new Promise(function(res,rej) {
                    var uuid = Date.now().toString(36) + Math.random().toString(36).substring(2);
                    window.cefQuery({
                      request: JSON.stringify({id:uuid,command:cmd,args:args}),
                      onSuccess:function(r){var o=JSON.parse(r);o.success?res(o.result):rej(o.error);},
                      onFailure:function(c,m){rej({code:'BRIDGE_ERROR',message:m});}
                    });
                  });
                },
                state: {
                  get: function(k) { return AnyLogic.call('__get__',k); },
                  set: function(k,v){ return AnyLogic.call('__set__',k,v); },
                  subscribe: function(k,cb) { if(!_stateCb[k])_stateCb[k]=[]; _stateCb[k].push(cb); }
                },
                events: {
                  on:   function(e,h){ if(!_ev[e])_ev[e]=[]; _ev[e].push(h); },
                  off:  function(e,h){ if(_ev[e])_ev[e]=_ev[e].filter(function(x){return x!==h;}); },
                  once: function(e,h){ var w=function(d){AnyLogic.events.off(e,w);h(d);}; AnyLogic.events.on(e,w); }
                },
                dialog: {
                  close:    function(){ AnyLogic.call('__dialog_close__'); },
                  setTitle: function(t){ document.title=t; }
                },
                files: {
                  openDialog: function(title, defaultName) { return AnyLogic.call('__fs_open__', title, defaultName); },
                  saveDialog: function(title, defaultName) { return AnyLogic.call('__fs_save__', title, defaultName); },
                  read:       function(path, isBase64)     { return AnyLogic.call('__fs_read__', path, !!isBase64); },
                  write:      function(path, data, isBase64){ return AnyLogic.call('__fs_write__', path, data, !!isBase64); }
                },
                runtime: { version:'0.2.0', platform:'windows' }
              };
              console.log('[WEBTK] API v0.2.0 loaded');
            })();
            """;
    }

    /**
     * Sets the browser instance that receives emitted events.
     * Must be called before any {@link #emit} or {@link #execute} calls.
     *
     * @param browser the active CEF browser
     */
    public void setBrowser(CefBrowser browser) { this.browser = browser; }

    /**
     * Pushes an event to JavaScript, invoking all {@code AnyLogic.events.on(event, ...)}
     * handlers registered in the page.
     *
     * <p>Safe to call from any thread. If no browser has been set, the call is a no-op.
     *
     * @param event the event name
     * @param data  the payload; serialized to JSON via Gson
     */
    public void emit(String event, Object data) {
        if (browser == null) { LOG.warn("emit() without browser"); return; }
        String json = gson.toJson(data);
        String js = String.format("window.__WEBTK__ && window.__WEBTK__.dispatch('%s',%s);", event.replace("'", "\\'"), json);
        SwingUtilities.invokeLater(() -> browser.executeJavaScript(js, "webtoolkit://bridge", 0));
    }

    /**
     * Executes arbitrary JavaScript in the browser.
     *
     * <p>Safe to call from any thread. If no browser has been set, the call is a no-op.
     *
     * @param jsCode the JavaScript source to execute
     */
    public void execute(String jsCode) {
        if (browser == null) return;
        SwingUtilities.invokeLater(() -> browser.executeJavaScript(jsCode, "webtoolkit://bridge", 0));
    }

    /**
     * Registers a handler for the given command name.
     * Replaces any previously registered handler for the same name.
     *
     * @param name    the command name as used in {@code AnyLogic.call(name, ...)}
     * @param handler the handler to invoke when the command is received
     */
    public void registerCommand(String name, BridgeCommandHandler handler) {
        commands.put(name, handler);
        LOG.debug("Command registered: " + name);
    }

    /**
     * Removes the handler registered for the given command name.
     *
     * @param name the command name to deregister
     */
    public void removeCommand(String name) { commands.remove(name); }

    /**
     * Subscribes a listener to a specific command. The listener is called after
     * the command's handler has been invoked, and cannot affect the response.
     *
     * @param event    the command name to observe
     * @param listener the listener to add
     */
    public void on(String event, BridgeEventListener listener) {
        listeners.computeIfAbsent(event, k -> new CopyOnWriteArrayList<>()).add(listener);
    }

    /**
     * Removes a previously added listener for the given command.
     *
     * @param event    the command name
     * @param listener the listener to remove
     */
    public void off(String event, BridgeEventListener listener) {
        var list = listeners.get(event);
        if (list != null) list.remove(listener);
    }

    /**
     * Subscribes a listener that is called for every command received from JavaScript,
     * regardless of name.
     *
     * @param listener the listener to add
     */
    public void onAnyEvent(BridgeEventListener listener) {
        anyEventListeners.add(listener);
    }

    /**
     * Creates the CEF message router handler that routes incoming {@code cefQuery}
     * requests to {@link #handleIncoming}.
     *
     * @return a {@link CefMessageRouterHandlerAdapter} ready to be added to a {@link org.cef.browser.CefMessageRouter}
     */
    public CefMessageRouterHandlerAdapter createRouterHandler() {
        return new CefMessageRouterHandlerAdapter() {
            @Override
            public boolean onQuery(CefBrowser b, CefFrame f, long queryId,
                                   String request, boolean persistent, CefQueryCallback callback) {
                handleIncoming(request, callback);
                return true;
            }
        };
    }

    /**
     * Sets the handler invoked when a command has no specific registration.
     * If {@code null}, unregistered commands are rejected with {@code UNKNOWN_COMMAND}.
     *
     * @param handler the fallback handler, or {@code null} to clear it
     */
    public void setDefaultCommandHandler(DefaultBridgeCommandHandler handler) {
        this.defaultCommandHandler = handler;
    }

    private void handleIncoming(String raw, CefQueryCallback callback) {
        try {
            JsonObject msg    = JsonParser.parseString(raw).getAsJsonObject();
            String id         = msg.get("id").getAsString();
            String command    = msg.get("command").getAsString();
            JsonElement argsEl = msg.get("args");

            BridgeCommandHandler handler = commands.get(command);
            if (handler == null) {
                if (defaultCommandHandler != null) {
                    final String cmd = command;
                    handler = new BridgeCommandHandler() {
                        @Override
                        public void handle(Object[] args, BridgeCallback callback) {
                            defaultCommandHandler.handle(cmd, args, callback);
                        }
                    };
                } else {
                    callback.failure(400, errorJson(id, "UNKNOWN_COMMAND", "Command not registered: " + command));
                    return;
                }
            }

            Object[] args = (argsEl != null && argsEl.isJsonArray())
                ? gson.fromJson(argsEl, Object[].class) : new Object[0];

            handler.handle(args, new BridgeCallback() {
                @Override public void success(Object result) {
                    JsonObject resp = new JsonObject();
                    resp.addProperty("id", id);
                    resp.addProperty("success", true);
                    resp.add("result", gson.toJsonTree(result));
                    callback.success(gson.toJson(resp));
                }
                @Override public void failure(String code, String message) {
                    callback.failure(500, errorJson(id, code, message));
                }
            });

            var list = listeners.get(command);
            if (list != null) list.forEach(l -> l.onEvent(command, args));
            anyEventListeners.forEach(l -> l.onEvent(command, args));

        } catch (Exception e) {
            LOG.error("Error in bridge", e);
            callback.failure(500, "{\"error\":\"BRIDGE_ERROR\"}");
        }
    }

    private String errorJson(String id, String code, String msg) {
        return String.format(
            "{\"id\":\"%s\",\"success\":false,\"error\":{\"code\":\"%s\",\"message\":\"%s\"}}",
            id, code, msg.replace("\"", "\\\""));
    }
}