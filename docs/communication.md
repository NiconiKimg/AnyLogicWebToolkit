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
        const orderData = { id: 123, priority: "High" };
        
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
    
    String id = (String) dataMap.get("id");
    String priority = (String) dataMap.get("priority");
    
    // Call internal logic
    main.createOrder(id, priority);
}
```

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

**Java (AnyLogic `WebApp` Block -> `onReady`):**
```java
// This block runs when JS fires the __ready__ command
java.util.Map<String, Object> depotCoords = new java.util.LinkedHashMap<>();
depotCoords.put("lat", depot.getLatitude());
depotCoords.put("lng", depot.getLongitude());

webApp.dialog.getBridge().emit("setDepot", depotCoords);
```
