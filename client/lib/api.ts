export const API_BASE =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

export type SessionUser = {
  id: string;
  email: string;
  name: string | null;
  image: string | null;
  emailVerified: boolean;
  role: string;
};

export type Session = {
  id: string;
  userId: string;
  user: SessionUser;
};

type GetSessionResponse = {
  session: Session | Record<string, never>;
};

/** Read the current Auth Center session (cookie is sent with credentials). */
/** Read the current Auth Center session using the browser's HttpOnly cookie. */
export async function fetchSession(): Promise<Session | null> {
  try {
    const res = await fetch(`${API_BASE}/api/auth/get-session`, {
      credentials: "include",
      cache: "no-store",
    });

    if (!res.ok) return null;

    const data = (await res.json()) as GetSessionResponse;
    const session = data.session as Session;

    if (!session || !("user" in session)) return null;

    return session;
  } catch {
    return null;
  }
}

/** Ask the backend to build a Google authorization URL. */
export async function startGoogleSignIn(callbackURL = "/dashboard"): Promise<string> {
  const res = await fetch(`${API_BASE}/api/auth/sign-in/social`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    credentials: "include",
    body: JSON.stringify({ provider: "google", callbackURL }),
  });
  if (!res.ok) {
    const body = await res.text();
    throw new Error(`Sign-in failed (${res.status}): ${body}`);
  }
  const data = (await res.json()) as { url: string };
  return data.url;
}

/** Invalidate the server-side session and clear the SESSION cookie. */
export async function signOut(): Promise<void> {
  await fetch(`${API_BASE}/sign-out`, {
    method: "POST",
    credentials: "include",
  });
}
