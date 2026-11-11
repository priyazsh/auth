import { LoginPageClient } from "./login-page-client";

// The login page needs the query string (request_id/error) at request time.
export const instant = false;

type LoginPageProps = {
  searchParams: Promise<{ request_id?: string; error?: string }>;
};

export default async function LoginPage({ searchParams }: LoginPageProps) {
  const params = await searchParams;
  return (
    <LoginPageClient
      requestId={params.request_id ?? null}
      error={params.error ?? null}
    />
  );
}
