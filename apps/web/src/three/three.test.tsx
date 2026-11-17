import { render, screen } from "@testing-library/react";
import ReactThreeTestRenderer from "@react-three/test-renderer";
import { vi } from "vitest";
import { QUALITY, pickQuality } from "./engine/quality";
import { initialSceneState, sceneReducer, type SceneEvent, type SceneState } from "./engine/scene-status";
import { SceneContainer } from "./engine/SceneContainer";
import { FLAT_RADIUS, FIELD, POND, groundKind, terrainHeight } from "./world/terrain-math";

// GLB loading needs a network and WebGL; the scene-graph test uses the procedural fallbacks.
vi.mock("./assets/FarmModel", () => ({
  FarmModel: ({ fallback }: { fallback: React.ReactNode }) => fallback,
  preloadFarmAssets: () => {},
}));

const run = (...events: SceneEvent[]): SceneState => events.reduce(sceneReducer, initialSceneState);

describe("scene lifecycle", () => {
  it("goes checking -> loading -> ready", () => {
    expect(run({ type: "webgl", available: true }).status).toBe("loading");
    expect(run({ type: "webgl", available: true }, { type: "assetsLoaded" }).status).toBe("ready");
  });
  it("is unsupported without WebGL and stays so", () => {
    expect(run({ type: "webgl", available: false }, { type: "failed", error: 1 }).status).toBe("unsupported");
  });
  it("recovers from errors and lost contexts by retrying with a fresh canvas", () => {
    const failed = run({ type: "webgl", available: true }, { type: "failed", error: "x" });
    expect(failed.status).toBe("error");
    const retried = sceneReducer(failed, { type: "retry" });
    expect(retried).toMatchObject({ status: "loading", attempt: 1, error: null });
    const lost = run({ type: "webgl", available: true }, { type: "assetsLoaded" }, { type: "contextLost" });
    expect(lost.status).toBe("contextLost");
    expect(sceneReducer(lost, { type: "contextRestored" }).status).toBe("ready");
  });
});

describe("quality tiers", () => {
  it("uses the low tier for 4 GB school PCs and data saver", () => {
    expect(pickQuality({ memoryGb: 4, cores: 8 }).tier).toBe("low");
    expect(pickQuality({ saveData: true }).tier).toBe("low");
    expect(QUALITY.low.shadows).toBe(false);
  });
  it("uses medium on phones and high on strong desktops", () => {
    expect(pickQuality({ memoryGb: 8, cores: 8, mobile: true }).tier).toBe("medium");
    expect(pickQuality({ memoryGb: 16, cores: 12 }).tier).toBe("high");
  });
});

describe("terrain", () => {
  it("is flat in the farmstead, hilly outside, and dips at the pond", () => {
    expect(terrainHeight(0, 0)).toBe(0);
    expect(terrainHeight(FLAT_RADIUS - 1, 0)).toBe(0);
    const far = [terrainHeight(60, 0), terrainHeight(0, -60), terrainHeight(-55, 30)];
    expect(Math.max(...far)).toBeGreaterThan(1);
    expect(terrainHeight(POND.x, POND.z)).toBeLessThan(-1);
  });
  it("marks the tilled field and the path", () => {
    expect(groundKind(FIELD.x, FIELD.z)).toBe("soil");
    expect(groundKind(Math.sin(10 * 0.12) * 2.5, 10)).toBe("path");
  });
});

describe("SceneContainer", () => {
  const overlays = {
    loading: () => <p>loading farm</p>,
    error: (retry: () => void) => <button onClick={retry}>retry farm</button>,
    unsupported: () => <p>no webgl</p>,
  };
  it("shows the unsupported state instead of a blank canvas when WebGL is missing", async () => {
    const onStatus = vi.fn();
    render(
      <SceneContainer quality={QUALITY.low} overlays={overlays} label="farm" webglAvailable={() => false} onStatusChange={onStatus}>
        <mesh />
      </SceneContainer>,
    );
    expect(await screen.findByText("no webgl")).toBeInTheDocument();
    expect(onStatus).toHaveBeenLastCalledWith("unsupported");
    expect(document.querySelector("canvas")).toBeNull();
  });
});

describe("FarmWorld scene graph", () => {
  it("builds real terrain and farm objects, and tapping an object selects it", async () => {
    const { FarmWorld } = await import("./world/FarmWorld");
    const onSelect = vi.fn();
    const r = await ReactThreeTestRenderer.create(<FarmWorld quality={QUALITY.low} onSelect={onSelect} controls={false} reducedMotion />);
    const names = new Set<string>();
    (r.scene.instance as unknown as { traverse: (f: (o: { name: string }) => void) => void }).traverse((o) => o.name && names.add(o.name));
    for (const n of ["terrain", "terrain-ground", "pond-water", "barn", "windmill", "crop-field", "fence", "tree-fallback", "hay-fallback"]) {
      expect(names).toContain(n);
    }
    const ground = r.scene.findByProps({ name: "terrain-ground" });
    const pos = (ground.instance as unknown as { geometry: { attributes: { position: { count: number } } } }).geometry.attributes.position;
    expect(pos.count).toBe(65 * 65); // low tier: 64 segments

    const barn = r.scene.findByProps({ name: "interactable-barn" });
    await r.fireEvent(barn, "click");
    expect(onSelect).toHaveBeenCalledWith("barn");
    await r.unmount();
  });
});
