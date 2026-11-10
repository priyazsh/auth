
"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuthSession } from "@/lib/use-auth-session";
import { SignOutButton } from "./sign-out-button";

export default function DashboardPage() {
  const router = useRouter();
  const { session, loading } = useAuthSession();

  useEffect(() => {
    if (!loading && !session) {
      router.replace("/login");
    }
  }, [loading, session, router]);

  if (loading || !session) {
    return (
      <main className="flex min-h-dvh items-center justify-center">
        <p className="text-sm text-[var(--muted)]">
          Checking your session...
        </p>
      </main>
    );
  }

  const { user } = session;
  const initial = (user.name ?? user.email).charAt(0).toUpperCase();

  return (
    <main className="flex min-h-dvh flex-col items-center justify-center px-6 py-16">
      <div className="w-full max-w-[400px]">
        <div className="rounded-[28px] border border-[var(--border)] bg-[var(--card)] p-8 shadow-[0_1px_0_rgba(255,255,255,0.55)_inset,0_24px_70px_-28px_rgba(0,0,0,0.28)] backdrop-blur-xl">
          <div className="flex items-center gap-4">
            {user.image ? (
              // eslint-disable-next-line @next/next/no-img-element
              <img
                src={user.image}
                alt=""
                className="h-12 w-12 rounded-full object-cover ring-1 ring-[var(--border)]"
              />
            ) : (
              <div className="flex h-12 w-12 items-center justify-center rounded-full bg-foreground text-sm font-semibold text-background">
                {initial}
              </div>
            )}

            <div className="min-w-0">
              <p className="truncate text-[15px] font-semibold tracking-tight">
                {user.name ?? "User"}
              </p>
              <p className="truncate text-xs text-[var(--muted)]">
                {user.email}
              </p>
            </div>
          </div>

          <div className="my-7 h-px bg-[var(--border)]" />

          <dl className="space-y-3.5 text-[13px]">
            <div className="flex items-center justify-between gap-4">
              <dt className="text-[var(--muted)]">Account ID</dt>
              <dd className="max-w-[58%] truncate font-mono text-[11px] text-[var(--muted)]">
                {user.id}
              </dd>
            </div>

            <div className="flex items-center justify-between gap-4">
              <dt className="text-[var(--muted)]">Role</dt>
              <dd className="font-medium capitalize">{user.role}</dd>
            </div>

            <div className="flex items-center justify-between gap-4">
              <dt className="text-[var(--muted)]">Email verified</dt>
              <dd className="font-medium">
                {user.emailVerified ? "Yes" : "No"}
              </dd>
            </div>
          </dl>

          <div className="mt-8 flex items-center justify-between gap-4">
            <span className="text-[11px] uppercase tracking-[0.16em] text-[var(--muted)]">
              Signed in
            </span>
            <SignOutButton />
          </div>
        </div>

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
