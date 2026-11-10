import { redirect } from "next/navigation";
import { getServerSession } from "@/lib/auth";
import { LoginForm } from "./login-form";

// Reads the HttpOnly session cookie, so render dynamically.
export const instant = false;

export default async function LoginPage() {
  const session = await getServerSession();
  if (session) {
    redirect("/dashboard");
  }

  return (
    <main className="flex min-h-dvh flex-col items-center justify-center px-6 py-16">
      <div className="w-full max-w-[360px]">
        <LoginForm />

        <footer className="mt-12 flex flex-col items-center gap-1.5 text-center">
          <span className="text-sm font-medium tracking-tight">Auth Center</span>
          <span className="text-[10px] font-medium uppercase tracking-[0.22em] text-[var(--muted)]">
            Secure Single Sign-On
          </span>
        </footer>
      </div>
    </main>
  );
}
