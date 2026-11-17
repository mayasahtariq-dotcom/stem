"use client";
import { RoleScreen, useRoleScreen } from "@/features/onboarding/RoleScreen";

export default function RolePage() {
  const vm = useRoleScreen();
  return <RoleScreen selected={vm.selected} onChoose={vm.choose} />;
}
