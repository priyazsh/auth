"use client";

import { useState } from "react";
import { signOut } from "@/lib/api";

export function SignOutButton() {
  const [loading, setLoading] = useState(false);

  async function handleSignOut() {
    setLoading(true);
    try {
      await signOut();
    } finally {
      window.location.assign("/");
    }
  }

  return (
    <button
      type="button"
      onClick={handleSignOut}
      disabled={loading}
      className="rounded-full border border-[var(--border)] px-4 py-1.5 text-xs font-medium text-[var(--muted)] transition-colors hover:bg-foreground hover:text-background disabled:opacity-60"
    >
      {loading ? "Signing out…" : "Sign out"}
    </button>
  );
}
