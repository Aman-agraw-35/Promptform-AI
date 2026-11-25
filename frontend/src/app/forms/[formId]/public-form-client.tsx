"use client";

import { useEffect, useState } from "react";
import { FormRenderer } from "@/features/form-builder/components/form-renderer";
import type { UiSchema } from "@/features/form-builder/lib/schema";

export function PublicFormClient({ formId }: { formId: string }) {
  const [schema, setSchema] = useState<UiSchema | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [submitted, setSubmitted] = useState(false);

  useEffect(() => {
    fetch(`${process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080"}/api/forms/${formId}`)
      .then(async (response) => {
        if (!response.ok) throw new Error("This approved form could not be found.");
        return response.json() as Promise<{ schema: UiSchema }>;
      })
      .then((form) => setSchema(form.schema))
      .catch((requestError) => setError(requestError instanceof Error ? requestError.message : "The form could not be loaded."));
  }, [formId]);

  if (error) return <main className="public-form-shell"><p className="error-message">{error}</p></main>;
  if (submitted) return <main className="public-form-shell"><section className="submission-success" role="status" aria-live="polite">
    <div className="submission-success-icon" aria-hidden="true">✓</div>
    <p className="submission-success-eyebrow">Submission complete</p>
    <h1>Form submitted successfully</h1>
    <p>Thank you. Your response has been recorded.</p>
  </section></main>;
  if (!schema) return <main className="public-form-shell"><p className="preview-status">Loading form...</p></main>;
  return <main className="public-form-shell"><FormRenderer mode="respondent" formId={formId} schema={schema} onSubmitted={() => setSubmitted(true)} /></main>;
}
