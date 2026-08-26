# Quick Start Guide

This guide will show you how to embed your first web application into AnyLogic using the Web Toolkit in less than 5 minutes.

## 1. Installation

1. Download the latest `AnyLogicWebToolkit-x.x.x.zip` from the Releases page.
2. Extract the contents to a safe folder on your computer.
3. Open your AnyLogic 8.9.9+ project.
4. In AnyLogic, at the bottom of the **Palette** panel, click the **+** button (Add Library).
5. Navigate to your extracted folder, select `WebToolkit.jar`, and click **Open**.
6. The `WebToolkit` palette will now appear in your AnyLogic IDE containing the `WebApp` agent block.

## 2. Setting up the Agent

1. Open your `Main` agent in AnyLogic.
2. Drag the `WebApp` block from the WebToolkit palette onto the `Main` canvas. Name it `webApp`.
3. In the properties of `webApp`:
   - Set **htmlPath** to the path of your HTML folder (e.g., `"examples/basic/index.html"`). This path is relative to the directory where your `.alp` file is saved.
   - Set **windowTitle** to `"My First Web App"`.

## 3. Initializing the Runtime

The Chromium runtime is automatically initialized when the `WebApp` agent starts. You **do not** need to write any initialization code! 

The `WebApp` block handles everything internally:
- It initializes the `WebRuntime`.
- It creates and opens the `WebDialog`.
- It safely closes the Chromium engine when the model terminates.

## 4. Run your Model

Click the **Run** button in AnyLogic. 
A separate, native window will appear displaying your HTML interface! 

> **Tip:** You can hide and show the window programmatically using `webApp.dialog.hide()` and `webApp.dialog.show()`.

To learn how to communicate between the Web App and the AnyLogic model, proceed to the [Communication Guide](communication.md).
