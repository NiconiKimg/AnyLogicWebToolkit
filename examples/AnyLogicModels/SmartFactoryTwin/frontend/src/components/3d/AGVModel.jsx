import React, { useRef } from 'react';
import { useFrame } from '@react-three/fiber';
import { Box } from '@react-three/drei';
import * as THREE from 'three';

export default function AGVModel({ data, isSelected, onSelect }) {
  const meshRef = useRef();
  const trailRef = useRef([]);

  useFrame((state, delta) => {
    if (meshRef.current) {
      const targetX = data.x / 10;
      const targetZ = data.y / 10;
      
      const posFactor = Math.min(1, delta * 15);
      meshRef.current.position.x = THREE.MathUtils.lerp(meshRef.current.position.x, targetX, posFactor);
      meshRef.current.position.z = THREE.MathUtils.lerp(meshRef.current.position.z, targetZ, posFactor);
      
      if (data.rotation !== undefined) {
        const targetAngle = -data.rotation + Math.PI / 2;
        let diff = targetAngle - meshRef.current.rotation.y;
        diff = Math.atan2(Math.sin(diff), Math.cos(diff));
        meshRef.current.rotation.y += diff * Math.min(1, delta * 12);
      }

      const curr = meshRef.current.position;
      const last = trailRef.current[trailRef.current.length - 1];
      if (!last || Math.hypot(curr.x - last.x, curr.z - last.z) > 0.4) {
        trailRef.current.push({ x: curr.x, z: curr.z, id: Math.random() });
        if (trailRef.current.length > 10) trailRef.current.shift();
      }
    }
  });

  const isHalted = data.status === 'HALTED';
  const isDelivering = data.status === 'DELIVERING';
  const isPickup = data.status === 'PICKUP';

  const statusColor = isHalted 
    ? '#ef4444' 
    : isDelivering 
      ? '#f59e0b' 
      : isPickup 
        ? '#06b6d4' 
        : '#10b981';

  return (
    <>
      {trailRef.current.map((pt, idx) => {
        const opacity = ((idx + 1) / trailRef.current.length) * 0.25;
        return (
          <mesh key={pt.id} position={[pt.x, 0.025, pt.z]} rotation={[-Math.PI / 2, 0, 0]}>
            <circleGeometry args={[0.35, 16]} />
            <meshBasicMaterial color="#38bdf8" transparent opacity={opacity} />
          </mesh>
        );
      })}

      <group 
        ref={meshRef} 
        position={[0, 0, 0]}
        onClick={(e) => { e.stopPropagation(); onSelect(data.id); }}
        onPointerOver={(e) => { e.stopPropagation(); document.body.style.cursor = 'pointer'; }}
        onPointerOut={() => { document.body.style.cursor = 'default'; }}
      >
        {/* Holographic Selection Reticle */}
        {isSelected && (
          <group position={[0, 0.03, 0]}>
            <mesh rotation={[-Math.PI / 2, 0, 0]}>
              <ringGeometry args={[1.3, 1.45, 32]} />
              <meshBasicMaterial color="#38bdf8" transparent opacity={0.8} />
            </mesh>
            <mesh rotation={[-Math.PI / 2, 0, 0]}>
              <ringGeometry args={[1.6, 1.65, 32]} />
              <meshBasicMaterial color="#0284c7" transparent opacity={0.4} />
            </mesh>
          </group>
        )}

        {/* 1. Heavy Lower Bumper Skirt (Safety Black) */}
        <Box args={[1.36, 0.14, 1.86]} position={[0, 0.12, 0]} castShadow receiveShadow>
          <meshStandardMaterial color="#0f172a" roughness={0.8} metalness={0.2} />
        </Box>

        {/* 2. Diagonal Safety LiDAR Scanners */}
        <group position={[0.62, 0.16, 0.82]}>
          <mesh castShadow>
            <cylinderGeometry args={[0.07, 0.07, 0.12, 16]} />
            <meshStandardMaterial color="#18181b" roughness={0.3} metalness={0.8} />
          </mesh>
          <mesh position={[0, 0.02, 0]}>
            <cylinderGeometry args={[0.075, 0.075, 0.03, 16]} />
            <meshBasicMaterial color="#06b6d4" />
          </mesh>
        </group>
        <group position={[-0.62, 0.16, -0.82]}>
          <mesh castShadow>
            <cylinderGeometry args={[0.07, 0.07, 0.12, 16]} />
            <meshStandardMaterial color="#18181b" roughness={0.3} metalness={0.8} />
          </mesh>
          <mesh position={[0, 0.02, 0]}>
            <cylinderGeometry args={[0.075, 0.075, 0.03, 16]} />
            <meshBasicMaterial color="#06b6d4" />
          </mesh>
        </group>

        {/* 3. Main Upper Chassis Body Shell */}
        <Box args={[1.3, 0.28, 1.76]} position={[0, 0.32, 0]} castShadow receiveShadow>
          <meshStandardMaterial 
            color={isSelected ? '#0284c7' : '#f1f5f9'} 
            metalness={0.3} 
            roughness={0.25} 
          />
        </Box>

        {/* 4. Contrast Side Accent Panels */}
        <Box args={[1.32, 0.16, 1.2]} position={[0, 0.32, 0]}>
          <meshStandardMaterial color="#1e293b" metalness={0.4} roughness={0.5} />
        </Box>

        {/* 5. 360-degree LED Status Light Ribbon */}
        <Box args={[1.24, 0.04, 0.02]} position={[0, 0.23, 0.89]}>
          <meshStandardMaterial color={statusColor} emissive={statusColor} emissiveIntensity={2} />
        </Box>
        <Box args={[1.24, 0.04, 0.02]} position={[0, 0.23, -0.89]}>
          <meshStandardMaterial color={statusColor} emissive={statusColor} emissiveIntensity={2} />
        </Box>
        <Box args={[0.02, 0.04, 1.7]} position={[-0.66, 0.23, 0]}>
          <meshStandardMaterial color={statusColor} emissive={statusColor} emissiveIntensity={2} />
        </Box>
        <Box args={[0.02, 0.04, 1.7]} position={[0.66, 0.23, 0]}>
          <meshStandardMaterial color={statusColor} emissive={statusColor} emissiveIntensity={2} />
        </Box>

        {/* 6. Twin LED Headlights & Taillights */}
        <Box args={[0.16, 0.06, 0.03]} position={[0.4, 0.36, 0.89]}>
          <meshStandardMaterial color="#ffffff" emissive="#e0f2fe" emissiveIntensity={2.5} />
        </Box>
        <Box args={[0.16, 0.06, 0.03]} position={[-0.4, 0.36, 0.89]}>
          <meshStandardMaterial color="#ffffff" emissive="#e0f2fe" emissiveIntensity={2.5} />
        </Box>
        <Box args={[0.16, 0.06, 0.03]} position={[0.4, 0.36, -0.89]}>
          <meshStandardMaterial color="#dc2626" emissive="#ef4444" emissiveIntensity={2} />
        </Box>
        <Box args={[0.16, 0.06, 0.03]} position={[-0.4, 0.36, -0.89]}>
          <meshStandardMaterial color="#dc2626" emissive="#ef4444" emissiveIntensity={2} />
        </Box>

        {/* 7. Industrial Heavy-Duty Wheels (4x) */}
        {[
          [0.68, 0.16, 0.55],
          [-0.68, 0.16, 0.55],
          [0.68, 0.16, -0.55],
          [-0.68, 0.16, -0.55]
        ].map((pos, i) => (
          <group key={`wheel-${i}`} position={pos} rotation={[0, 0, Math.PI / 2]}>
            <mesh castShadow>
              <cylinderGeometry args={[0.16, 0.16, 0.1, 24]} />
              <meshStandardMaterial color="#020617" roughness={0.8} />
            </mesh>
            <mesh position={[0, 0.02, 0]}>
              <cylinderGeometry args={[0.09, 0.09, 0.11, 16]} />
              <meshStandardMaterial color="#64748b" metalness={0.8} roughness={0.2} />
            </mesh>
          </group>
        ))}

        {/* 8. Top Load Turntable / Hydraulic Deck Plate */}
        <Box args={[1.12, 0.04, 1.45]} position={[0, 0.48, 0]} castShadow receiveShadow>
          <meshStandardMaterial color="#334155" metalness={0.7} roughness={0.3} />
        </Box>
        <Box args={[0.9, 0.01, 0.2]} position={[0, 0.505, 0.45]}>
          <meshStandardMaterial color="#0f172a" />
        </Box>
        <Box args={[0.9, 0.01, 0.2]} position={[0, 0.505, -0.45]}>
          <meshStandardMaterial color="#0f172a" />
        </Box>

        {/* Emergency Stop Mushroom Button on Deck */}
        <group position={[0.46, 0.50, -0.55]}>
          <mesh>
            <cylinderGeometry args={[0.04, 0.04, 0.03, 16]} />
            <meshStandardMaterial color="#eab308" />
          </mesh>
          <mesh position={[0, 0.02, 0]}>
            <cylinderGeometry args={[0.035, 0.035, 0.025, 16]} />
            <meshStandardMaterial color="#dc2626" roughness={0.3} />
          </mesh>
        </group>

        {/* 9. Realistic Industrial Cargo (Pallet + Container Box) without Z-fighting */}
        {isDelivering && (
          <group position={[0, 0.51, 0]}>
            {/* Wooden Pallet Base */}
            <group position={[0, 0.04, 0]}>
              {/* Top Wood Planks */}
              <Box args={[1.05, 0.03, 1.35]} position={[0, 0.04, 0]} castShadow>
                <meshStandardMaterial color="#92400e" roughness={0.9} />
              </Box>
              {/* Stringer Runner Blocks */}
              <Box args={[0.12, 0.05, 1.35]} position={[-0.45, 0, 0]} castShadow>
                <meshStandardMaterial color="#78350f" roughness={0.9} />
              </Box>
              <Box args={[0.12, 0.05, 1.35]} position={[0, 0, 0]} castShadow>
                <meshStandardMaterial color="#78350f" roughness={0.9} />
              </Box>
              <Box args={[0.12, 0.05, 1.35]} position={[0.45, 0, 0]} castShadow>
                <meshStandardMaterial color="#78350f" roughness={0.9} />
              </Box>
            </group>

            {/* Industrial Container Crate */}
            <group position={[0, 0.51, 0]}>
              {/* Main Crate Body */}
              <Box args={[0.94, 0.82, 1.22]} castShadow receiveShadow>
                <meshStandardMaterial color="#d97706" metalness={0.2} roughness={0.4} />
              </Box>
              {/* Crate Top Lid */}
              <Box args={[0.98, 0.06, 1.26]} position={[0, 0.44, 0]} castShadow>
                <meshStandardMaterial color="#b45309" roughness={0.5} />
              </Box>
              {/* Reinforcing Corner Posts with clear separation */}
              {[
                [-0.47, 0, -0.61],
                [0.47, 0, -0.61],
                [-0.47, 0, 0.61],
                [0.47, 0, 0.61]
              ].map((pos, i) => (
                <Box key={`post-${i}`} args={[0.06, 0.82, 0.06]} position={pos} castShadow>
                  <meshStandardMaterial color="#334155" metalness={0.7} roughness={0.3} />
                </Box>
              ))}
              {/* Perimeter Protective Bands (Z-fighting free: explicitly wider than crate) */}
              <Box args={[0.96, 0.05, 1.24]} position={[0, 0.15, 0]}>
                <meshStandardMaterial color="#1e293b" metalness={0.5} roughness={0.6} />
              </Box>
              <Box args={[0.96, 0.05, 1.24]} position={[0, -0.15, 0]}>
                <meshStandardMaterial color="#1e293b" metalness={0.5} roughness={0.6} />
              </Box>
            </group>
          </group>
        )}
      </group>
    </>
  );
}
