"use client";
import { GrowthAndBadgesRoute } from "@/features/game/routes";
import { ProfileRoute } from "@/features/learning/ProfileRoute";
import { ParentLinkCard } from "@/features/parent/ParentLinkCard";

export default function ProfilePage() {
  return (
    <ProfileRoute>
      <GrowthAndBadgesRoute />
      <ParentLinkCard />
    </ProfileRoute>
  );
}
