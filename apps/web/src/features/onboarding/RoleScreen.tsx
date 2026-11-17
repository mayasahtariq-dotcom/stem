"use client";
import { useRouter } from "next/navigation";
import { StickerIcon, type StickerIconName } from "@/components/ui";
import { ROUTES } from "@/features/auth/routing";
import { ROLES, type Role } from "@/features/auth/types";
import { useBilingual } from "@/lib/i18n/use-bilingual";
import { useAppStore } from "@/lib/store/app-store";
import { AuthFrame, ChoiceCard } from "./AuthFrame";

export function useRoleScreen() {
  const router = useRouter();
  const selected = useAppStore((s) => s.draftRole);
  const setDraft = useAppStore((s) => s.setDraftRole);
  return {
    selected,
    choose(role: Role) {
      setDraft(role);
      router.push(ROUTES.login);
    },
  };
}

const ICON: Record<Role, StickerIconName> = { student: "backpack", teacher: "teacher", parent: "people" };
const TILE: Record<Role, string> = { student: "bg-grass/20 border-grass-strong", teacher: "bg-sky-soft border-sky-strong", parent: "bg-coral/15 border-coral-strong" };

function RoleArt({ role }: { role: Role }) {
  return (
    <span className={`grid size-16 shrink-0 place-items-center rounded-card border-2 ${TILE[role]}`}>
      <StickerIcon name={ICON[role]} size={44} />
    </span>
  );
}

export function RoleScreen({ selected, onChoose }: { selected: Role | null; onChoose: (r: Role) => void }) {
  const t = useBilingual("role");
  return (
    <AuthFrame title={t("title")} backHref={ROUTES.language} golu={{ pose: "question", says: t("title") }}>
      <div className="flex flex-col gap-3">
        {ROLES.map((r) => (
          <ChoiceCard key={r} text={t(r)} hint={t(`${r}Hint`)} icon={<RoleArt role={r} />} onClick={() => onChoose(r)} selected={selected === r} />
        ))}
      </div>
    </AuthFrame>
  );
}
