# AnyLogic Web Toolkit

![Version](https://img.shields.io/badge/version-1.0.0-blue.svg)
![Java](https://img.shields.io/badge/Java-17-orange.svg)
![AnyLogic](https://img.shields.io/badge/AnyLogic-8.9.9+-green.svg)
![OS](https://img.shields.io/badge/OS-Windows_x64-lightgrey.svg)

AnyLogic Web Toolkit is an open-source library that empowers developers to seamlessly embed modern web applications (HTML, CSS, JavaScript) directly into AnyLogic simulation models. It provides a robust, bidirectional bridge between your Java simulation logic and rich web frontends like Leaflet maps, React dashboards, or custom D3.js visualizations.

Powered by a native embedded Chromium engine (JCEF), it bypasses the limitations of the internal AnyLogic browser, giving you full control over DevTools, local filesystem access, and modern web standards.

## Features

- **Embedded Chromium Engine:** Fully isolated JCEF instance that runs natively within AnyLogic.
- **Bidirectional Communication:** Send commands from JS to Java (`AnyLogic.call(...)`) and push real-time events from Java to JS (`bridge.emit(...)`).
- **File System API:** Native OS file dialogs (Open/Save) exposed safely to JavaScript without browser sandbox restrictions.
- **Offline Capable:** Load local HTML files and assets seamlessly via the custom `http://webtoolkit/` protocol. No external web server required.
- **Developer Tools:** Press `F12` during simulation to open standard Chrome DevTools for rapid UI debugging.

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
│       ├── Bidirectional/     # Two-way communication example
│       └── Logistics/         # Logistics Digital Twin example
├── scripts/                   # Helper scripts for packaging and building
├── src/main/java/...          # Java source code for the Web Toolkit
├── build.gradle               # Gradle build script
└── README.md
```

## Documentation

- [Installation & Quick Start](docs/quick-start.md)
- [Agent Configuration Guide](docs/agent-configuration.md)
- [Communication (Java ↔ JS)](docs/communication.md)

## Included Examples

The `examples/AnyLogicModels/` directory contains ready-to-run AnyLogic models with integrated HTML UIs:
1. **`BasicExample/`**: A simple Hello World demonstrating basic command execution.
2. **`BidirectionalExample/`**: Shows how to control simulation speed from JS and receive time/population updates from Java.
3. **`LogisticsDigitalTwin/`**: A complete Digital Twin dashboard using Leaflet maps and SheetJS, demonstrating real-time vehicle tracking and order dispatching.