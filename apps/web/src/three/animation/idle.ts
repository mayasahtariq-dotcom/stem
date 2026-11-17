/**
 * Idle animation curves for the farm (pure functions of time, unit-tested).
 * Scene components call these from useFrame; with reduced motion they are
 * evaluated once at t = 0 so everything rests in a natural pose.
 */

/** Eyelid scale: 1 open, 0 closed. A quick blink every `period` seconds, offset per animal. */
export function blink(t: number, period = 4.2, offset = 0): number {
  const p = (((t + offset) % period) + period) % period;
  if (p > 0.16) return 1;
  return Math.abs(Math.cos((p / 0.16) * Math.PI));
}

/** Tail swing angle in radians: a lazy swish with a short pause. */
export function tailSwing(t: number, offset = 0): number {
  const s = Math.sin((t + offset) * 2.4);
  return 0.45 * s * Math.abs(s);
}

/** Grazing: the head dips to the grass now and then (0 up, 1 down). */
export function graze(t: number, offset = 0): number {
  const p = ((t + offset) % 9) / 9;
  return p < 0.55 ? 0 : Math.sin(((p - 0.55) / 0.45) * Math.PI);
}

/** Gentle sway for grass, bushes and trees (radians). */
export function sway(t: number, x: number, z: number, strength = 0.04): number {
  return Math.sin(t * 1.3 + x * 0.35 + z * 0.21) * strength;
}

/** A point on a butterfly's looping flight around its home. */
export function butterflyPath(t: number, seed: number, radius = 2.2): [number, number, number] {
  const a = t * (0.6 + (seed % 3) * 0.15) + seed;
  return [Math.cos(a) * radius + Math.sin(a * 2.3) * 0.6, 1.2 + Math.sin(a * 3.1) * 0.35, Math.sin(a) * radius * 0.7];
}

/** Wing flap angle. */
export const flap = (t: number, seed: number) => Math.sin(t * 18 + seed) * 0.9;

/** Clouds drift east and wrap around the sky. */
export function cloudX(t: number, startX: number, speed: number, span = 160): number {
  return ((((startX + t * speed + span / 2) % span) + span) % span) - span / 2;
}

/** Bounce for the current lesson marker (0..1). */
export const bounce = (t: number) => Math.abs(Math.sin(t * 2.2));
