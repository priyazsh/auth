import { cookies } from "next/headers";
import { API_BASE, type Session } from "./api";

/**
 * Server-to-server calls should hit the backend directly instead of looping
 * back out through the public domain / CDN. Browsers still use API_BASE.
 */
const SERVER_API_BASE = process.env.API_INTERNAL_BASE_URL ?? API_BASE;

type GetSessionResponse = {
  session: Session | Record<string, never>;
};

/**
 * Server-side session lookup. Forwards the incoming HttpOnly SESSION cookie to
 * the Auth Center backend. Never exposes the cookie or tokens to client JS.
 */
export async function getServerSession(): Promise<Session | null> {
  const cookieStore = await cookies();
  const cookieHeader = cookieStore.toString();

  try {
    const res = await fetch(`${SERVER_API_BASE}/api/auth/get-session`, {
      headers: cookieHeader ? { cookie: cookieHeader } : {},
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
