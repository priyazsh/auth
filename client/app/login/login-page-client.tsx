"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { API_BASE, fetchAuthorizationRequest } from "@/lib/api";
import { useAuthSession } from "@/lib/use-auth-session";
import { LoginForm } from "./login-form";

type LoginPageClientProps = {
  requestId: string | null;
  error: string | null;
};

export function LoginPageClient({ requestId, error }: LoginPageClientProps) {
  const router = useRouter();
  const { session, loading } = useAuthSession();
  const [clientName, setClientName] = useState<string | null>(null);

  useEffect(() => {
    if (!requestId) return;
    let cancelled = false;

    fetchAuthorizationRequest(requestId).then((request) => {
      if (!cancelled) {
        setClientName(request?.clientName ?? null);
      }
    });

    return () => {
      cancelled = true;
    };
  }, [requestId]);

  useEffect(() => {
    if (loading) return;

    // A session already exists. Finish the parked OAuth request without Google.
    if (session && requestId) {
      window.location.assign(
        `${API_BASE}/oauth2/authorize/complete?request_id=${encodeURIComponent(requestId)}`
      );
      return;
    }

    // Direct login page: a session means the user is already signed in.
    if (session && !requestId) {
      router.replace("/dashboard");
    }
  }, [loading, session, requestId, router]);

  if (loading || (session && requestId)) {
    return (
      <main className="flex min-h-dvh items-center justify-center">
        <p className="text-sm text-[var(--muted)]">
          Checking your session...
        </p>
      </main>
    );
  }

  if (session && !requestId) {
    return null;
  }

  return (
    <main className="flex min-h-dvh flex-col items-center justify-center px-6 py-16">
      <div className="w-full max-w-[360px]">
        {error === "expired" ? (
          <NoticeCard
            title="This sign-in request expired"
            message="Return to the app and try signing in again."
          />
        ) : error === "google_not_configured" ? (
          <NoticeCard
            title="Google sign-in is unavailable"
            message="Auth Center is not configured for Google right now. Try again later."
          />
        ) : (
          <LoginForm requestId={requestId} clientName={clientName} />
        )}

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

function NoticeCard({ title, message }: { title: string; message: string }) {
  return (
    <div className="rounded-[28px] border border-[var(--border)] bg-[var(--card)] p-8 text-center shadow-[0_1px_0_rgba(255,255,255,0.55)_inset,0_24px_70px_-28px_rgba(0,0,0,0.28)] backdrop-blur-xl">
      <h1 className="text-[22px] font-semibold tracking-tight">{title}</h1>
      <p className="mt-2 text-sm leading-6 text-[var(--muted)]">{message}</p>
    </div>
  );
}
