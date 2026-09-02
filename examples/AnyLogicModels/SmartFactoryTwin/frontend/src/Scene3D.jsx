import React, { useRef } from 'react';
import { Canvas, useFrame, useThree } from '@react-three/fiber';
import { OrbitControls } from '@react-three/drei';
import * as THREE from 'three';
import AGVModel from './components/3d/AGVModel';
import FactoryEnvironment from './components/3d/FactoryEnvironment';

function CameraController({ selectedAgv, cameraMode, controlsRef }) {
  const { camera } = useThree();
  const smoothPos = useRef(new THREE.Vector3());
  const smoothAngle = useRef(0);
  const lookTarget = useRef(new THREE.Vector3());
  const isInitialized = useRef(false);

  useFrame((state, delta) => {
    if (!selectedAgv || cameraMode === 'orbit') {
      if (controlsRef.current) controlsRef.current.enabled = true;
      isInitialized.current = false;
      return;
    }

    if (controlsRef.current) controlsRef.current.enabled = false;

    const targetX = selectedAgv.x / 10;
    const targetZ = selectedAgv.y / 10;
    const targetAngle = -selectedAgv.rotation + Math.PI / 2;

    if (!isInitialized.current) {
      smoothPos.current.set(targetX, 0, targetZ);
      smoothAngle.current = targetAngle;
      camera.position.set(targetX - Math.sin(targetAngle) * 6, 4.0, targetZ - Math.cos(targetAngle) * 6);
      lookTarget.current.set(targetX, 1.0, targetZ);
      isInitialized.current = true;
    }

    const posFactor = Math.min(1, delta * 15);
    smoothPos.current.x = THREE.MathUtils.lerp(smoothPos.current.x, targetX, posFactor);
    smoothPos.current.z = THREE.MathUtils.lerp(smoothPos.current.z, targetZ, posFactor);

    let diff = targetAngle - smoothAngle.current;
    diff = Math.atan2(Math.sin(diff), Math.cos(diff));
    smoothAngle.current += diff * Math.min(1, delta * 12);

    const fwdX = Math.sin(smoothAngle.current);
    const fwdZ = Math.cos(smoothAngle.current);

    if (cameraMode === 'chase') {
      const targetCamX = smoothPos.current.x - fwdX * 6;
      const targetCamY = 4.0;
      const targetCamZ = smoothPos.current.z - fwdZ * 6;

      const camFactor = Math.min(1, delta * 8);
      camera.position.x = THREE.MathUtils.lerp(camera.position.x, targetCamX, camFactor);
      camera.position.y = THREE.MathUtils.lerp(camera.position.y, targetCamY, camFactor);
      camera.position.z = THREE.MathUtils.lerp(camera.position.z, targetCamZ, camFactor);

      const destLookX = smoothPos.current.x + fwdX * 3;
      const destLookZ = smoothPos.current.z + fwdZ * 3;
      lookTarget.current.x = THREE.MathUtils.lerp(lookTarget.current.x, destLookX, camFactor);
      lookTarget.current.y = 1.0;
      lookTarget.current.z = THREE.MathUtils.lerp(lookTarget.current.z, destLookZ, camFactor);
      camera.lookAt(lookTarget.current);
    } else if (cameraMode === 'fpv') {
      const targetCamX = smoothPos.current.x + fwdX * 0.7;
      const targetCamY = 1.3;
      const targetCamZ = smoothPos.current.z + fwdZ * 0.7;

      const camFactor = Math.min(1, delta * 15);
      camera.position.x = THREE.MathUtils.lerp(camera.position.x, targetCamX, camFactor);
      camera.position.y = targetCamY;
      camera.position.z = THREE.MathUtils.lerp(camera.position.z, targetCamZ, camFactor);

      const destLookX = smoothPos.current.x + fwdX * 8;
      const destLookZ = smoothPos.current.z + fwdZ * 8;
      lookTarget.current.x = THREE.MathUtils.lerp(lookTarget.current.x, destLookX, camFactor);
      lookTarget.current.y = 1.0;
      lookTarget.current.z = THREE.MathUtils.lerp(lookTarget.current.z, destLookZ, camFactor);
      camera.lookAt(lookTarget.current);
    }
  });

  return null;
}

export default function Scene3D({ 
  agvs, 
  layout, 
  selectedAgvId, 
  onSelectAgv, 
  cameraMode 
}) {
  const controlsRef = useRef();
  const selectedAgv = agvs.find(a => a.id === selectedAgvId);

  return (
    <Canvas 
      shadows={{ type: THREE.PCFShadowMap }}
      camera={{ position: [55, 45, 55], fov: 45 }}
      onPointerMissed={(e) => {
        if (e && e.target && e.target.tagName !== 'CANVAS') return;
        onSelectAgv(null);
      }}
    >
      <color attach="background" args={['#090d16']} />
      
      {/* Cinematic Studio Lighting */}
      <ambientLight intensity={0.7} />
      <directionalLight 
        position={[30, 60, 30]} 
        intensity={1.8} 
        castShadow 
        shadow-mapSize={[2048, 2048]}
        shadow-camera-left={-60}
        shadow-camera-right={60}
        shadow-camera-top={60}
        shadow-camera-bottom={-60}
      />
      <directionalLight position={[-30, 30, -30]} intensity={0.4} color="#38bdf8" />
      
      {/* Ground Grid */}
      <gridHelper args={[240, 60, '#334155', '#1e293b']} position={[0, 0.01, 0]} />

      {/* Warehouse Infrastructure */}
      <FactoryEnvironment layout={layout} />
      
      {/* Fleet of AGVs */}
      {agvs.map(agv => (
        <AGVModel 
          key={agv.id} 
          data={agv} 
          isSelected={agv.id === selectedAgvId}
          onSelect={onSelectAgv}
        />
      ))}

      <CameraController 
        selectedAgv={selectedAgv} 
        cameraMode={cameraMode} 
        controlsRef={controlsRef} 
      />

      <OrbitControls 
        ref={controlsRef} 
        maxPolarAngle={Math.PI / 2.05} 
        minDistance={5} 
        maxDistance={120} 
        target={[25, 0, 25]}
        dampingFactor={0.05}
        enableDamping
      />
    </Canvas>
  );
}
