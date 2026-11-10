
"use client";

import { useEffect, useState } from "react";
import { fetchSession, type Session } from "@/lib/api";

export function useAuthSession() {
  const [session, setSession] = useState<Session | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;

    async function checkSession() {
      const currentSession = await fetchSession();

      if (!cancelled) {
        setSession(currentSession);
        setLoading(false);
      }
    }

    checkSession();

    return () => {
      cancelled = true;
    };
  }, []);

  return { session, loading };
}
