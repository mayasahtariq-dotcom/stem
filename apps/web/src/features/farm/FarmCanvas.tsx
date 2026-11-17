"use client";
import type { ReactNode } from "react";
import { SceneContainer, type SceneOverlays } from "@/three/engine/SceneContainer";
import type { QualitySettings } from "@/three/engine/quality";
import type { SceneStatus } from "@/three/engine/scene-status";
import { FarmWorld, type FarmPlace } from "@/three/world/FarmWorld";

export interface FarmCanvasProps {
  quality: QualitySettings;
  overlays: SceneOverlays;
  label: string;
  selected: FarmPlace | null;
  onSelect: (p: FarmPlace) => void;
  reducedMotion: boolean;
  onError: (e: unknown) => void;
  onStatusChange?: (s: SceneStatus) => void;
}

/** The 3D bundle boundary: everything three.js lives behind this lazily loaded module. */
export default function FarmCanvas({ quality, overlays, label, selected, onSelect, reducedMotion, onError, onStatusChange }: FarmCanvasProps): ReactNode {
  return (
    <SceneContainer quality={quality} overlays={overlays} label={label} onError={onError} onStatusChange={onStatusChange}>
      <FarmWorld quality={quality} selected={selected} onSelect={onSelect} reducedMotion={reducedMotion} onAssetError={onError} />
    </SceneContainer>
  );
}
