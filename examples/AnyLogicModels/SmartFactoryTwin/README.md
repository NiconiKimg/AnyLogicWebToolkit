# Smart Factory Digital Twin

## Prerequisites

- Node.js (v18+)
- AnyLogic 8.9+ with AnyLogic Web Toolkit installed

## How to Run

### 1. Build the Frontend
Before running the simulation in AnyLogic, compile the frontend:

```bash
cd frontend
npm install
npm run build
```

This compiles the React/Three.js frontend into `frontend/dist/`.

### 2. Run the AnyLogic Model
1. Open `SmartFactoryTwin.alp` in AnyLogic.
2. Run the `Simulation` experiment.
3. The embedded Chromium window will launch automatically with the 3D Digital Twin.
