export type EvalRun = {
  id: string;
  status: "RUNNING" | "COMPLETED" | "FAILED";
  totalCases: number;
  passedCases: number;
  passRate: number;
  startedAt: string;
  completedAt?: string;
};

const apiUrl = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

async function request<T>(path: string, token: string, options?: RequestInit): Promise<T> {
  const response = await fetch(`${apiUrl}${path}`, {
    ...options,
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json",
      ...options?.headers,
    },
  });
  if (!response.ok) throw new Error(`Evaluation request failed with status ${response.status}.`);
  return response.json() as Promise<T>;
}

export function listEvalRuns(token: string) {
  return request<EvalRun[]>("/api/evals/runs", token);
}

export function startEvalRun(token: string) {
  return request<EvalRun>("/api/evals/run", token, { method: "POST" });
}
