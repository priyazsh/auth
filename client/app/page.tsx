import { redirect } from "next/navigation";
import { getServerSession } from "@/lib/auth";

// Reads the HttpOnly session cookie, so render dynamically.
export const instant = false;

export default async function Home() {
  const session = await getServerSession();
  redirect(session ? "/dashboard" : "/login");
}
