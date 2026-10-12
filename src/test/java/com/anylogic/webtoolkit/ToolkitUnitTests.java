package com.anylogic.webtoolkit;

import com.anylogic.webtoolkit.bridge.WebBridge;
import com.anylogic.webtoolkit.core.WebConfig;
import com.anylogic.webtoolkit.security.Permission;
import com.anylogic.webtoolkit.security.WebPermissions;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Unit tests for classes that do not depend on Chromium (no display required). */
class ToolkitUnitTests {

    @Test
    void webConfig_defaults() {
        var cfg = WebConfig.defaults();
        assertNull(cfg.getRuntimeDir());
        assertTrue(cfg.isInternetAccess());
        assertFalse(cfg.isDevMode());
    }

    @Test
    void webConfig_builder() {
        var cfg = WebConfig.builder()
            .internetAccess(false)
            .devMode(true)
            .defaultSize(800, 600)
            .build();
        assertFalse(cfg.isInternetAccess());
        assertTrue(cfg.isDevMode());
        assertEquals(800, cfg.getDefaultWidth());
    }

    @Test
    void permissions_defaults() {
        var p = new WebPermissions();
        assertTrue(p.isAllowed(Permission.MODEL_READ));
        assertTrue(p.isAllowed(Permission.PROJECT_FILES));
        assertFalse(p.isAllowed(Permission.SIMULATION_CONTROL));
        assertFalse(p.isAllowed(Permission.DEVTOOLS));
    }

    @Test
    void permissions_allow_deny() {
        var p = new WebPermissions();
        p.allow(Permission.DEVTOOLS);
        assertTrue(p.isAllowed(Permission.DEVTOOLS));
        p.deny(Permission.MODEL_READ);
        assertFalse(p.isAllowed(Permission.MODEL_READ));
    }

    @Test
    void permissions_requireOrThrow() {
        var p = new WebPermissions();
        assertThrows(SecurityException.class, () -> p.requireOrThrow(Permission.SIMULATION_CONTROL));
        p.allow(Permission.SIMULATION_CONTROL);
        assertDoesNotThrow(() -> p.requireOrThrow(Permission.SIMULATION_CONTROL));
    }

    @Test
    void webBridge_registerCommand() {
        var bridge = new WebBridge();
        bridge.registerCommand("test", (args, cb) -> cb.success("ok"));
    }

    @Test
    void webBridge_emitWithoutBrowser_doesNotThrow() {
        var bridge = new WebBridge();
    }

    @Test
    void stateSync_listeners() {
        var sync = new com.anylogic.webtoolkit.core.StateSync();
        var changes = new java.util.ArrayList<Object>();
        sync.onChange("key1", changes::add);
        
        sync.set("key1", "val1");
        assertEquals(1, changes.size());
        assertEquals("val1", changes.get(0));
        assertEquals("val1", sync.get("key1"));
    }

    @Test
    void webFileSystem_readWrite() throws Exception {
        java.nio.file.Path temp = java.nio.file.Files.createTempFile("webtk-test", ".txt");
        try {
            com.anylogic.webtoolkit.core.WebFileSystem.writeText(temp.toString(), "Hello WebToolkit");
            String read = com.anylogic.webtoolkit.core.WebFileSystem.readText(temp.toString());
            assertEquals("Hello WebToolkit", read);
        } finally {
            java.nio.file.Files.deleteIfExists(temp);
        }
    }

    @Test
    void localHttpServer_startAndStop() throws Exception {
        var server = new com.anylogic.webtoolkit.server.LocalHttpServer(null, null);
        assertFalse(server.isRunning());
        assertEquals(-1, server.getPort());

        server.start();
        assertTrue(server.isRunning());
        assertTrue(server.getPort() > 0);
        assertTrue(server.getBaseUrl().startsWith("http://127.0.0.1:"));

        server.stop();
        assertFalse(server.isRunning());
    }

    @Test
    void localHttpServer_servesFilesAndApi() throws Exception {
        java.nio.file.Path tempDir = java.nio.file.Files.createTempDirectory("webtk-server-test");
        java.nio.file.Path fallbackDir = java.nio.file.Files.createTempDirectory("webtk-server-fb");

        try {
            java.nio.file.Path html = tempDir.resolve("index.html");
            java.nio.file.Files.writeString(html, "<html><head><script src=\"http://webtoolkit/__webtk_api.js\"></script></head><body><h1>Hello</h1></body></html>");

            java.nio.file.Path sharedAsset = fallbackDir.resolve("model.dae");
            java.nio.file.Files.writeString(sharedAsset, "<COLLADA></COLLADA>");

            var server = new com.anylogic.webtoolkit.server.LocalHttpServer(tempDir, fallbackDir);
            server.start();

            var client = java.net.http.HttpClient.newHttpClient();

            // 1. Check index.html with legacy script rewrite
            var req1 = java.net.http.HttpRequest.newBuilder(java.net.URI.create(server.getUrl("/index.html"))).GET().build();
            var resp1 = client.send(req1, java.net.http.HttpResponse.BodyHandlers.ofString());
            assertEquals(200, resp1.statusCode());
            assertTrue(resp1.headers().firstValue("Content-Type").orElse("").contains("text/html"));
            assertTrue(resp1.body().contains("/__webtk_api.js"));
            assertFalse(resp1.body().contains("http://webtoolkit/__webtk_api.js"));

            // 2. Check in-memory API script endpoint
            var req2 = java.net.http.HttpRequest.newBuilder(java.net.URI.create(server.getUrl("/__webtk_api.js"))).GET().build();
            var resp2 = client.send(req2, java.net.http.HttpResponse.BodyHandlers.ofString());
            assertEquals(200, resp2.statusCode());
            assertTrue(resp2.headers().firstValue("Content-Type").orElse("").contains("application/javascript"));
            assertTrue(resp2.body().contains("window.AnyLogic ="));

            // 3. Check fallback directory resolution and DAE mime
            var req3 = java.net.http.HttpRequest.newBuilder(java.net.URI.create(server.getUrl("/model.dae"))).GET().build();
            var resp3 = client.send(req3, java.net.http.HttpResponse.BodyHandlers.ofString());
            assertEquals(200, resp3.statusCode());
            assertEquals("model/vnd.collada+xml", resp3.headers().firstValue("Content-Type").orElse(""));
            assertTrue(resp3.body().contains("<COLLADA>"));

            // 4. Check 404 for missing file
            var req4 = java.net.http.HttpRequest.newBuilder(java.net.URI.create(server.getUrl("/not_found.png"))).GET().build();
            var resp4 = client.send(req4, java.net.http.HttpResponse.BodyHandlers.ofString());
            assertEquals(404, resp4.statusCode());

            server.stop();
        } finally {
            java.nio.file.Files.walk(tempDir)
                .sorted(java.util.Comparator.reverseOrder())
                .forEach(p -> { try { java.nio.file.Files.deleteIfExists(p); } catch (Exception ignored) {} });
            java.nio.file.Files.walk(fallbackDir)
                .sorted(java.util.Comparator.reverseOrder())
                .forEach(p -> { try { java.nio.file.Files.deleteIfExists(p); } catch (Exception ignored) {} });
        }
    }
}
