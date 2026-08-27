# Installation — AnyLogic Web Toolkit

## Requirements

- AnyLogic 8.9.9 (PLE, University, or Professional)
- Java 17
- Windows x64

## Installation Steps

1. **Download and Extract:**
   Download the `AnyLogicWebToolkit-0.1.0.zip` from the Releases page and extract it to a permanent folder on your computer (e.g., `C:\AnyLogicLibs\AnyLogicWebToolkit\`).
   *(Alternatively, you can download just the `AnyLogicWebToolkit-0.1.0.jar` directly if you don't need the examples and docs).*

2. **Add the Toolkit to your AnyLogic Palette:**
   - Open AnyLogic.
   - At the bottom of the **Palette** panel, click the `+` button (Add Library).
   - Navigate to your extracted folder (or where you downloaded the file) and select `AnyLogicWebToolkit-0.1.0.jar`.
   - The `WebToolkit` palette will now appear, containing the `WebApp` agent.

3. **Runtime Directory:**
   The Chromium binaries are massive (~180 MB) and will be downloaded automatically by the `jcefmaven` library on the very first run. 
   - An internet connection is required during the first model execution.
   - The binaries will be stored in `~/.webtoolkit/runtime/`.

## JVM Arguments (If needed)

If AnyLogic shows Java Module errors when loading the library, go to **Tools → Preferences → Advanced** and add the following to **JVM Arguments**:
```
--add-opens=java.desktop/java.awt.peer=ALL-UNNAMED
--add-opens=java.desktop/sun.awt=ALL-UNNAMED
--add-opens=java.desktop/sun.awt.windows=ALL-UNNAMED
```

## Verification

To verify that the library is working, simply open the `examples/AnyLogicModels/BasicExample/BasicExample.alp` file and hit **Run**.
