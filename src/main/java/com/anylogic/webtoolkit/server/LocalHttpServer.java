package com.anylogic.webtoolkit.server;

import com.anylogic.webtoolkit.bridge.WebBridge;
import com.anylogic.webtoolkit.logging.WebToolkitLogger;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Embedded local HTTP server providing standard loopback HTTP serving for local web assets.
 *
 * <p>Serves files from a base directory (typically where the HTML entry point resides)
 * with a fallback to the project root directory. Eliminates macOS signature validation
 * and custom protocol restrictions in JCEF.
 */
public class LocalHttpServer {

    private static final WebToolkitLogger LOG = WebToolkitLogger.get("SERVER");

    private final Path baseDir;
    private final Path fallbackDir;
    private HttpServer server;
    private ExecutorService executor;
    private int port = -1;

    /**
     * @param baseDir primary directory to serve files from (e.g. folder containing index.html)
     * @param fallbackDir secondary directory to resolve files if not found in baseDir (e.g. projectRoot)
     */
    public LocalHttpServer(Path baseDir, Path fallbackDir) {
        this.baseDir = baseDir != null ? baseDir.toAbsolutePath().normalize() : null;
        this.fallbackDir = fallbackDir != null ? fallbackDir.toAbsolutePath().normalize() : null;
    }

    /**
     * Starts the HTTP server on 127.0.0.1 on an ephemeral free port.
     */
    public synchronized void start() throws IOException {
        if (server != null) return;

        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        this.port = server.getAddress().getPort();

        executor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "webtk-http-" + port);
            t.setDaemon(true);
            return t;
        });
        server.setExecutor(executor);
        server.createContext("/", new ResourceHandler());
        server.start();

        LOG.info("Started local HTTP server at " + getBaseUrl());
    }

    /**
     * Stops the HTTP server and releases all associated resources.
     */
    public synchronized void stop() {
        if (server != null) {
            try {
                server.stop(0);
            } catch (Exception e) {
                LOG.warn("Error stopping HTTP server: " + e.getMessage());
            }
            server = null;
        }
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
        port = -1;
    }

    public boolean isRunning() {
        return server != null;
    }

    public int getPort() {
        return port;
    }

    public String getBaseUrl() {
        return "http://127.0.0.1:" + port;
    }

    public String getUrl(String relativePath) {
        if (relativePath == null || relativePath.isEmpty()) {
            return getBaseUrl() + "/";
        }
        String path = relativePath.replace('\\', '/');
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return getBaseUrl() + path;
    }

    private class ResourceHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                String method = exchange.getRequestMethod();

                // Handle CORS preflight
                if ("OPTIONS".equalsIgnoreCase(method)) {
                    exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                    exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, HEAD, OPTIONS");
                    exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "*");
                    exchange.sendResponseHeaders(204, -1);
                    exchange.close();
                    return;
                }

                if (!"GET".equalsIgnoreCase(method) && !"HEAD".equalsIgnoreCase(method)) {
                    exchange.sendResponseHeaders(405, -1);
                    exchange.close();
                    return;
                }

                String rawPath = exchange.getRequestURI().getPath();
                String decodedPath = URLDecoder.decode(rawPath != null ? rawPath : "/", StandardCharsets.UTF_8);

                // Special route for AnyLogic WebToolkit in-memory JS bridge API
                if (decodedPath.equals("/__webtk_api.js") || decodedPath.endsWith("/__webtk_api.js")) {
                    byte[] apiBytes = WebBridge.getApiScript().getBytes(StandardCharsets.UTF_8);
                    sendResponse(exchange, 200, "application/javascript; charset=UTF-8", apiBytes, "HEAD".equalsIgnoreCase(method));
                    return;
                }

                if (decodedPath.equals("/") || decodedPath.isEmpty()) {
                    decodedPath = "/index.html";
                }

                // Strip leading slashes for relative path resolution
                while (decodedPath.startsWith("/")) {
                    decodedPath = decodedPath.substring(1);
                }

                Path resolvedFile = resolveFile(decodedPath);
                if (resolvedFile == null || !Files.exists(resolvedFile) || Files.isDirectory(resolvedFile)) {
                    byte[] notFoundMsg = ("404 Not Found: " + decodedPath).getBytes(StandardCharsets.UTF_8);
                    sendResponse(exchange, 404, "text/plain; charset=UTF-8", notFoundMsg, "HEAD".equalsIgnoreCase(method));
                    return;
                }

                byte[] data = Files.readAllBytes(resolvedFile);
                String mimeType = guessMime(resolvedFile.getFileName().toString());

                // For HTML files, transparently rewrite legacy http://webtoolkit/__webtk_api.js to relative /__webtk_api.js
                if (mimeType.startsWith("text/html")) {
                    String html = new String(data, StandardCharsets.UTF_8);
                    if (html.contains("http://webtoolkit/__webtk_api.js")) {
                        html = html.replace("http://webtoolkit/__webtk_api.js", "/__webtk_api.js");
                        data = html.getBytes(StandardCharsets.UTF_8);
                    }
                }

                sendResponse(exchange, 200, mimeType, data, "HEAD".equalsIgnoreCase(method));
            } catch (Exception e) {
                LOG.error("Error handling request: " + exchange.getRequestURI(), e);
                try {
                    byte[] errBytes = ("500 Internal Server Error: " + e.getMessage()).getBytes(StandardCharsets.UTF_8);
                    sendResponse(exchange, 500, "text/plain; charset=UTF-8", errBytes, false);
                } catch (Exception ignored) {}
            }
        }

        private Path resolveFile(String relPath) {
            if (relPath.contains("..")) {
                // Prevent path traversal
                return null;
            }
            if (baseDir != null) {
                Path candidate = baseDir.resolve(relPath).normalize();
                if (candidate.startsWith(baseDir) && Files.exists(candidate)) {
                    return candidate;
                }
            }
            if (fallbackDir != null) {
                Path candidate = fallbackDir.resolve(relPath).normalize();
                if (candidate.startsWith(fallbackDir) && Files.exists(candidate)) {
                    return candidate;
                }
            }
            return null;
        }

        private void sendResponse(HttpExchange exchange, int status, String mimeType, byte[] data, boolean isHead) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", mimeType);
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Cache-Control", "no-cache, no-store, must-revalidate");
            if (data != null) {
                exchange.getResponseHeaders().set("Content-Length", String.valueOf(data.length));
            }
            if (isHead) {
                exchange.sendResponseHeaders(status, -1);
            } else {
                exchange.sendResponseHeaders(status, data != null ? data.length : -1);
                if (data != null && data.length > 0) {
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(data);
                    }
                }
            }
            exchange.close();
        }

        private String guessMime(String name) {
            String lower = name.toLowerCase();
            if (lower.endsWith(".html") || lower.endsWith(".htm")) return "text/html; charset=UTF-8";
            if (lower.endsWith(".css")) return "text/css; charset=UTF-8";
            if (lower.endsWith(".js") || lower.endsWith(".mjs")) return "application/javascript; charset=UTF-8";
            if (lower.endsWith(".json")) return "application/json; charset=UTF-8";
            if (lower.endsWith(".png")) return "image/png";
            if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
            if (lower.endsWith(".gif")) return "image/gif";
            if (lower.endsWith(".svg")) return "image/svg+xml";
            if (lower.endsWith(".ico")) return "image/x-icon";
            if (lower.endsWith(".webp")) return "image/webp";
            if (lower.endsWith(".woff2")) return "font/woff2";
            if (lower.endsWith(".woff")) return "font/woff";
            if (lower.endsWith(".ttf")) return "font/ttf";
            if (lower.endsWith(".otf")) return "font/otf";
            if (lower.endsWith(".eot")) return "application/vnd.ms-fontobject";
            if (lower.endsWith(".dae")) return "model/vnd.collada+xml";
            if (lower.endsWith(".glb")) return "model/gltf-binary";
            if (lower.endsWith(".gltf")) return "model/gltf+json";
            if (lower.endsWith(".obj")) return "text/plain; charset=UTF-8";
            if (lower.endsWith(".mp4")) return "video/mp4";
            if (lower.endsWith(".webm")) return "video/webm";
            if (lower.endsWith(".mp3")) return "audio/mpeg";
            if (lower.endsWith(".wav")) return "audio/wav";
            if (lower.endsWith(".wasm")) return "application/wasm";
            if (lower.endsWith(".pdf")) return "application/pdf";
            if (lower.endsWith(".xml")) return "application/xml; charset=UTF-8";
            if (lower.endsWith(".txt")) return "text/plain; charset=UTF-8";
            if (lower.endsWith(".csv")) return "text/csv; charset=UTF-8";
            if (lower.endsWith(".xlsx")) return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            return "application/octet-stream";
        }
    }
}
