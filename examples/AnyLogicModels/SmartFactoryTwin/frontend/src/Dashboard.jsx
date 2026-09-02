import React from 'react';
import { AreaChart, Area, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid } from 'recharts';
import { Activity, Battery, Box, Zap, Truck, Video, Eye, X, Crosshair, PauseCircle } from 'lucide-react';

export default function Dashboard({ 
  metrics, 
  agvs, 
  layout,
  onAddOrder,
  selectedAgvId,
  onSelectAgv,
  cameraMode,
  onChangeCameraMode,
  onToggleHalt
}) {
  const history = metrics?.history || [];

  const avgBattery = agvs.length > 0 
    ? Math.round(agvs.reduce((sum, agv) => sum + (agv.battery || 100), 0) / agvs.length)
    : 100;
    
  const activeAgvs = agvs.filter(a => a.status !== 'IDLE').length;
  const utilization = agvs.length > 0 ? Math.round((activeAgvs / agvs.length) * 100) : 0;

  const selectedAgv = agvs.find(a => a.id === selectedAgvId);

  return (
    <div className="ui-overlay" onPointerDown={(e) => e.stopPropagation()}>
      <div className="panel-left">
        <div className="glass-panel dashboard-card" style={{ flexShrink: 0 }}>
          <div className="dashboard-header">
            <Activity className="text-accent" size={24} />
            <h2 className="dashboard-title">Smart Factory Control</h2>
          </div>
          
          <div className="stat-grid">
            <div className="stat-box">
              <div className="stat-value">{utilization}%</div>
              <div className="stat-label">Fleet Utilization</div>
            </div>
            <div className="stat-box">
              <div className="stat-value">{metrics?.activeOrders || 0}</div>
              <div className="stat-label">Active Orders</div>
            </div>
          </div>

          <button className="action-btn" onClick={onAddOrder}>
            <Box size={18} />
            Dispatch New Order
          </button>
        </div>

        {/* Selected AGV Telemetry & Camera Controls */}
        {selectedAgv && (
          <div className="glass-panel dashboard-card" style={{ flexShrink: 0, border: '1px solid #38bdf8' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', fontWeight: '700', color: '#38bdf8' }}>
                <Crosshair size={18} />
                <span>AGV #{selectedAgv.id} Telemetry</span>
              </div>
              <button 
                style={{ background: 'transparent', border: 'none', color: '#94a3b8', cursor: 'pointer' }}
                onClick={() => onSelectAgv(null)}
              >
                <X size={16} />
              </button>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px', marginBottom: '12px', fontSize: '0.85rem' }}>
              <div className="stat-box" style={{ padding: '8px' }}>
                <div className="stat-label">Battery</div>
                <div style={{ fontWeight: '700', color: selectedAgv.battery < 30 ? '#ef4444' : '#10b981' }}>
                  {Math.round(selectedAgv.battery)}%
                </div>
              </div>
              <div className="stat-box" style={{ padding: '8px' }}>
                <div className="stat-label">Status</div>
                <div style={{ fontWeight: '700', color: selectedAgv.status === 'IDLE' ? '#94a3b8' : '#eab308' }}>
                  {selectedAgv.status}
                </div>
              </div>
            </div>

            {/* Camera View Switcher */}
            <div style={{ fontSize: '0.75rem', color: '#94a3b8', marginBottom: '6px', fontWeight: '600' }}>
              CAMERA PERSPECTIVE
            </div>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '6px', marginBottom: '10px' }}>
              <button 
                style={{
                  background: cameraMode === 'orbit' ? '#0284c7' : 'rgba(0,0,0,0.3)',
                  border: '1px solid rgba(255,255,255,0.1)',
                  borderRadius: '6px',
                  color: '#fff',
                  padding: '6px 4px',
                  fontSize: '0.75rem',
                  cursor: 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  gap: '4px'
                }}
                onClick={() => onChangeCameraMode('orbit')}
              >
                <Eye size={14} /> Orbit
              </button>

              <button 
                style={{
                  background: cameraMode === 'chase' ? '#0284c7' : 'rgba(0,0,0,0.3)',
                  border: '1px solid rgba(255,255,255,0.1)',
                  borderRadius: '6px',
                  color: '#fff',
                  padding: '6px 4px',
                  fontSize: '0.75rem',
                  cursor: 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  gap: '4px'
                }}
                onClick={() => onChangeCameraMode('chase')}
              >
                <Video size={14} /> Chase
              </button>

              <button 
                style={{
                  background: cameraMode === 'fpv' ? '#0284c7' : 'rgba(0,0,0,0.3)',
                  border: '1px solid rgba(255,255,255,0.1)',
                  borderRadius: '6px',
                  color: '#fff',
                  padding: '6px 4px',
                  fontSize: '0.75rem',
                  cursor: 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  gap: '4px'
                }}
                onClick={() => onChangeCameraMode('fpv')}
              >
                <Crosshair size={14} /> FPV
              </button>
            </div>

            <button 
              style={{
                width: '100%',
                background: 'rgba(239, 68, 68, 0.2)',
                border: '1px solid rgba(239, 68, 68, 0.4)',
                borderRadius: '6px',
                color: '#ef4444',
                padding: '8px',
                fontSize: '0.8rem',
                fontWeight: '600',
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                gap: '6px'
              }}
              onClick={() => onToggleHalt(selectedAgv.id)}
            >
              <PauseCircle size={16} /> Emergency Stop AGV
            </button>
          </div>
        )}

        {/* Individual AGVs List */}
        <div className="glass-panel dashboard-card" style={{ flex: 1, overflowY: 'auto', display: 'flex', flexDirection: 'column', gap: '10px' }}>
          <div className="dashboard-header" style={{ marginBottom: '8px', paddingBottom: '8px' }}>
            <Truck className="text-accent" size={20} />
            <h3 className="dashboard-title" style={{ fontSize: '1rem' }}>Fleet Status</h3>
          </div>
          
          {agvs.map(agv => {
            const isSelected = agv.id === selectedAgvId;
            return (
              <div 
                key={agv.id} 
                style={{ 
                  background: isSelected ? 'rgba(56, 189, 248, 0.15)' : 'rgba(0,0,0,0.2)', 
                  padding: '10px', 
                  borderRadius: '8px', 
                  border: isSelected ? '1px solid #38bdf8' : '1px solid rgba(255,255,255,0.05)',
                  cursor: 'pointer',
                  transition: 'all 0.2s ease'
                }}
                onClick={() => onSelectAgv(agv.id)}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '6px' }}>
                  <span style={{ fontWeight: '600', fontSize: '0.9rem', color: isSelected ? '#38bdf8' : '#fff' }}>
                    AGV #{agv.id}
                  </span>
                  <span style={{ fontSize: '0.8rem', color: agv.battery < 30 ? '#ef4444' : '#10b981' }}>
                    {Math.round(agv.battery)}%
                  </span>
                </div>
                <div style={{ width: '100%', background: 'rgba(255,255,255,0.1)', height: '4px', borderRadius: '2px', overflow: 'hidden' }}>
                  <div style={{ width: `${agv.battery}%`, background: agv.battery < 30 ? '#ef4444' : '#10b981', height: '100%' }} />
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '6px' }}>
                  <span style={{ color: agv.status === 'IDLE' ? '#94a3b8' : '#eab308' }}>{agv.status || 'IDLE'}</span>
                  <span>Pos: ({Math.round(agv.x)}, {Math.round(agv.y)})</span>
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* Right Panel - Analytics */}
      <div className="panel-right">
        <div className="glass-panel dashboard-card">
          <div className="dashboard-header">
            <Zap className="text-accent" size={24} />
            <h2 className="dashboard-title">System Analytics</h2>
          </div>

          <div className="stat-grid" style={{ gridTemplateColumns: '1fr' }}>
            <div className="stat-box" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <div style={{ textAlign: 'left' }}>
                <div className="stat-label">Fleet Avg Battery</div>
                <div className="stat-value" style={{ color: avgBattery < 30 ? '#ef4444' : '#10b981' }}>
                  {avgBattery}%
                </div>
              </div>
              <Battery size={32} color={avgBattery < 30 ? '#ef4444' : '#10b981'} />
            </div>
          </div>

          <h3 className="stat-label" style={{ marginTop: '20px', marginBottom: '10px' }}>Orders Completed</h3>
          <div className="chart-container">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={history}>
                <defs>
                  <linearGradient id="colorOrders" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#10b981" stopOpacity={0.8}/>
                    <stop offset="95%" stopColor="#10b981" stopOpacity={0}/>
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.1)" vertical={false} />
                <XAxis dataKey="time" stroke="#94a3b8" fontSize={12} tickLine={false} axisLine={false} />
                <YAxis stroke="#94a3b8" fontSize={12} tickLine={false} axisLine={false} />
                <Tooltip 
                  contentStyle={{ backgroundColor: 'rgba(30, 41, 59, 0.9)', border: '1px solid rgba(255,255,255,0.1)', borderRadius: '8px' }}
                  itemStyle={{ color: '#f8fafc' }}
                />
                <Area type="monotone" dataKey="ordersCompleted" stroke="#10b981" strokeWidth={3} fillOpacity={1} fill="url(#colorOrders)" isAnimationActive={false} />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>
      </div>
    </div>
  );
}
