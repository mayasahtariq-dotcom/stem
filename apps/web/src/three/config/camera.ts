/**
 * Camera: a three-quarter "farm sim" view looking down on the farmstead, with
 * orbit limits so the player can turn and zoom but never go under the ground
 * or lose the farm.
 */
export const CAMERA = {
  fov: 45,
  near: 0.5,
  far: 400,
  position: [26, 22, 30] as [number, number, number],
  target: [0, 1, 0] as [number, number, number],
} as const;

export const ORBIT_LIMITS = {
  minDistance: 18,
  maxDistance: 110,
  minPolarAngle: Math.PI * 0.18, // not straight down
  maxPolarAngle: Math.PI * 0.42, // never level with or below the horizon
  enablePan: false,
  dampingFactor: 0.08,
  rotateSpeed: 0.6,
  zoomSpeed: 0.8,
} as const;
