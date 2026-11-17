"use client";
import { Suspense } from "react";
import { PhoneLoginScreen, usePhoneLogin } from "@/features/auth/PhoneLoginScreen";
import { usePostSignIn } from "@/features/auth/use-post-sign-in";

function PhoneLogin() {
  const vm = usePhoneLogin(usePostSignIn());
  return <PhoneLoginScreen state={vm.state} onPhoneChange={vm.setPhone} onCodeChange={vm.setCode} onSend={vm.send} onVerify={vm.verify} onChangeNumber={vm.changeNumber} />;
}

export default function LoginPage() {
  return (
    <Suspense>
      <PhoneLogin />
    </Suspense>
  );
}
