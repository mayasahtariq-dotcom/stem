"use client";
import { OrbitControls } from "@react-three/drei";
import { useThree } from "@react-three/fiber";
import { useEffect } from "react";
import { CAMERA, ORBIT_LIMITS } from "../config/camera";
import { LIGHTING } from "../config/lighting";
import { preloadFarmAssets } from "../assets/FarmModel";
import type { QualitySettings } from "../engine/quality";
import { Interactable } from "./Interactable";
import { Barn, CropField, Fence, HayBale, Tree, Windmill } from "./objects";
import { Terrain } from "./Terrain";
import { FIELD, POND, terrainHeight } from "./terrain-math";

/** Places on the farm a player can tap. Their meaning is defined by later phases. */
export const FARM_PLACES = ["barn", "field", "windmill", "pond"] as const;
export type FarmPlace = (typeof FARM_PLACES)[number];

export interface FarmWorldProps {
  quality: QualitySettings;
  selected?: FarmPlace | null;
  onSelect?: (place: FarmPlace) => void;
  reducedMotion?: boolean;
  onAssetError?: (error: unknown) => void;
  /** Off in tests (no DOM element for OrbitControls). */
  controls?: boolean;
}

const TREES: [number, number, number][] = [
  [-20, -14, 1.1], [-24, -4, 0.9], [-18, 22, 1.2], [22, 16, 1], [26, 4, 1.3],
  [-6, -24, 1], [6, -26, 1.15], [30, -18, 0.95], [-30, 12, 1.05], [16, 28, 1.1],
];

/** Pulls the camera back on portrait screens so the whole farmstead fits a phone. */
function PortraitFit() {
  const camera = useThree((s) => s.camera);
  const aspect = useThree((s) => s.size.width / Math.max(1, s.size.height));
  useEffect(() => {
    const k = aspect < 1 ? Math.min(1.9, 1 / aspect) : 1;
    camera.position.set(CAMERA.position[0] * k, CAMERA.position[1] * k, CAMERA.position[2] * k);
    camera.updateProjectionMatrix();
  }, [aspect, camera]);
  return null;
}

/**
 * Phase 1 proof-of-concept farm: real terrain plus a few farm objects, lit and
 * shadowed, with orbit/zoom and tap selection. Not the game world itself.
 */
export function FarmWorld({ quality, selected, onSelect, reducedMotion, onAssetError, controls = true }: FarmWorldProps) {
  useEffect(() => preloadFarmAssets(), []);
  const select = (id: string) => onSelect?.(id as FarmPlace);
  const segments = quality.tier === "low" ? 64 : quality.tier === "medium" ? 96 : 128;
  const treeCount = Math.max(4, Math.round(TREES.length * quality.detail));

  return (
    <>
      <hemisphereLight args={[LIGHTING.hemisphere.sky, LIGHTING.hemisphere.ground, LIGHTING.hemisphere.intensity]} />
      <directionalLight
        color={LIGHTING.sun.color}
        intensity={LIGHTING.sun.intensity}
        position={LIGHTING.sun.position}
        castShadow={quality.shadows}
        shadow-mapSize={[quality.shadowMapSize, quality.shadowMapSize]}
        shadow-bias={LIGHTING.sun.shadowBias}
        shadow-normalBias={LIGHTING.sun.shadowNormalBias}
        shadow-camera-left={-LIGHTING.sun.shadowBounds}
        shadow-camera-right={LIGHTING.sun.shadowBounds}
        shadow-camera-top={LIGHTING.sun.shadowBounds}
        shadow-camera-bottom={-LIGHTING.sun.shadowBounds}
      />

      <Terrain segments={segments} receiveShadow={quality.shadows} />

      <Interactable id="barn" position={[-2, 0, -6]} selected={selected === "barn"} onSelect={select} reducedMotion={reducedMotion}>
        <Barn />
      </Interactable>
      <Interactable id="windmill" position={[-11, 0, -9]} rotationY={0.5} selected={selected === "windmill"} onSelect={select} reducedMotion={reducedMotion}>
        <Windmill reducedMotion={reducedMotion} />
      </Interactable>
      <Interactable id="field" selected={selected === "field"} onSelect={select} reducedMotion={reducedMotion}>
        <group position={[0, 0, 0]}>
          <CropField detail={quality.detail} />
          <Fence from={[FIELD.x - FIELD.width / 2 - 0.6, FIELD.z + FIELD.depth / 2 + 0.6]} to={[FIELD.x + FIELD.width / 2 + 0.6, FIELD.z + FIELD.depth / 2 + 0.6]} />
        </group>
      </Interactable>
      <Interactable id="pond" position={[POND.x, terrainHeight(POND.x, POND.z), POND.z]} selected={selected === "pond"} onSelect={select} reducedMotion={reducedMotion}>
        {/* Invisible hit area over the water so the pond is easy to tap. */}
        <mesh visible={false} position={[0, 0.6, 0]}>
          <cylinderGeometry args={[POND.radius, POND.radius, 1.2, 16]} />
        </mesh>
      </Interactable>

      <HayBale position={[4, 3]} rotationY={0.4} onError={onAssetError} />
      <HayBale position={[5.6, 4.4]} rotationY={1.2} onError={onAssetError} />
      {TREES.slice(0, treeCount).map(([x, z, s]) => (
        <Tree key={`${x},${z}`} position={[x, z]} scale={s} onError={onAssetError} />
      ))}

      <PortraitFit />
      {controls && (
        <OrbitControls
          makeDefault
          target={CAMERA.target}
          enableDamping={!reducedMotion}
          {...ORBIT_LIMITS}
        />
      )}
    </>
  );
}
