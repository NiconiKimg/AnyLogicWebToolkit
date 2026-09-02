import React from 'react';
import { Box } from '@react-three/drei';

export default function FactoryEnvironment({ layout }) {
  if (!layout) return null;
  const racks = layout.racks || [];
  const docks = layout.docks || [];
  const walls = layout.walls || [];

  return (
    <group>
      {/* 1. Pallet Racks (Industrial Teardrop Storage Racks with Goods) */}
      {racks.map((pos, i) => {
        const rx = pos.x / 10;
        const rz = pos.y / 10;
        return (
          <group key={`rack-${i}`} position={[rx, 0, rz]}>
            {/* Safety Floor Border Line */}
            <mesh position={[0, 0.015, 0]} rotation={[-Math.PI / 2, 0, 0]}>
              <planeGeometry args={[2.4, 2.4]} />
              <meshBasicMaterial color="#3b82f6" transparent opacity={0.12} />
            </mesh>

            {/* 4 Vertical Steel Upright Columns (Industrial Blue) */}
            {[
              [-0.9, 1.8, -0.9],
              [0.9, 1.8, -0.9],
              [-0.9, 1.8, 0.9],
              [0.9, 1.8, 0.9]
            ].map((p, idx) => (
              <Box key={`col-${idx}`} args={[0.08, 3.6, 0.08]} position={p} castShadow>
                <meshStandardMaterial color="#0284c7" metalness={0.6} roughness={0.3} />
              </Box>
            ))}

            {/* Shelf Level 1 (Load Beams - Safety Orange) */}
            <Box args={[1.88, 0.08, 0.06]} position={[0, 0.8, 0.9]} castShadow>
              <meshStandardMaterial color="#ea580c" metalness={0.4} roughness={0.4} />
            </Box>
            <Box args={[1.88, 0.08, 0.06]} position={[0, 0.8, -0.9]} castShadow>
              <meshStandardMaterial color="#ea580c" metalness={0.4} roughness={0.4} />
            </Box>
            <Box args={[1.72, 0.04, 1.72]} position={[0, 0.84, 0]}>
              <meshStandardMaterial color="#475569" wireframe />
            </Box>

            {/* Shelf Level 2 */}
            <Box args={[1.88, 0.08, 0.06]} position={[0, 2.0, 0.9]} castShadow>
              <meshStandardMaterial color="#ea580c" metalness={0.4} roughness={0.4} />
            </Box>
            <Box args={[1.88, 0.08, 0.06]} position={[0, 2.0, -0.9]} castShadow>
              <meshStandardMaterial color="#ea580c" metalness={0.4} roughness={0.4} />
            </Box>
            <Box args={[1.72, 0.04, 1.72]} position={[0, 2.04, 0]}>
              <meshStandardMaterial color="#475569" wireframe />
            </Box>

            {/* Top Shelf Cross Beams */}
            <Box args={[1.88, 0.08, 0.06]} position={[0, 3.2, 0.9]}>
              <meshStandardMaterial color="#0284c7" metalness={0.5} roughness={0.4} />
            </Box>
            <Box args={[1.88, 0.08, 0.06]} position={[0, 3.2, -0.9]}>
              <meshStandardMaterial color="#0284c7" metalness={0.5} roughness={0.4} />
            </Box>

            {/* Stored Palletized Goods on Level 1 */}
            <group position={[0, 0.86, 0]}>
              <Box args={[1.4, 0.05, 1.4]} position={[0, 0.025, 0]} castShadow>
                <meshStandardMaterial color="#92400e" roughness={0.9} />
              </Box>
              <Box args={[1.2, 0.8, 1.2]} position={[0, 0.45, 0]} castShadow receiveShadow>
                <meshStandardMaterial color="#ca8a04" metalness={0.1} roughness={0.6} />
              </Box>
            </group>

            {/* Stored Palletized Goods on Level 2 */}
            <group position={[0, 2.06, 0]}>
              <Box args={[1.4, 0.05, 1.4]} position={[0, 0.025, 0]} castShadow>
                <meshStandardMaterial color="#92400e" roughness={0.9} />
              </Box>
              <Box args={[1.1, 0.7, 1.1]} position={[0, 0.4, 0]} castShadow receiveShadow>
                <meshStandardMaterial color="#0284c7" metalness={0.2} roughness={0.5} />
              </Box>
            </group>
          </group>
        );
      })}

      {/* 2. Loading Docks (Rollup Doors, Leveler Plates & Safety Bollards) */}
      {docks.map((pos, i) => {
        const dx = pos.x / 10;
        const dz = pos.y / 10;
        return (
          <group key={`dock-${i}`} position={[dx, 0, dz]}>
            {/* Dock Floor Area */}
            <mesh position={[0, 0.015, 0]} rotation={[-Math.PI / 2, 0, 0]}>
              <planeGeometry args={[3.2, 3.2]} />
              <meshBasicMaterial color="#10b981" transparent opacity={0.15} />
            </mesh>

            {/* Diamond Tread Steel Leveler Plate */}
            <Box args={[2.4, 0.08, 2.4]} position={[0, 0.04, 0]} receiveShadow>
              <meshStandardMaterial color="#334155" metalness={0.8} roughness={0.3} />
            </Box>

            {/* Industrial Rollup Door Frame */}
            <group position={[0, 2.0, -1.2]}>
              <Box args={[2.8, 4.0, 0.1]} position={[0, 0, 0]} castShadow>
                <meshStandardMaterial color="#1e293b" metalness={0.7} roughness={0.4} />
              </Box>
              {/* Door Slats / Segments */}
              {[-1.2, -0.6, 0, 0.6, 1.2].map((sy, sIdx) => (
                <Box key={`slat-${sIdx}`} args={[2.6, 0.04, 0.12]} position={[0, sy, 0.02]}>
                  <meshStandardMaterial color="#0f172a" />
                </Box>
              ))}
              {/* Overhead Dock Status Light */}
              <mesh position={[0, 2.15, 0.15]}>
                <sphereGeometry args={[0.12, 16, 16]} />
                <meshStandardMaterial color="#10b981" emissive="#10b981" emissiveIntensity={3} />
              </mesh>
            </group>

            {/* Safety Yellow Bollards */}
            {[-1.3, 1.3].map((bx, bIdx) => (
              <group key={`bollard-${bIdx}`} position={[bx, 0.45, 1.2]}>
                <mesh castShadow>
                  <cylinderGeometry args={[0.08, 0.08, 0.9, 16]} />
                  <meshStandardMaterial color="#eab308" metalness={0.3} roughness={0.3} />
                </mesh>
                {/* Black Hazard Stripe */}
                <mesh position={[0, 0.15, 0]}>
                  <cylinderGeometry args={[0.082, 0.082, 0.18, 16]} />
                  <meshStandardMaterial color="#0f172a" />
                </mesh>
              </group>
            ))}
          </group>
        );
      })}

      {/* 3. Inductive Charging Pad with Power Totem */}
      <group position={[25, 0, 47]}>
        {/* Floor Staging Mat */}
        <mesh position={[0, 0.015, 0]} rotation={[-Math.PI / 2, 0, 0]}>
          <planeGeometry args={[11, 5]} />
          <meshBasicMaterial color="#eab308" transparent opacity={0.15} />
        </mesh>
        {/* Charging Contact Plate */}
        <Box args={[9.5, 0.05, 3.5]} position={[0, 0.03, 0]}>
          <meshStandardMaterial color="#1e293b" metalness={0.6} roughness={0.4} />
        </Box>
        {/* Glowing Contact Coils */}
        {[-3, 0, 3].map((cx, cIdx) => (
          <mesh key={`coil-${cIdx}`} position={[cx, 0.06, 0]} rotation={[-Math.PI / 2, 0, 0]}>
            <ringGeometry args={[0.4, 0.7, 24]} />
            <meshBasicMaterial color="#eab308" />
          </mesh>
        ))}
        {/* Charging Station Pillar */}
        <group position={[5.2, 1.2, 0]}>
          <Box args={[0.3, 2.4, 0.8]} castShadow>
            <meshStandardMaterial color="#0f172a" metalness={0.7} roughness={0.3} />
          </Box>
          <Box args={[0.32, 0.2, 0.6]} position={[0, 0.6, 0]}>
            <meshStandardMaterial color="#10b981" emissive="#10b981" emissiveIntensity={2} />
          </Box>
        </group>
      </group>

      {/* 4. Industrial Warehouse Walls */}
      {walls.map((wall, i) => {
        const w = wall.width / 10;
        const h = wall.height / 10;
        const cx = (wall.x / 10) + (w / 2);
        const cz = (wall.y / 10) + (h / 2);
        const rotY = -wall.rotation;
        
        return (
          <group key={`wall-${i}`}>
            {/* Concrete Main Wall */}
            <Box args={[w, 2.8, h]} position={[cx, 1.4, cz]} rotation={[0, rotY, 0]} castShadow receiveShadow>
              <meshStandardMaterial color="#334155" metalness={0.2} roughness={0.8} />
            </Box>
            {/* Safety Yellow Baseboard Trim */}
            <Box args={[w + 0.04, 0.15, h + 0.04]} position={[cx, 0.08, cz]} rotation={[0, rotY, 0]}>
              <meshStandardMaterial color="#eab308" metalness={0.3} roughness={0.5} />
            </Box>
          </group>
        );
      })}
    </group>
  );
}
