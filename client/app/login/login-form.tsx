"use client";

import { useState } from "react";
import { API_BASE, startGoogleSignIn } from "@/lib/api";

type LoginFormProps = {
  requestId?: string | null;
  clientName?: string | null;
};

export function LoginForm({ requestId, clientName }: LoginFormProps) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleGoogleSignIn() {
    setLoading(true);
    setError(null);
    try {
      // OAuth client flow: let the backend start Google for the parked request.
      if (requestId) {
        window.location.assign(
          `${API_BASE}/oauth2/authorize/start?request_id=${encodeURIComponent(requestId)}`
        );
        return;
      }

      // Direct Auth Center login.
      const url = await startGoogleSignIn("/dashboard");
      window.location.assign(url);
    } catch {
      setError("We couldn’t start sign-in. Please try again.");
      setLoading(false);
    }
  }

  return (
    <div className="rounded-[28px] border border-[var(--border)] bg-[var(--card)] p-8 shadow-[0_1px_0_rgba(255,255,255,0.55)_inset,0_24px_70px_-28px_rgba(0,0,0,0.28)] backdrop-blur-xl">
      <div className="flex flex-col items-center text-center">
        <div className="mb-6 flex h-11 w-11 items-center justify-center rounded-2xl border border-[var(--border)] bg-background/60">
          <svg
            width="18"
            height="18"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="1.6"
            strokeLinecap="round"
            strokeLinejoin="round"
            aria-hidden="true"
          >
            <rect x="4" y="10" width="16" height="10" rx="2.5" />
            <path d="M8 10V7a4 4 0 0 1 8 0v3" />
            <circle cx="12" cy="15" r="1" />
          </svg>
        </div>

        <h1 className="text-[22px] font-semibold tracking-tight">
          {requestId
            ? `Sign in to continue${clientName ? ` to ${clientName}` : ""}`
            : "Sign in to continue"}
        </h1>
        <p className="mt-2 max-w-[26ch] text-sm leading-6 text-[var(--muted)]">
          {requestId
            ? "You’ll return to the app after signing in."
            : "Centralized auth for @priyazsh builds"}
        </p>
      </div>

      <button
        type="button"
        onClick={handleGoogleSignIn}
        disabled={loading}
        className="group mt-8 flex h-11 w-full items-center justify-center gap-2.5 rounded-full bg-foreground text-sm font-medium text-background transition-all duration-200 hover:opacity-90 active:scale-[0.985] disabled:cursor-not-allowed disabled:opacity-60"
      >
        <svg width="16" height="16" viewBox="0 0 48 48" aria-hidden="true">
          <path
            fill="#EA4335"
            d="M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5z"
          />
          <path
            fill="#4285F4"
            d="M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.78 7.18l7.73 6c4.51-4.18 7.09-10.36 7.09-17.65z"
          />
          <path
            fill="#FBBC05"
            d="M10.53 28.59c-.48-1.45-.76-2.99-.76-4.59s.27-3.14.76-4.59l-7.98-6.19C.92 16.46 0 20.12 0 24c0 3.88.92 7.54 2.56 10.78l7.97-6.19z"
          />
          <path
            fill="#34A853"
            d="M24 48c6.48 0 11.93-2.13 15.89-5.81l-7.73-6c-2.15 1.45-4.92 2.3-8.16 2.3-6.26 0-11.57-4.22-13.47-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48z"
          />
        </svg>
        {loading ? "Redirecting…" : "Continue with Google"}
      </button>

      {error && (
        <p
          role="alert"
          className="mt-4 rounded-xl border border-red-200/60 bg-red-50 px-3 py-2 text-center text-xs text-red-700 dark:border-red-900/50 dark:bg-red-950/40 dark:text-red-300"
        >
          {error}
        </p>
      )}
    </div>
  );
}
