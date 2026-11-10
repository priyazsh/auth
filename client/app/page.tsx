
"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuthSession } from "@/lib/use-auth-session";

export default function Home() {
  const router = useRouter();
  const { session, loading } = useAuthSession();

  useEffect(() => {
    if (!loading) {
      router.replace(session ? "/dashboard" : "/login");
    }
  }, [loading, session, router]);

  return (
    <main className="flex min-h-dvh items-center justify-center">
      <p className="text-sm text-[var(--muted)]">
        Checking your session...
      </p>
    </main>
  );
}
