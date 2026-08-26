# Architecture Decision Record — AnyLogic Web Toolkit v0.1.0

**Target:** AnyLogic 8.9.9 PLE · Java 17 · Windows x64

---

## Decisiones clave

| ADR | Decisión | Razón |
|-----|----------|-------|
| 001 | **Motor web: JCEF via jcefmaven** (Chromium 146) | Único motor con ES6+, Leaflet, FileReader, fetch y DevTools funcionales |
| 002 | **JavaFX WebView: DESCARTADO** | Leaflet falla, WebSocket tiene bugs, JSC antiguo |
| 003 | **Browser interno de AnyLogic: NO reutilizable** | Sin API pública; acceso requeriría reflection invasiva |
| 004 | **Resource serving: esquema `webtoolkit://`** | Evita CORS, oculta rutas del OS, control de acceso total |
| 005 | **Build: Gradle + Shadow Plugin** | Shading de dependencias para evitar conflictos con classpath de AnyLogic |
| 006 | **Excel: SheetJS en frontend por defecto** | El usuario ya lo usa; sin dependencia Java adicional |
| 007 | **Distribución: ZIP con JAR + `runtime/` separado** | Binarios CEF (~180 MB) no pueden embeberse en JAR |
| 008 | **Threading: `SwingUtilities.invokeLater` para toda operación CEF** | CEF y Swing tienen hilos distintos; invokeLater es la única vía segura |
| 009 | **Security: allowlist explícita de comandos** | Solo se expone al JS lo que el desarrollador registra explícitamente |

---

## AnyLogic 8.9.9 — Lo que importa

- Usa **Java 17**. Compilar la librería con `--release 17`.
- IDE basado en **Eclipse OSGi** — riesgo de conflicto de ClassLoaders → usar shadow JAR.
- AnyLogic incluye su propio Chromium para animaciones del modelo. No tiene API pública para custom libraries.
- En **PLE**: se pueden *usar* librerías externas (JAR en Dependencies), no crear ni exportar.
- Al exportar modelos, AnyLogic incluye sus binarios Chromium en `chromium/` — son independientes de los nuestros.

---

## Versiones fijadas

```
jcefmaven:   146.0.10
CEF:         146.0.10
Chromium:    146.0.7680.179
Gson:        2.11.0  (shaded)
Gradle:      8.7
Java target: 17
```

---

## Distribución

```
AnyLogicWebToolkit-0.1.0-win64/
├── AnyLogicWebToolkit.jar   ← agregar a Model > Dependencies
├── runtime/windows-amd64/   ← binarios CEF (~180 MB)
├── licenses/
├── examples/
└── INSTALL.md
```

---

## Elementos UNKNOWN (requieren validación experimental)

1. Compatibilidad de jcefmaven con el ClassLoader OSGi de AnyLogic 8.9.9
2. Comportamiento de shutdown cuando AnyLogic cierra abruptamente
3. Capacidad de PLE para cargar librerías nativas vía `System.loadLibrary`
