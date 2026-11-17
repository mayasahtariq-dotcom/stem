"use client";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { GAME_PATHS, type GameStats, type ShopItem } from "@mooshala/api-client";
import { queryKeys } from "@/lib/api/query-keys";
import { useServices } from "@/lib/services";
import { useAuthStore } from "@/features/auth/auth-store";

/**
 * Game hooks. These endpoints are named in the web spec but NOT built by the
 * backend yet; in fake mode MSW serves them from src/mocks/game. In real mode
 * they fail with NOT_FOUND and every game panel shows its error state, never
 * invented numbers. Gems never gate a lesson.
 */
const useSignedIn = () => useAuthStore((s) => s.status === "signedIn");

export function useGameStats() {
  const { pending } = useServices();
  return useQuery({ queryKey: queryKeys.game.stats, enabled: useSignedIn(), queryFn: () => pending.get<GameStats>(GAME_PATHS.stats), retry: false });
}

export function useShop() {
  const { pending } = useServices();
  return useQuery({ queryKey: queryKeys.game.shop, enabled: useSignedIn(), queryFn: () => pending.get<ShopItem[]>(GAME_PATHS.shop), retry: false });
}

function useGameMutation<V>(fn: (v: V) => Promise<GameStats>) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: fn,
    onSuccess: (stats) => {
      qc.setQueryData(queryKeys.game.stats, stats);
      void qc.invalidateQueries({ queryKey: queryKeys.game.shop });
    },
  });
}

/** POST /game/freeze: buy a streak freeze (the server checks gems and the two-freeze cap). */
export function useBuyFreeze() {
  const { pending } = useServices();
  return useGameMutation<void>(() => pending.post<GameStats>(GAME_PATHS.freeze));
}

export function useBuyItem() {
  const { pending } = useServices();
  return useGameMutation<string>((item) => pending.post<GameStats>(GAME_PATHS.buy(item)));
}

export function useEquipOutfit() {
  const { pending } = useServices();
  return useGameMutation<string | null>((outfit) => pending.put<GameStats>(GAME_PATHS.equip, { outfit }));
}
