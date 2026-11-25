"use client";

import Link from "next/link";
import { FormEvent, useState } from "react";
import { login, register } from "@/features/auth/lib/auth-api";

type AuthMode = "login" | "signup";

export function AuthPage({ mode }: { mode: AuthMode }) {
  const isSignup = mode === "signup";
  const [displayName, setDisplayName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    if (isSignup && password !== confirmPassword) {
      setError("Passwords do not match.");
      return;
    }

    setIsSubmitting(true);
    try {
      if (isSignup) await register(email.trim(), password, displayName.trim());
      else await login(email.trim(), password);
      window.location.assign("/");
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Authentication failed.");
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <main className="auth-shell">
      <div className="auth-layout">
        <section className="auth-story" aria-label="Promptform introduction">
          <Link className="brand auth-brand" href="/">
            <span className="brand-mark">p</span>
            <span>promptform ai</span>
          </Link>
          <p className="auth-kicker">forms from plain language</p>
          <h1>Build forms that feel <em>obvious.</em></h1>
          <p className="auth-story-copy">
            Create, publish, and understand every response from one focused workspace.
          </p>
          <div className="auth-story-line"><span /> secure workspace access</div>
        </section>

        <section className="auth-card" aria-labelledby="auth-title">
          <div className="auth-card-topline">
            <span>{isSignup ? "Create workspace" : "Welcome back"}</span>
            <span>01 / 02</span>
          </div>
          <h2 id="auth-title">{isSignup ? "Start building." : "Sign in."}</h2>
          <p className="auth-subtitle">
            {isSignup ? "Set up your account to create and manage forms." : "Continue to your form workspace."}
          </p>

          <form className="auth-form" onSubmit={(event) => void handleSubmit(event)}>
            {isSignup ? (
              <label>
                Display name
                <input autoComplete="name" onChange={(event) => setDisplayName(event.target.value)} required value={displayName} />
              </label>
            ) : null}
            <label>
              Email address
              <input autoComplete="email" onChange={(event) => setEmail(event.target.value)} required type="email" value={email} />
            </label>
            <label>
              Password
              <input autoComplete={isSignup ? "new-password" : "current-password"} minLength={8} onChange={(event) => setPassword(event.target.value)} required type="password" value={password} />
              {isSignup ? <span className="auth-field-hint">Use at least 8 characters.</span> : null}
            </label>
            {isSignup ? (
              <label>
                Confirm password
                <input autoComplete="new-password" minLength={8} onChange={(event) => setConfirmPassword(event.target.value)} required type="password" value={confirmPassword} />
              </label>
            ) : null}
            {error ? <p className="auth-error" role="alert">{error}</p> : null}
            <button className="auth-submit" disabled={isSubmitting} type="submit">
              {isSubmitting ? "Please wait..." : isSignup ? "Create account" : "Sign in"}
              <span>↗</span>
            </button>
          </form>

          <p className="auth-switch">
            {isSignup ? "Already have an account?" : "Need an account?"}{" "}
            <Link href={isSignup ? "/login" : "/signup"}>{isSignup ? "Sign in" : "Create one"}</Link>
          </p>
          <p className="auth-privacy">Your session is protected with an HttpOnly access cookie.</p>
        </section>
      </div>
    </main>
  );
}
