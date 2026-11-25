"use client";

import Link from "next/link";
import { useCallback, useEffect, useMemo, useState } from "react";
import type { UiNode, UiSchema } from "@/features/form-builder/lib/schema";

type FormResponse = { id: string; schema: UiSchema; uniqueFieldName: string; approvedAt: string; sharePath: string };
type Submission = { id: string; submittedAt: string; values: Record<string, unknown> };

function fields(nodes: UiNode[]): UiNode[] {
  return nodes.flatMap((node) => node.children ? fields(node.children) : node.name ? [node] : []);
}

function displayValue(value: unknown) {
  if (value === null || value === undefined || value === "") return "—";
  if (Array.isArray(value)) return value.join(", ");
  if (typeof value === "object") return JSON.stringify(value);
  return String(value);
}

function csvValue(value: unknown) {
  return `"${displayValue(value).replaceAll('"', '""')}"`;
}

export function AdminFormPage({ formId }: { formId: string }) {
  const [form, setForm] = useState<FormResponse | null>(null);
  const [submissions, setSubmissions] = useState<Submission[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [copied, setCopied] = useState(false);
  const apiBase = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

  const load = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const [formResponse, submissionsResponse] = await Promise.all([
        fetch(`${apiBase}/api/forms/${formId}`),
        fetch(`${apiBase}/api/forms/${formId}/submissions`),
      ]);
      if (!formResponse.ok || !submissionsResponse.ok) throw new Error("The form responses could not be loaded.");
      setForm(await formResponse.json() as FormResponse);
      setSubmissions(await submissionsResponse.json() as Submission[]);
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : "The form responses could not be loaded.");
    } finally {
      setIsLoading(false);
    }
  }, [apiBase, formId]);

  useEffect(() => { void load(); }, [load]);

  const formFields = useMemo(() => form ? fields(form.schema.children) : [], [form]);
  const publicUrl = form && typeof window !== "undefined" ? `${window.location.origin}${form.sharePath}` : "";

  async function copyLink() {
    if (!publicUrl) return;
    await navigator.clipboard.writeText(publicUrl);
    setCopied(true);
    window.setTimeout(() => setCopied(false), 1800);
  }

  function exportCsv() {
    if (!form) return;
    const columns = formFields.map((field) => field.name!);
    const header = ["Submitted at", ...formFields.map((field) => field.label)].map(csvValue).join(",");
    const rows = submissions.map((submission) => [
      submission.submittedAt,
      ...columns.map((column) => submission.values[column]),
    ].map(csvValue).join(","));
    const blob = new Blob([[header, ...rows].join("\n")], { type: "text/csv;charset=utf-8" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = `${form.schema.title.toLowerCase().replace(/[^a-z0-9]+/g, "-")}-responses.csv`;
    link.click();
    URL.revokeObjectURL(url);
  }

  if (isLoading) return <main className="admin-shell"><p className="admin-loading">Loading responses...</p></main>;
  if (error || !form) return <main className="admin-shell"><p className="error-message">{error ?? "Form not found."}</p></main>;

  return <main className="admin-shell">
    <header className="admin-header">
      <div><Link className="admin-back" href="/">← Promptform studio</Link><p className="admin-eyebrow">Response dashboard</p><h1>{form.schema.title}</h1><p>{form.schema.description ?? "Review and export responses from your approved form."}</p></div>
      <div className="admin-actions"><button type="button" onClick={() => void copyLink()}>{copied ? "Link copied" : "Copy form link"}</button><Link href={form.sharePath} target="_blank">Open form</Link><button type="button" onClick={exportCsv} disabled={!submissions.length}>Export CSV</button></div>
    </header>
    <section className="admin-stats"><div><span>Responses</span><strong>{submissions.length}</strong></div><div><span>Unique field</span><strong>{form.uniqueFieldName}</strong></div><div><span>Approved</span><strong>{new Date(form.approvedAt).toLocaleDateString()}</strong></div></section>
    <section className="responses-panel"><div className="responses-heading"><div><p className="admin-eyebrow">Submissions</p><h2>All responses</h2></div><button className="refresh-button" type="button" onClick={() => void load()}>Refresh</button></div>
      {submissions.length === 0 ? <div className="empty-responses"><strong>No responses yet</strong><span>Share the form link to start collecting answers.</span></div> : <div className="responses-table-wrap"><table className="responses-table"><thead><tr><th>Submitted</th>{formFields.map((field) => <th key={field.name}>{field.label}</th>)}</tr></thead><tbody>{submissions.map((submission) => <tr key={submission.id}><td>{new Date(submission.submittedAt).toLocaleString()}</td>{formFields.map((field) => <td key={field.name}>{displayValue(submission.values[field.name!])}</td>)}</tr>)}</tbody></table></div>}
    </section>
  </main>;
}
