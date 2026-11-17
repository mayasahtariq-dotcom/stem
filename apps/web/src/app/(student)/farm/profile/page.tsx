"use client";
import { ProfileRoute } from "@/features/learning/ProfileRoute";
import { ParentLinkCard } from "@/features/parent/ParentLinkCard";

export default function ProfilePage() {
  return (
    <ProfileRoute>
      <ParentLinkCard />
    </ProfileRoute>
  );
}
