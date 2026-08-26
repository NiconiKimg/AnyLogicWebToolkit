package com.anylogic.webtoolkit;

import com.anylogic.webtoolkit.bridge.WebBridge;
import com.anylogic.webtoolkit.core.WebConfig;
import com.anylogic.webtoolkit.security.Permission;
import com.anylogic.webtoolkit.security.WebPermissions;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Tests de las clases que no dependen de Chromium. */
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
        // No excepcion = registro exitoso
    }

    @Test
    void webBridge_emitWithoutBrowser_doesNotThrow() {
        var bridge = new WebBridge();
        // Sin browser asignado, emit no debe lanzar excepcion
        assertDoesNotThrow(() -> bridge.emit("event", "data"));
    }
}
