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

public class WebBridge {

    private static final WebToolkitLogger LOG = WebToolkitLogger.get("BRIDGE");
    private final Gson gson = new Gson();
    private final Map<String, BridgeCommandHandler> commands = new ConcurrentHashMap<>();
    private final Map<String, CopyOnWriteArrayList<BridgeEventListener>> listeners = new ConcurrentHashMap<>();
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
              console.log('[WEBTK] API AnyLogic importada v0.2.0');
            })();
            """;
    }

    private CefBrowser browser;
    public void setBrowser(CefBrowser browser) { this.browser = browser; }

    public void emit(String event, Object data) {
        if (browser == null) { LOG.warn("emit() without browser"); return; }
        String json = gson.toJson(data);
        String js = String.format("window.__WEBTK__ && window.__WEBTK__.dispatch('%s',%s);", event.replace("'","\\'"), json);
        SwingUtilities.invokeLater(() -> browser.executeJavaScript(js, "webtoolkit://bridge", 0));
    }

    public void execute(String jsCode) {
        if (browser == null) return;
        SwingUtilities.invokeLater(() -> browser.executeJavaScript(jsCode, "webtoolkit://bridge", 0));
    }

    public void registerCommand(String name, BridgeCommandHandler handler) {
        commands.put(name, handler);
        LOG.debug("Comando registrado: " + name);
    }

    public void removeCommand(String name) { commands.remove(name); }

    public void on(String event, BridgeEventListener listener) {
        listeners.computeIfAbsent(event, k -> new CopyOnWriteArrayList<>()).add(listener);
    }

    public void off(String event, BridgeEventListener listener) {
        var list = listeners.get(event);
        if (list != null) list.remove(listener);
    }

    private final CopyOnWriteArrayList<BridgeEventListener> anyEventListeners = new CopyOnWriteArrayList<>();
    public void onAnyEvent(BridgeEventListener listener) {
        anyEventListeners.add(listener);
    }

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

    private DefaultBridgeCommandHandler defaultCommandHandler;
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
                    callback.failure(400, errorJson(id, "UNKNOWN_COMMAND", "Comando no registrado: " + command));
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