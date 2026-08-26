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

/**
 * CEF resource handler for the virtual {@code http://webtoolkit/} scheme.
 *
 * <p>Intercepts all requests whose URL begins with {@code http://webtoolkit/} and
 * serves them from the local file system, rooted at the AnyLogic project directory.
 * The special path {@code __webtk_api.js} is served from memory (the JS API
 * injected by {@link com.anylogic.webtoolkit.bridge.WebBridge#getApiScript()})
 * rather than from disk.
 *
 * <p>A new instance is created per request by {@link com.anylogic.webtoolkit.ui.WebDialog}.
 */
public class WebSchemeHandler extends CefResourceHandlerAdapter {

    private static final WebToolkitLogger LOG = WebToolkitLogger.get("BROWSER");

    private final Path projectRoot;
    private byte[] data;
    private String mimeType;
    private int offset;

    /**
     * @param projectRoot the root directory from which relative file paths are resolved;
     *                    typically {@code Paths.get(System.getProperty("user.dir"))}
     */
    public WebSchemeHandler(Path projectRoot) { this.projectRoot = projectRoot; }

    /**
     * Resolves the requested URL to a file on disk (or the in-memory API script),
     * reads the content, and signals CEF to proceed.
     *
     * @return {@code true} in all cases, as required by the CEF resource handler contract
     */
    @Override
    public boolean processRequest(CefRequest request, CefCallback callback) {
        try {
            String path = extractPath(request.getURL());

            if (path.endsWith("__webtk_api.js")) {
                data = com.anylogic.webtoolkit.bridge.WebBridge.getApiScript()
                           .getBytes(java.nio.charset.StandardCharsets.UTF_8);
                mimeType = "application/javascript";
                offset = 0;
                callback.Continue();
                return true;
            }

            Path file = projectRoot.resolve(path).normalize();
            if (!Files.exists(file)) { callback.cancel(); return true; }
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

    /** Strips the {@code http://webtoolkit/} prefix, returning the bare relative path. */
    private String extractPath(String url) {
        try {
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