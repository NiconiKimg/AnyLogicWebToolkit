# AnyLogic Web Toolkit

![Version](https://img.shields.io/badge/version-0.1.0-blue.svg)
![Java](https://img.shields.io/badge/Java-17-orange.svg)
![AnyLogic](https://img.shields.io/badge/AnyLogic-8.9.9+-green.svg)
![OS](https://img.shields.io/badge/OS-Windows_x64-lightgrey.svg)
![CI](https://github.com/NiconiKimg/AnyLogicWebToolkit/actions/workflows/ci.yml/badge.svg)

AnyLogic Web Toolkit is an open-source library that empowers developers to seamlessly embed modern web applications (HTML, CSS, JavaScript) directly into AnyLogic simulation models. It provides a robust, bidirectional bridge between your Java simulation logic and rich web frontends like Leaflet maps, React dashboards, or custom D3.js visualizations.

Powered by a native embedded Chromium engine (JCEF), it bypasses the limitations of the internal AnyLogic browser, giving you full control over DevTools, local filesystem access, and modern web standards.

## Features

- **Embedded Chromium Engine:** Fully isolated JCEF instance that runs natively within AnyLogic.
- **Bidirectional Communication:** Send commands from JS to Java (`AnyLogic.call(...)`) and push real-time events from Java to JS (`bridge.emit(...)`).
- **File System API:** Native OS file dialogs (Open/Save) exposed safely to JavaScript without browser sandbox restrictions.
- **Offline Capable:** Load local HTML files and assets seamlessly via the custom `http://webtoolkit/` protocol. No external web server required.
- **Developer Tools:** Press `F12` during simulation to open standard Chrome DevTools for rapid UI debugging.

> [!NOTE]
> **First-time usage:** The first time you run a model, the toolkit will automatically download the required Chromium engine binaries (JCEF) for your operating system in the background. This may take a minute or two depending on your connection. Subsequent runs will start instantly.

## Quick Start

To install and start using the toolkit, check out the [Quick Start Guide](docs/quick-start.md).

## Repository Structure

```text
AnyLogicWebToolkit/
├── AnyLogicLibrary/           # Source AnyLogic .alp library with the WebApp agent
├── docs/                      # Documentation and guides
├── examples/                  # Ready-to-run AnyLogic models with embedded HTML UIs
│   └── AnyLogicModels/
│       ├── BasicExample/      # Hello World example
│       ├── BidirectionalExample/  # Two-way communication example
│       ├── LogisticsDigitalTwin/ # Logistics Digital Twin example
│       └── SmartFactoryTwin/  # Advanced 3D React Digital Twin example
├── src/main/java/...          # Java source code for the Web Toolkit
├── build.gradle               # Gradle build script
└── README.md
```

## Documentation

- **[Java API Reference (Javadoc)](https://NiconiKimg.github.io/AnyLogicWebToolkit/)**
- [Installation & Quick Start](docs/quick-start.md)
- [Agent Configuration Guide](docs/agent-configuration.md)
- [Communication (Java ↔ JS)](docs/communication.md)

## Included Examples

The `examples/AnyLogicModels/` directory contains ready-to-run AnyLogic models with integrated HTML UIs:
1. **`BasicExample/`**: A simple Hello World demonstrating basic command execution.
2. **`BidirectionalExample/`**: Shows how to control simulation speed from JS and receive time/population updates from Java.
3. **`LogisticsDigitalTwin/`**: A complete Digital Twin dashboard using Leaflet maps and SheetJS, demonstrating real-time vehicle tracking and order dispatching.
4. **`SmartFactoryTwin/`**: An advanced 3D Digital Twin using React, Vite, and Three.js, featuring a glassmorphism dashboard, real-time metrics, and bidirectional control.