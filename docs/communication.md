# Communication Guide (Java ↔ JS)

The AnyLogic Web Toolkit allows seamless, asynchronous data exchange between your HTML UI and your AnyLogic simulation.

To enable communication, you must include the magic API script in your HTML file:
```html
<script src="http://webtoolkit/__webtk_api.js"></script>
```

This injects the global `AnyLogic` object into your JavaScript context.

---

## 1. Web to AnyLogic (JS -> Java)

To send a command from your Web UI to your AnyLogic model, use `AnyLogic.call()`. This method returns a Promise, allowing you to use `async/await` to wait for a response from the simulation.

**JavaScript (Web UI):**
```javascript
async function dispatchTruck() {
    try {
        const orderData = { id: "order-123", priority: "High" };
        
        // Call the 'createOrder' command in Java
        await AnyLogic.call("createOrder", orderData);
        
        console.log("Order dispatched successfully!");
    } catch(err) {
        console.error("Failed to dispatch:", err);
    }
}
```

**Java (AnyLogic `WebApp` Block -> `On message received` action):**
```java
if (type.equals("createOrder")) {
    // Cast the JSON object to a Java Map
    java.util.Map<String, Object> dataMap = (java.util.Map<String, Object>) data;
    
    // Note: JSON strings -> String, JSON numbers -> Double, JSON booleans -> Boolean
    String id       = (String) dataMap.get("id");
    String priority = (String) dataMap.get("priority");
    
    // Call internal logic
    main.createOrder(id, priority);
}
```

> **Type mapping:** Gson deserializes JSON values to Java types as follows: JSON strings → `String`, JSON numbers → `Double`, JSON booleans → `Boolean`, JSON objects → `java.util.LinkedHashMap<String, Object>`, JSON arrays → `java.util.ArrayList<Object>`.

---

## 2. AnyLogic to Web (Java -> JS)

Sometimes the simulation needs to push data to the web app (e.g., live vehicle positions, simulation time updates, or completion events).

**Java (AnyLogic Model):**
```java
// Usually inside a cyclic event or statechart action
// Emits the 'vehicleMoved' event to JS
webApp.dialog.getBridge().emit("vehicleMoved", 
    java.util.Map.of("lat", vehicle.getLat(), "lng", vehicle.getLng())
);
```

**JavaScript (Web UI):**
```javascript
// Listen for events emitted by AnyLogic
AnyLogic.events.on("vehicleMoved", (payload) => {
    const lat = payload.lat;
    const lng = payload.lng;
    
    // Update map marker
    updateMarker(lat, lng);
});
```

### Event listener variants

| Method | Description |
|---|---|
| `AnyLogic.events.on(event, handler)` | Subscribe permanently to an event. |
| `AnyLogic.events.off(event, handler)` | Remove a previously registered handler. |
| `AnyLogic.events.once(event, handler)` | Subscribe for a single firing only; auto-removed after the first call. |

---

## 3. Requesting Data on Startup

When the web app opens, it usually needs to download the initial state of the simulation (like existing orders or the map's depot coordinates). 

The best practice is for the web app to alert AnyLogic when it's fully loaded by firing a command:

**JavaScript (Web UI):**
```javascript
window.addEventListener('load', () => {
    // Tell AnyLogic we are ready!
    AnyLogic.call('__ready__'); 
});
```

**Java (AnyLogic `WebApp` Block -> `On message received` action, handling `'__ready__'`):**
```java
// This block runs when JS fires the __ready__ command
java.util.Map<String, Object> depotCoords = new java.util.LinkedHashMap<>();
depotCoords.put("lat", depot.getLatitude());
depotCoords.put("lng", depot.getLongitude());

webApp.dialog.getBridge().emit("setDepot", depotCoords);
```

---

## 4. Shared State (AnyLogic.state)

The toolkit provides a built-in key-value state store that is automatically synchronized between Java and JavaScript. This is useful for sharing simple values without writing custom commands.

**JavaScript:**
```javascript
// Read a value set by Java
const speed = await AnyLogic.state.get("simulationSpeed");

// Write a value from JS (readable by Java via webApp.dialog.getStateSync().get(...))
await AnyLogic.state.set("zoom", 12);

// Subscribe to changes pushed from Java (via StateSync.set())
AnyLogic.state.subscribe("simulationSpeed", (newValue) => {
    document.getElementById("speedLabel").innerText = newValue;
});
```

**Java:**
```java
// Push a value to JS (triggers AnyLogic.state.subscribe callbacks in JS)
webApp.dialog.getStateSync().set("simulationSpeed", engine.getSpeed());

// Read a value set by JS
Object zoom = webApp.dialog.getStateSync().get("zoom");

// React to any key change (from either side)
webApp.dialog.getStateSync().onAnyChange((key, value) -> {
    traceln("State changed: " + key + " = " + value);
});
```

---

## 5. File System API (AnyLogic.files)

The toolkit exposes native OS file dialogs and direct file read/write access to JavaScript, bypassing the browser sandbox.

**JavaScript:**
```javascript
// Open a native file picker and get the selected file path
const path = await AnyLogic.files.openDialog("Select a file", "data.xlsx");
if (path) {
    // Read as plain text
    const text = await AnyLogic.files.read(path, false);

    // Or read as Base64 (for binary files like XLSX)
    const base64 = await AnyLogic.files.read(path, true);
}

// Save dialog
const savePath = await AnyLogic.files.saveDialog("Save As", "output.xlsx");
if (savePath) {
    await AnyLogic.files.write(savePath, textContent, false);
}
```

| Method | Parameters | Returns |
|---|---|---|
| `AnyLogic.files.openDialog(title, defaultName)` | Optional dialog title and default filename | `Promise<string\|null>` — absolute path, or `null` if cancelled |
| `AnyLogic.files.saveDialog(title, defaultName)` | Optional dialog title and default filename | `Promise<string\|null>` — absolute path, or `null` if cancelled |
| `AnyLogic.files.read(path, isBase64)` | Absolute path; `false` for UTF-8 text, `true` for Base64 | `Promise<string>` |
| `AnyLogic.files.write(path, data, isBase64)` | Absolute path, content string, `false` for text / `true` for Base64 | `Promise<boolean>` |

---

## 6. Dialog Control (AnyLogic.dialog)

The JavaScript API exposes two methods to control the browser window:

```javascript
// Close the browser window from JavaScript
AnyLogic.dialog.close();

// Change the window title from JavaScript
AnyLogic.dialog.setTitle("New Title");
```

> **Note:** `setTitle()` works by setting `document.title`, which the toolkit intercepts via CEF's display handler to update the native window title bar.

---

## 7. Runtime Info (AnyLogic.runtime)

A read-only object exposing basic runtime metadata:

```javascript
console.log(AnyLogic.runtime.version);   // e.g. "0.2.0"
console.log(AnyLogic.runtime.platform);  // "windows"
```

---

## 8. Advanced: Java-side Event Listeners (WebBridge)

In addition to the `On message received` AnyLogic action, you can register programmatic Java listeners directly on the bridge object:

```java
// Listen for a specific command received from JS
webApp.dialog.getBridge().on("myCommand", (event, args) -> {
    traceln("Received: " + event + ", args: " + java.util.Arrays.toString((Object[]) args));
});

// Remove a specific listener
webApp.dialog.getBridge().off("myCommand", myListenerReference);

// Listen for ALL commands received from JS
webApp.dialog.getBridge().onAnyEvent((event, args) -> {
    traceln("Any event: " + event);
});
```

> **Note:** `BridgeEventListener.onEvent(String event, Object data)` receives the full `Object[]` args array deserialized from the JS call as the `data` parameter.
