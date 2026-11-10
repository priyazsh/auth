import { redirect } from "next/navigation";
import { getServerSession } from "@/lib/auth";
import { SignOutButton } from "./sign-out-button";

// Reads the HttpOnly session cookie, so render dynamically.
export const instant = false;

export default async function DashboardPage() {
  const session = await getServerSession();

  if (!session) {
    redirect("/login");
  }

  const { user } = session;

  return (
    <main className="mx-auto flex w-full max-w-2xl flex-1 flex-col gap-8 px-6 py-16">
      <header className="flex items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">Dashboard</h1>
          <p className="text-sm text-zinc-600 dark:text-zinc-400">
            Your central identity
          </p>
        </div>
        <SignOutButton />
      </header>

      <section className="rounded-2xl border border-black/[.08] p-6 dark:border-white/[.145]">
        <div className="flex items-center gap-4">
          {user.image ? (
            // eslint-disable-next-line @next/next/no-img-element
            <img
              src={user.image}
              alt=""
              className="h-16 w-16 rounded-full object-cover"
            />
          ) : (
            <div className="flex h-16 w-16 items-center justify-center rounded-full bg-zinc-200 text-xl font-semibold text-zinc-600 dark:bg-zinc-800 dark:text-zinc-300">
              {(user.name ?? user.email).charAt(0).toUpperCase()}
            </div>
          )}
          <div className="min-w-0">
            <p className="truncate text-lg font-medium">{user.name ?? "User"}</p>
            <p className="truncate text-sm text-zinc-600 dark:text-zinc-400">
              {user.email}
            </p>
          </div>
        </div>

        <dl className="mt-6 grid grid-cols-1 gap-4 text-sm sm:grid-cols-2">
          <div>
            <dt className="text-zinc-500">User ID</dt>
            <dd className="truncate font-mono text-xs">{user.id}</dd>
          </div>
          <div>
            <dt className="text-zinc-500">Role</dt>
            <dd className="font-medium">{user.role}</dd>
          </div>
          <div>
            <dt className="text-zinc-500">Email verified</dt>
            <dd className="font-medium">{user.emailVerified ? "Yes" : "No"}</dd>
          </div>
          <div>
            <dt className="text-zinc-500">Session</dt>
            <dd className="truncate font-mono text-xs">{session.id}</dd>
          </div>
        </dl>
      </section>
    </main>
  );
}
