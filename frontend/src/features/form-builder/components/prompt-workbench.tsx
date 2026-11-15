"use client";

import Link from "next/link";
import { useState } from "react";
import { FormRenderer } from "@/features/form-builder/components/form-renderer";
import { generateSchema } from "@/features/form-builder/lib/generation-api";
import type { UiSchema } from "@/features/form-builder/lib/schema";

const starterPrompt =
  "Create an insurance claim form with the customer's name, email, vehicle type, and incident description.";

const promptIdeas = [
  "Equipment rental intake",
  "Travel reimbursement request",
  "Employee onboarding checklist",
];

export function PromptWorkbench() {
  const [prompt, setPrompt] = useState(starterPrompt);
  const [schema, setSchema] = useState<UiSchema | null>(null);
  const [status, setStatus] = useState("Ready to build");
  const [error, setError] = useState<string | null>(null);
  const [isGenerating, setIsGenerating] = useState(false);

  async function handleGenerate() {
    if (!prompt.trim() || isGenerating) {
      console.debug("[promptform] generation click ignored", {
        hasPrompt: Boolean(prompt.trim()),
        isGenerating,
      });
      return;
    }
    console.info("[promptform] build form clicked", { promptChars: prompt.trim().length });
    setError(null);
    setIsGenerating(true);
    setStatus("Reading your brief...");

    try {
      await generateSchema(prompt, (event) => {
        console.info("[promptform] generation event handled", { event: event.event });
        if (event.event === "progress" && event.data.message) setStatus(event.data.message);
        if (event.event === "error") throw new Error(event.data.message ?? "Generation failed.");
        if (event.event === "result" && event.data.schema) {
          setSchema(event.data.schema);
          setStatus("Form ready");
        }
      });
    } catch (generationError) {
      console.error("[promptform] generation failed", generationError);
      setStatus("Build interrupted");
      setError(
        generationError instanceof Error ? generationError.message : "Something went wrong.",
      );
    } finally {
      setIsGenerating(false);
    }
  }

  function handlePromptKeyDown(event: React.KeyboardEvent<HTMLTextAreaElement>) {
    if ((event.metaKey || event.ctrlKey) && event.key === "Enter") {
      event.preventDefault();
      void handleGenerate();
    }
  }

  return (
    <main className="shell">
      <div className="ambient ambient-one" aria-hidden="true" />
      <div className="ambient ambient-two" aria-hidden="true" />

      <header className="topbar">
        <Link className="brand" href="/" aria-label="Promptform home">
          <span className="brand-mark">P</span>
          <span>promptform</span>
        </Link>
        <div className="topbar-meta">
          <Link className="topbar-evals" href="/evals">
            quality / evals ↗
          </Link>
          <span className="availability">
            <i /> online
          </span>
          <span className="topbar-note">AI form studio / 01</span>
        </div>
      </header>

      <section className="intro" aria-labelledby="page-title">
        <div className="intro-kicker">
          <span>01</span> schema-first creation
        </div>
        <h1 id="page-title">
          Make the brief
          <br />
          <em>click.</em>
        </h1>
        <div className="intro-bottomline">
          <p className="intro-copy">
            Describe the workflow in plain English.
            <br />
            Leave with a form your team can use.
          </p>
          <span className="intro-index">PROMPTFORM / 2026</span>
        </div>
        <div className="studio-rail" aria-label="Studio capabilities">
          <span>
            <b>01</b> natural language
          </span>
          <span>
            <b>02</b> validated schema
          </span>
          <span>
            <b>03</b> live interface
          </span>
        </div>
      </section>

      <section className="workbench" aria-label="Promptform builder">
        <div className="prompt-panel panel">
          <div className="panel-topline">
            <div className="panel-label">
              <span className="panel-number">01</span> brief
            </div>
            <span className="shortcut">
              <kbd>⌘</kbd>
              <kbd>↵</kbd>
            </span>
          </div>
          <div className="prompt-copy">
            <h2>What are you building?</h2>
            <p>Give the generator the job, the audience, and the decisions it needs to capture.</p>
          </div>
          <label className="sr-only" htmlFor="prompt">
            Describe the form you want to build
          </label>
          <textarea
            id="prompt"
            value={prompt}
            onChange={(event) => setPrompt(event.target.value)}
            onKeyDown={handlePromptKeyDown}
            placeholder="A form for..."
            rows={8}
          />
          <div className="idea-row" aria-label="Prompt examples">
            <span className="idea-label">Try</span>
            {promptIdeas.map((idea) => (
              <button
                key={idea}
                type="button"
                onClick={() => setPrompt(`Create a ${idea.toLowerCase()} form.`)}
              >
                {idea}
              </button>
            ))}
          </div>
          <div className="prompt-footer">
            <span className="input-hint">{prompt.length} chars / be specific</span>
            <button
              className="build-button"
              disabled={isGenerating || !prompt.trim()}
              onClick={() => void handleGenerate()}
              type="button"
            >
              <span>{isGenerating ? "Building" : "Build form"}</span>
              <span className="button-arrow" aria-hidden="true">
                ↗
              </span>
            </button>
          </div>
          {error ? (
            <p className="error-message" role="alert">
              {error}
            </p>
          ) : null}
        </div>

        <div className="preview-panel panel" aria-busy={isGenerating}>
          <div className="panel-topline">
            <div className="panel-label">
              <span className="panel-number">02</span> live preview
            </div>
            <div className="preview-status">
              <span className={isGenerating ? "status-dot is-active" : "status-dot"} />
              {status}
            </div>
          </div>
          {schema ? (
            <FormRenderer schema={schema} />
          ) : (
            <div className="empty-preview">
              <div className="preview-orbit" aria-hidden="true">
                <div className="orbit-ring ring-one" />
                <div className="orbit-ring ring-two" />
                <span className="orbit-core">P</span>
              </div>
              <p className="empty-title">
                Your interface
                <br />
                starts here.
              </p>
              <span className="empty-copy">A validated form will appear in this space.</span>
              <div className="empty-tags">
                <span>JSON</span>
                <span>SAFE</span>
                <span>READY</span>
              </div>
              <div className="empty-footnote">
                <span />
                <span>waiting for a brief</span>
              </div>
            </div>
          )}
        </div>
      </section>

      <footer className="footer">
        <span>
          <b>PF</b> promptform studio
        </span>
        <span>
          Validated output <i className="footer-pulse" />
        </span>
        <span className="footer-right">Built for people who care about the details ↗</span>
      </footer>
    </main>
  );
}
