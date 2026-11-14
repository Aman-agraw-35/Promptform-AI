"use client";

import { useState } from "react";
import { generateSchema } from "@/lib/generation-api";
import type { UiSchema } from "@/lib/schema";
import { FormRenderer } from "@/components/form-renderer";

const starterPrompt =
  "Create an insurance claim form with the customer's name, email, vehicle type, and incident description.";

export function PromptWorkbench() {
  const [prompt, setPrompt] = useState(starterPrompt);
  const [schema, setSchema] = useState<UiSchema | null>(null);
  const [status, setStatus] = useState("Ready to build");
  const [error, setError] = useState<string | null>(null);

  async function handleGenerate() {
    if (!prompt.trim()) return;
    setError(null);
    setStatus("Reading your brief…");
    try {
      await generateSchema(prompt, (event) => {
        if (event.event === "progress" && event.data.message) setStatus(event.data.message);
        if (event.event === "error") throw new Error(event.data.message ?? "Generation failed.");
        if (event.event === "result" && event.data.schema) {
          setSchema(event.data.schema);
          setStatus("Form ready");
        }
      });
    } catch (generationError) {
      setStatus("Could not build the form");
      setError(
        generationError instanceof Error ? generationError.message : "Something went wrong.",
      );
    }
  }

  return (
    <main className="shell">
      <header className="topbar">
        <a className="brand" href="/" aria-label="Promptform home">
          <span className="brand-mark">P</span>promptform
        </a>
        <span className="topbar-note">AI form builder</span>
      </header>
      <section className="intro">
        <p className="eyebrow">Describe it. Promptform builds it.</p>
        <h1>Turn a plain-English brief into a working form.</h1>
        <p className="intro-copy">
          Start with an idea. Get a structured, editable form in seconds.
        </p>
      </section>
      <section className="workbench" aria-label="Promptform builder">
        <div className="prompt-panel panel">
          <div className="panel-heading">
            <div>
              <p className="eyebrow">01 / Brief</p>
              <h2>What are you building?</h2>
            </div>
            <span className="status-dot" aria-label={status} />
          </div>
          <label className="sr-only" htmlFor="prompt">
            Describe the form you want to build
          </label>
          <textarea
            id="prompt"
            value={prompt}
            onChange={(event) => setPrompt(event.target.value)}
            rows={8}
          />
          <div className="prompt-footer">
            <span>Be specific about fields and choices</span>
            <button className="build-button" onClick={handleGenerate} type="button">
              Build form <span aria-hidden="true">↗</span>
            </button>
          </div>
          {error ? (
            <p className="error-message" role="alert">
              {error}
            </p>
          ) : null}
        </div>
        <div className="preview-panel panel">
          <div className="panel-heading">
            <div>
              <p className="eyebrow">02 / Preview</p>
              <h2>Your form appears here</h2>
            </div>
            <span className="status-label">{status}</span>
          </div>
          {schema ? (
            <FormRenderer schema={schema} />
          ) : (
            <div className="empty-preview">
              <div className="empty-icon">✦</div>
              <p>Your validated form will appear here.</p>
              <span>Try the example prompt, then make it your own.</span>
            </div>
          )}
        </div>
      </section>
      <footer className="footer">
        <span>Promptform AI</span>
        <span>Schema-first generation</span>
      </footer>
    </main>
  );
}
