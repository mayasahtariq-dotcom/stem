import { color } from "@mooshala/tokens";

/**
 * Warm late-morning lighting: a sky/grass hemisphere fill plus one shadow-casting
 * sun. Shadow camera bounds are tight around the farm for crisp shadows at a
 * small map size.
 */
export const LIGHTING = {
  background: "#BFE8FB",
  fog: { color: "#D8F0FB", near: 70, far: 170 },
  hemisphere: { sky: "#CFEFFF", ground: color.leaf, intensity: 1.1 },
  sun: {
    color: "#FFF1D0",
    intensity: 2.4,
    position: [30, 42, 18] as [number, number, number],
    shadowBounds: 40,
    shadowBias: -0.0004,
    shadowNormalBias: 0.04,
  },
} as const;
