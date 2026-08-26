# WebApp Agent Configuration

When you drag the `WebApp` agent from the WebToolkit palette onto your `Main` canvas, you can configure it via the Properties panel in AnyLogic. This document explains each parameter.

## 1. htmlPath (String)
**Description:** The path to the main HTML file of your web application.
**Usage:**
- By default, AnyLogic expects resources to be bundled inside the model's directory. 
- The path provided here is resolved relative to the folder where your `.alp` file is saved.
- **Example:** If you place `index.html` in the same folder as your model, simply write `"index.html"`.
- **Note:** Do not forget the double quotes (`""`), as this is a Java String expression.

## 2. windowTitle (String)
**Description:** The title that appears at the top of the native Chromium window.
**Usage:**
- **Example:** `"My Logistics Dashboard"`
- This title can also be changed dynamically from JavaScript later on by calling `AnyLogic.dialog.setTitle("New Title");`.

## 3. width & height (int)
**Description:** The initial dimensions (in pixels) of the browser window.
**Usage:** 
- The default is usually `1280`x`720`.
- The window is resizable by the user during execution.

## 4. resizable (boolean)
**Description:** Controls whether the user can resize the Chromium window.
**Usage:** Set to `true` or `false`.

## 5. On message received (Action)
**Description:** This is the most important callback. It triggers whenever your JavaScript code calls `AnyLogic.call(type, data)`.
**Available Local Variables:**
- `type` (String): The name of the command sent from JS.
- `data` (Object): The payload sent from JS (usually a `LinkedHashMap` if a JSON object was sent).

**Example Usage:**
```java
if (type.equals("dispatchTruck")) {
    // Cast the payload to a Map
    java.util.Map<String, Object> payload = (java.util.Map<String, Object>) data;
    String target = (String) payload.get("targetId");
    
    // Execute logic in your model
    myTruck.moveTo(target);
    
    // Optionally respond back with an event
    webApp.dialog.getBridge().emit("truckDispatched", target);
}
```

## 6. On ready (Action)
**Description:** This action triggers when the HTML page is fully loaded, but **only if** the web page explicitly notifies Java.
**Usage:** Use this hook to send initial state data from your simulation to the web interface. 

**Important:** For this action to trigger, your JavaScript code must manually call `AnyLogic.call('__ready__')` after it finishes rendering its UI (e.g. inside `window.onload`).

**Example Usage (Java):**
```java
// Send the initial population count as soon as the UI is ready
webApp.dialog.getBridge().emit("updatePopulation", agents.size());
```
