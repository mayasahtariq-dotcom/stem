"use client";
import { Suspense } from "react";
import { PhoneLoginScreen, usePhoneLogin } from "@/features/auth/PhoneLoginScreen";
import { usePostSignIn } from "@/features/auth/use-post-sign-in";
import { useAppStore } from "@/lib/store/app-store";

function PhoneLogin() {
  const vm = usePhoneLogin(usePostSignIn());
  const role = useAppStore((s) => s.draftRole);
  const setRole = useAppStore((s) => s.setDraftRole);
  return <PhoneLoginScreen role={role} onRoleChange={setRole} state={vm.state} onPhoneChange={vm.setPhone} onCodeChange={vm.setCode} onSend={vm.send} onVerify={vm.verify} onChangeNumber={vm.changeNumber} />;
}

export default function LoginPage() {
  return (
    <Suspense>
      <PhoneLogin />
    </Suspense>
  );
}
