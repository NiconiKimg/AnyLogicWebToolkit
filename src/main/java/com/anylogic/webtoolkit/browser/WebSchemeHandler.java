package com.anylogic.webtoolkit.browser;

import com.anylogic.webtoolkit.logging.WebToolkitLogger;
import org.cef.callback.CefCallback;
import org.cef.handler.CefResourceHandlerAdapter;
import org.cef.misc.IntRef;
import org.cef.misc.StringRef;
import org.cef.network.CefRequest;
import org.cef.network.CefResponse;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

public class WebSchemeHandler extends CefResourceHandlerAdapter {

    private static final WebToolkitLogger LOG = WebToolkitLogger.get("BROWSER");

    private final Path projectRoot;
    private byte[] data;
    private String mimeType;
    private int offset;

    public WebSchemeHandler(Path projectRoot) { this.projectRoot = projectRoot; }

    @Override
    public boolean processRequest(CefRequest request, CefCallback callback) {
        try {
            String path = extractPath(request.getURL());
            
            // Provide the magic API script
            if (path.endsWith("__webtk_api.js")) {
                data = com.anylogic.webtoolkit.bridge.WebBridge.getApiScript().getBytes(java.nio.charset.StandardCharsets.UTF_8);
                mimeType = "application/javascript";
                offset = 0;
                callback.Continue();
                return true;
            }

            Path file = projectRoot.resolve(path).normalize();
            if (!file.startsWith(projectRoot)) { callback.cancel(); return true; }
            if (!Files.exists(file))            { callback.cancel(); return true; }
            data     = Files.readAllBytes(file);
            mimeType = guessMime(file.getFileName().toString());
            offset   = 0;
            callback.Continue();
        } catch (Exception e) {
            LOG.error("Error serving resource", e);
            callback.cancel();
        }
        return true;
    }

    @Override
    public void getResponseHeaders(CefResponse response, IntRef responseLength, StringRef redirectUrl) {
        response.setStatus(200);
        response.setMimeType(mimeType);
        response.setHeaderByName("Access-Control-Allow-Origin", "*", false);
        responseLength.set(data != null ? data.length : 0);
    }

    @Override
    public boolean readResponse(byte[] dataOut, int bytesToRead, IntRef bytesRead, CefCallback callback) {
        if (data == null || offset >= data.length) return false;
        int n = Math.min(bytesToRead, data.length - offset);
        System.arraycopy(data, offset, dataOut, 0, n);
        bytesRead.set(n);
        offset += n;
        return true;
    }

    private String extractPath(String url) {
        try {
            // intercept http://webtoolkit/{rest}
            String path = new URI(url).getPath();
            if (path != null && path.startsWith("/")) {
                return path.substring(1);
            }
            return "";
        } catch (Exception e) { return ""; }
    }

    private String guessMime(String name) {
        if (name.endsWith(".html") || name.endsWith(".htm")) return "text/html";
        if (name.endsWith(".css"))   return "text/css";
        if (name.endsWith(".js"))    return "application/javascript";
        if (name.endsWith(".json"))  return "application/json";
        if (name.endsWith(".png"))   return "image/png";
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
        if (name.endsWith(".svg"))   return "image/svg+xml";
        if (name.endsWith(".ico"))   return "image/x-icon";
        if (name.endsWith(".woff2")) return "font/woff2";
        if (name.endsWith(".xlsx"))  return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        return "application/octet-stream";
    }
}