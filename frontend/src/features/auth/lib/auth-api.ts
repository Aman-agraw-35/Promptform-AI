export type AuthUser = { id: string; email: string; displayName: string };
export type AuthResponse = { user: AuthUser; accessToken: string };

const apiUrl = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

async function request(path: string, body: Record<string, string>) {
  const response = await fetch(`${apiUrl}${path}`, {
    method: "POST",
    credentials: "include",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });

  if (!response.ok) {
    let message = "Authentication failed. Please try again.";
    try {
      const problem = await response.json() as { detail?: string; message?: string };
      message = problem.detail ?? problem.message ?? message;
    } catch {
      // Keep the safe generic message when the server response is not JSON.
    }
    throw new Error(message);
  }

  return response.json() as Promise<AuthResponse>;
}

export function login(email: string, password: string) {
  return request("/api/auth/login", { email, password });
}

export function register(email: string, password: string, displayName: string) {
  return request("/api/auth/register", { email, password, displayName });
}
