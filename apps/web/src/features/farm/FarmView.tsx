"use client";
import dynamic from "next/dynamic";
import { useMemo, useState } from "react";
import { BilingualText, ChunkyButton, ErrorState, GoluMascot, LoadingState, SpeechBubble, StickerIcon, WoodPanel } from "@/components/ui";
import { useProfile } from "@/features/auth/hooks";
import { useReducedMotion } from "@/lib/hooks/use-reduced-motion";
import { useBilingual } from "@/lib/i18n/use-bilingual";
import { useServices } from "@/lib/services";
import { sounds } from "@/lib/sound/sound-manager";
import { pickQuality, readDeviceHints } from "@/three/engine/quality";
import { FARM_PLACES, type FarmPlace } from "@/three/world/FarmWorld";

// three.js (~600 KB) loads only here, after sign-in, never on the login screens.
const FarmCanvas = dynamic(() => import("./FarmCanvas"), { ssr: false, loading: () => <LoadingState className="h-full" /> });

/**
 * The student's primary viewport: the 3D farm plus its HTML overlays (selected
 * place card, greeting, screen-reader list of places). UI state only; the
 * scene itself lives in src/three.
 */
export function FarmView() {
  const t = useBilingual("farm");
  const { reporter } = useServices();
  const profile = useProfile();
  const reduced = useReducedMotion();
  const quality = useMemo(() => pickQuality(readDeviceHints()), []);
  const [selected, setSelected] = useState<FarmPlace | null>(null);
  const [greeted, setGreeted] = useState(false);

  const select = (p: FarmPlace) => {
    setSelected(p);
    setGreeted(true);
    sounds.play("tap");
  };

  return (
    <div className="relative h-full w-full">
      <FarmCanvas
        quality={quality}
        label={t("label").english}
        selected={selected}
        onSelect={select}
        reducedMotion={reduced}
        onError={(e) => reporter.capture(e, { where: "farm3d" })}
        overlays={{
          loading: (pct) => (
            <LoadingState message={pct > 0 && pct < 100 ? t("loadingProgress", { percent: Math.round(pct) }) : t("loading")} />
          ),
          error: (retry) => <ErrorState error={new Error("farm")} onRetry={retry} className="[&_[role=alert]]:contents" />,
          unsupported: () => (
            <div className="grid h-full place-items-center bg-gradient-to-b from-sky-soft to-[#eaf8d8] p-6">
              <SpeechBubble className="max-w-sm">
                <BilingualText text={t("unsupported")} align="center" />
              </SpeechBubble>
            </div>
          ),
        }}
      />

      {!greeted && profile.data?.name && (
        <div className="pointer-events-none absolute left-3 top-20 z-10 flex max-w-xs items-end gap-2 tablet:left-4">
          <GoluMascot pose="wave" size={72} />
          <SpeechBubble tail="left" className="mb-4">
            <BilingualText size="sm" text={t("hello", { name: profile.data.name })} />
          </SpeechBubble>
        </div>
      )}

      {selected && (
        <div className="absolute inset-x-3 bottom-28 z-10 mx-auto max-w-sm" role="dialog" aria-label={t(selected).english}>
          <WoodPanel>
            <div className="flex items-start gap-3">
              <StickerIcon name={selected === "field" ? "corn" : selected === "pond" ? "drop" : selected === "windmill" ? "grass" : "home"} size={40} />
              <div className="flex flex-1 flex-col gap-1">
                <BilingualText as="h2" size="lg" text={t(selected)} />
                <BilingualText size="sm" text={t("placeComingSoon")} />
              </div>
              <ChunkyButton variant="ghost" size="icon" onClick={() => setSelected(null)} aria-label={t("close").english}>
                ✕
              </ChunkyButton>
            </div>
          </WoodPanel>
        </div>
      )}

      {/* Keyboard and screen-reader access to the same places the 3D scene offers. */}
      <nav aria-label={t("places").english} className="sr-only focus-within:not-sr-only focus-within:absolute focus-within:left-3 focus-within:top-20 focus-within:z-30 focus-within:flex focus-within:gap-2">
        {FARM_PLACES.map((p) => (
          <button key={p} type="button" onClick={() => select(p)} className="min-h-11 rounded-button bg-white px-3 font-bold">
            {t(p).regional} · {t(p).english}
          </button>
        ))}
      </nav>
      <p aria-live="polite" className="sr-only">
        {selected ? t("selected", { place: t(selected).english }).english : ""}
      </p>
    </div>
  );
}
