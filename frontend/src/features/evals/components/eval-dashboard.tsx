"use client";

import { useCallback, useEffect, useState } from "react";
import { listEvalRuns, startEvalRun, type EvalRun } from "@/features/evals/lib/evals-api";

function formatDate(value: string) {
  return new Intl.DateTimeFormat("en", {
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value));
}

function percent(value: number) {
  return `${Math.round(value * 100)}%`;
}

export function EvalDashboard() {
  const [token, setToken] = useState("");
  const [runs, setRuns] = useState<EvalRun[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadRuns = useCallback(
    async (activeToken = token) => {
      if (!activeToken.trim()) return;
      setIsLoading(true);
      setError(null);
      try {
        setRuns(await listEvalRuns(activeToken));
      } catch (requestError) {
        setError(
          requestError instanceof Error ? requestError.message : "Could not load evaluation runs.",
        );
      } finally {
        setIsLoading(false);
      }
    },
    [token],
  );

  async function handleStart() {
    if (!token.trim()) return;
    setIsLoading(true);
    setError(null);
    try {
      await startEvalRun(token);
      await loadRuns();
    } catch (requestError) {
      setError(
        requestError instanceof Error ? requestError.message : "Could not start evaluation.",
      );
      setIsLoading(false);
    }
  }

  useEffect(() => {
    const savedToken = window.sessionStorage.getItem("promptform-access-token") ?? "";
    setToken(savedToken);
    if (savedToken) void loadRuns(savedToken);
  }, [loadRuns]);

  useEffect(() => {
    if (!runs.some((run) => run.status === "RUNNING") || !token) return;
    const interval = window.setInterval(() => void loadRuns(), 3000);
    return () => window.clearInterval(interval);
  }, [loadRuns, runs, token]);

  function saveToken() {
    window.sessionStorage.setItem("promptform-access-token", token.trim());
    void loadRuns(token.trim());
  }

  const latestCompleted = runs.find((run) => run.status === "COMPLETED");
  const averagePassRate = latestCompleted?.passRate ?? 0;

  return (
    <main className="eval-shell">
      <header className="eval-header">
        <div>
          <p className="eyebrow">phase 05 / quality control</p>
          <h1>Evaluation runs.</h1>
          <p>Track whether generated forms are valid, renderable, and faithful to the brief.</p>
        </div>
        <button
          className="eval-start"
          disabled={!token || isLoading}
          onClick={() => void handleStart()}
          type="button"
        >
          {isLoading ? "Working..." : "Run 31 prompts"} <span>↗</span>
        </button>
      </header>

      <section className="eval-access" aria-label="Evaluation access token">
        <label htmlFor="eval-token">Developer access token</label>
        <div>
          <input
            id="eval-token"
            onChange={(event) => setToken(event.target.value)}
            placeholder="Paste a JWT from /api/auth/login"
            type="password"
            value={token}
          />
          <button onClick={saveToken} type="button">
            Connect
          </button>
        </div>
      </section>

      {error ? (
        <p className="eval-error" role="alert">
          {error}
        </p>
      ) : null}

      <section className="eval-stats" aria-label="Evaluation summary">
        <article>
          <span>Latest pass rate</span>
          <strong>{latestCompleted ? percent(averagePassRate) : "—"}</strong>
          <small>after schema + requirement checks</small>
        </article>
        <article>
          <span>Cases in suite</span>
          <strong>31</strong>
          <small>versioned prompts in the current set</small>
        </article>
        <article>
          <span>Runs tracked</span>
          <strong>{runs.length}</strong>
          <small>most recent 20 runs retained in view</small>
        </article>
      </section>

      <section className="eval-table-wrap" aria-label="Evaluation history">
        <div className="eval-table-heading">
          <span>Run history</span>
          <span>status / coverage / score</span>
        </div>
        {runs.length === 0 ? (
          <div className="eval-empty">
            <strong>No evaluation runs yet.</strong>
            <span>
              Connect a developer token, then start the suite to create the first benchmark.
            </span>
          </div>
        ) : (
          runs.map((run) => (
            <div className="eval-row" key={run.id}>
              <div className="eval-run-id">
                <span className={`eval-dot ${run.status.toLowerCase()}`} />{" "}
                <strong>{run.id.slice(0, 8)}</strong>
                <small>{formatDate(run.startedAt)}</small>
              </div>
              <span className="eval-status">{run.status.toLowerCase()}</span>
              <span className="eval-coverage">
                {run.passedCases} / {run.totalCases}
              </span>
              <strong className="eval-score">
                {run.status === "COMPLETED" ? percent(run.passRate) : "—"}
              </strong>
            </div>
          ))
        )}
      </section>
    </main>
  );
}
