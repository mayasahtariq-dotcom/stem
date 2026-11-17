"use client";
import Link from "next/link";
import { usePathname } from "next/navigation";
import type { ReactNode } from "react";
import { BilingualText, GoluMascot, StickerIcon } from "@/components/ui";
import { useProfile } from "@/features/auth/hooks";
import { useBilingual } from "@/lib/i18n/use-bilingual";
import { cn } from "@/lib/utils/cn";
import { HudControls } from "./HudControls";
import { NAV, isAvailable } from "./nav";

/**
 * Student shell: the 3D farm fills the viewport; UI floats over it as a game
 * HUD (top) and a wooden signpost dock (bottom). No dashboard sidebar.
 */
export function StudentShell({ children }: { children: ReactNode }) {
  const profile = useProfile();
  const n = useBilingual("nav");
  const c = useBilingual("common");
  const pathname = usePathname();
  return (
    <div className="relative h-dvh w-full overflow-hidden bg-sky-soft">
      <main className="absolute inset-0">{children}</main>

      <header className="pointer-events-none absolute inset-x-0 top-0 z-20 flex items-start justify-between gap-2 p-3 tablet:p-4">
        <div className="pointer-events-auto flex min-h-11 items-center gap-2 rounded-full bg-white/95 py-1 pl-1 pr-4 shadow-float-1">
          <span className="grid size-10 place-items-center overflow-hidden rounded-full bg-sun-soft">
            <GoluMascot pose="wave" size={40} animated={false} />
          </span>
          <span className="max-w-[40vw] truncate font-bold">{profile.data?.name ?? ""}</span>
        </div>
        <div className="pointer-events-auto">
          <HudControls />
        </div>
      </header>

      <nav aria-label={n("studentNav").english} className="pointer-events-none absolute inset-x-0 bottom-0 z-20 flex justify-center p-3 pb-[max(12px,env(safe-area-inset-bottom))]">
        <ul className="wood-grain pointer-events-auto flex gap-1 rounded-panel p-1.5 [box-shadow:0_6px_0_var(--color-wood-dark),var(--shadow-float-2)]">
          {NAV.student.map((item) => {
            const active = pathname === item.href;
            const available = isAvailable(item);
            const label = n(item.label);
            const body = (
              <>
                <StickerIcon name={item.icon} size={28} className={available ? undefined : "opacity-50 grayscale"} />
                <span className="font-regional text-xs font-bold leading-tight">{label.regional}</span>
                {!available && <span className="sr-only">({c("comingSoon").english})</span>}
              </>
            );
            const cls = cn(
              "flex min-h-14 min-w-14 flex-col items-center justify-center gap-0.5 rounded-card px-2 tablet:min-w-20",
              active ? "bg-parchment text-ink shadow-[var(--shadow-inset)]" : "text-white [text-shadow:0_1px_0_rgb(0_0_0/0.4)]",
              available ? "hover:bg-white/15" : "cursor-not-allowed",
            );
            return (
              <li key={item.key}>
                {available ? (
                  <Link href={item.href} aria-current={active ? "page" : undefined} aria-label={label.english} className={cls}>
                    {body}
                  </Link>
                ) : (
                  <span role="link" aria-disabled="true" aria-label={`${label.english} (${c("comingSoon").english})`} title={c("comingSoon").regional} className={cls}>
                    {body}
                  </span>
                )}
              </li>
            );
          })}
        </ul>
      </nav>
      <BilingualText className="sr-only" text={c("appName")} />
    </div>
  );
}
