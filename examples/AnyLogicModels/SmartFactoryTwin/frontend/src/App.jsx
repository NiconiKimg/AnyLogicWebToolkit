import React, { useState, useEffect, useCallback } from 'react';
import Scene3D from './Scene3D';
import Dashboard from './Dashboard';

export default function App() {
  const [agvs, setAgvs] = useState([]);
  const [layout, setLayout] = useState(null);
  const [metrics, setMetrics] = useState({ activeOrders: 0, ordersCompleted: 0, history: [] });
  const [isLoading, setIsLoading] = useState(true);

  const [selectedAgvId, setSelectedAgvId] = useState(null);
  const [cameraMode, setCameraMode] = useState('orbit');

  const handleData = useCallback((data) => {
    if (!data) return;
    if (data.agvs) setAgvs(data.agvs);
    if (data.layout) setLayout(data.layout);

    if (data.metrics) {
      setMetrics(prev => {
        const now = new Date();
        const timeStr = `${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}:${now.getSeconds().toString().padStart(2, '0')}`;
        const newHistory = [...(prev.history || []), { time: timeStr, ordersCompleted: data.metrics.ordersCompleted || 0 }];
        if (newHistory.length > 20) newHistory.shift();
        return { ...data.metrics, history: newHistory };
      });
    }

    setIsLoading(false);
  }, []);

  useEffect(() => {
    window.updateSimulationData = handleData;

    if (window.AnyLogic && window.AnyLogic.events) {
      window.AnyLogic.events.on("updateSimulationData", handleData);
      window.AnyLogic.call("__ready__").catch(() => {});
    }

    return () => {
      delete window.updateSimulationData;
      if (window.AnyLogic && window.AnyLogic.events) {
        window.AnyLogic.events.off("updateSimulationData", handleData);
      }
    };
  }, [handleData]);

  const handleAddOrder = useCallback(() => {
    if (window.AnyLogic) {
      window.AnyLogic.call("addOrder", "newOrder").catch(console.error);
    }
  }, []);

  const handleToggleHalt = useCallback((agvId) => {
    if (window.AnyLogic) {
      window.AnyLogic.call("toggleAgvHalt", { agvId }).catch(console.error);
    }
  }, []);

  const handleSelectAgv = useCallback((id) => {
    setSelectedAgvId(id);
    if (id === null) setCameraMode('orbit');
  }, []);

  return (
    <>
      {isLoading && (
        <div className="loading-overlay">
          <div className="spinner"></div>
          <p>Connecting to AnyLogic Simulation...</p>
        </div>
      )}
      
      <Scene3D 
        agvs={agvs} 
        layout={layout} 
        selectedAgvId={selectedAgvId}
        onSelectAgv={handleSelectAgv}
        cameraMode={cameraMode}
      />
      
      <Dashboard 
        metrics={metrics} 
        agvs={agvs} 
        layout={layout}
        onAddOrder={handleAddOrder}
        selectedAgvId={selectedAgvId}
        onSelectAgv={handleSelectAgv}
        cameraMode={cameraMode}
        onChangeCameraMode={setCameraMode}
        onToggleHalt={handleToggleHalt}
      />
    </>
  );
}
