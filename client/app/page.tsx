import Link from "next/link";
import { getServerSession } from "@/lib/auth";

// The page reads the HttpOnly session cookie, so it must render dynamically.
export const instant = false;

export default async function Home() {
  const session = await getServerSession();

  return (
    <main className="flex flex-1 flex-col items-center justify-center gap-10 px-6 py-24">
      <div className="flex flex-col items-center gap-4 text-center">
        <span className="rounded-full border border-black/[.08] px-3 py-1 text-xs font-medium tracking-wide uppercase dark:border-white/[.145]">
          Central Identity
        </span>
        <h1 className="text-4xl font-semibold tracking-tight sm:text-5xl">
          auth.priyazsh.me
        </h1>
        <p className="max-w-lg text-base leading-7 text-zinc-600 dark:text-zinc-400">
          One account for every project. Sign in once with Google and use the
          same identity across GoLink and future apps.
        </p>
      </div>

      <div className="flex flex-col items-center gap-4 sm:flex-row">
        {session ? (
          <Link
            href="/dashboard"
            className="flex h-12 items-center justify-center rounded-full bg-foreground px-6 text-sm font-medium text-background transition-opacity hover:opacity-90"
          >
            Go to dashboard
          </Link>
        ) : (
          <Link
            href="/login"
            className="flex h-12 items-center justify-center rounded-full bg-foreground px-6 text-sm font-medium text-background transition-opacity hover:opacity-90"
          >
            Sign in
          </Link>
        )}
        <a
          href="https://go.priyazsh.me"
          className="flex h-12 items-center justify-center rounded-full border border-black/[.08] px-6 text-sm font-medium transition-colors hover:bg-black/[.04] dark:border-white/[.145] dark:hover:bg-white/[.08]"
        >
          GoLink
        </a>
      </div>

      {session && (
        <p className="text-sm text-zinc-500">
          Signed in as {session.user.email}
        </p>
      )}
    </main>
  );
}
