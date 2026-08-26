# Quick Start Guide

This guide will show you how to embed your first web application into AnyLogic using the Web Toolkit in less than 5 minutes.

## 1. Installation

1. Download the latest `AnyLogicWebToolkit-x.x.x.zip` from the Releases page.
2. Extract the contents to a safe folder on your computer.
3. Open your AnyLogic 8.9.9+ project.
4. In the AnyLogic Palette, right-click and select **Manage Libraries**.
5. Click **Add**, locate the `AnyLogicWebToolkit.jar` file you just extracted, and import it.
6. The `WebToolkit` palette will now appear in your AnyLogic IDE containing the `WebApp` agent block.

## 2. Setting up the Agent

1. Open your `Main` agent in AnyLogic.
2. Drag the `WebApp` block from the WebToolkit palette onto the `Main` canvas. Name it `webApp`.
3. In the properties of `webApp`:
   - Set **htmlPath** to the path of your HTML folder (e.g., `"examples/basic/index.html"`). This path is relative to the directory where your `.alp` file is saved.
   - Set **windowTitle** to `"My First Web App"`.

## 3. Initializing the Runtime

To ensure the Chromium engine starts correctly and binds to the web interface, you must initialize it when the `WebApp` agent starts.

In the **On startup** action of your `webApp` block, add:

```java
// 1. Initialize Chromium (only happens once)
com.anylogic.webtoolkit.core.WebRuntime.getInstance().initialize(new com.anylogic.webtoolkit.core.WebConfig());

// 2. Create the Window
dialog = new com.anylogic.webtoolkit.ui.WebDialog(htmlPath);
dialog.setTitle(windowTitle);

// 3. Setup incoming message handler (JS -> Java)
dialog.getBridge().setDefaultCommandHandler((command, args, cb) -> {
    Object data = (args != null && args.length > 0) ? args[0] : null;
    this.onMessageReceived(command, data);
    cb.success(null);
});

// 4. Setup ready event (to know when HTML finished loading)
dialog.getBridge().registerCommand("__ready__", (args, cb) -> {
    this.onReady();
    cb.success(null);
});

// 5. Show the window
dialog.open();
```

In the **On destroy** action of your `webApp` block, add:

```java
if (dialog != null) {
    dialog.close();
}
```

## 4. Run your Model

Click the **Run** button in AnyLogic. 
A separate, native window will appear displaying your HTML interface! 

> **Tip:** You can hide and show the window programmatically using `webApp.dialog.hide()` and `webApp.dialog.show()`.

To learn how to communicate between the Web App and the AnyLogic model, proceed to the [Communication Guide](communication.md).
