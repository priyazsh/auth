
"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuthSession } from "@/lib/use-auth-session";
import { LoginForm } from "./login-form";

export default function LoginPage() {
  const router = useRouter();
  const { session, loading } = useAuthSession();

  useEffect(() => {
    if (session) {
      router.replace("/dashboard");
    }
  }, [session, router]);

  if (loading || session) {
    return (
      <main className="flex min-h-dvh items-center justify-center">
        <p className="text-sm text-[var(--muted)]">
          Checking your session...
        </p>
      </main>
    );
  }

  return (
    <main className="flex min-h-dvh flex-col items-center justify-center px-6 py-16">
      <div className="w-full max-w-[360px]">
        <LoginForm />

        <footer className="mt-12 flex flex-col items-center gap-1.5 text-center">
          <span className="text-sm font-medium tracking-tight">
            Auth Center
          </span>
          <span className="text-[10px] font-medium uppercase tracking-[0.22em] text-[var(--muted)]">
            Secure Single Sign-On
          </span>
        </footer>
      </div>
    </main>
  );
}
