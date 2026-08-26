# Instalacion — AnyLogic Web Toolkit

## Requisitos

- AnyLogic 8.9.9 (PLE o superior)
- Java 17
- Windows x64

## Pasos

1. **Descomprimir** `AnyLogicWebToolkit-0.1.0-win64.zip` en una carpeta permanente.
   Ejemplo: `C:\AnyLogicLibs\AnyLogicWebToolkit\`

2. **Agregar el JAR al modelo:**
   - Abre tu modelo en AnyLogic
   - Ve a **Model → Properties → Dependencies**
   - Haz clic en **Add JARs**
   - Selecciona `AnyLogicWebToolkit.jar`

3. **Directorio runtime:**
   Si el folder `runtime/` esta junto al JAR, se detecta automaticamente.
   Si esta en otra ubicacion, configurar al inicio del modelo:
   ```java
   WebRuntime.getInstance().initialize(
       WebConfig.builder().runtimeDir(new File("C:/ruta/a/runtime/windows-amd64")).build()
   );
   ```

4. **Primera ejecucion:**
   Si los binarios CEF no estan en `runtime/windows-amd64/`, jcefmaven los descargara
   automaticamente (~180 MB). Requiere conexion a internet solo en la primera vez.

## JVM Arguments (si es necesario)

Si AnyLogic muestra errores de modulo Java al cargar la libreria, agregar en
**Preferences → Advanced → JVM Arguments**:
```
--add-opens=java.desktop/java.awt.peer=ALL-UNNAMED
--add-opens=java.desktop/sun.awt=ALL-UNNAMED
--add-opens=java.desktop/sun.awt.windows=ALL-UNNAMED
```

## Verificacion

Ejecutar el ejemplo `examples/basic/index.html` desde un modelo AnyLogic:
```java
WebRuntime.getInstance().initialize();
WebDialog d = new WebDialog("examples/basic/index.html");
d.getBridge().registerCommand("hello", (args, cb) -> cb.success("Hola desde AnyLogic!"));
d.open();
```
