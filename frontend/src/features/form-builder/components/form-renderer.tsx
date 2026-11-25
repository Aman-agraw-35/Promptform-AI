"use client";

import { useState } from "react";
import { useForm, useWatch, type UseFormRegister } from "react-hook-form";
import { isNodeVisible, type FormValues } from "@/features/form-builder/lib/conditions";
import { buildValidationSchema } from "@/features/form-builder/lib/validation";
import type { UiNode, UiSchema } from "@/features/form-builder/lib/schema";

type FormMode = "builder" | "respondent";
type FormRendererProps = { schema: UiSchema; mode?: FormMode; formId?: string; onSchemaChange?: (schema: UiSchema) => void; onSubmitted?: () => void };
type ApprovedForm = { id: string; sharePath: string };

async function readApiError(response: Response, fallback: string) {
  try {
    const problem = (await response.json()) as { detail?: string; message?: string };
    return new Error(problem.detail ?? problem.message ?? fallback);
  } catch {
    return new Error(fallback);
  }
}

async function approveForm(schema: UiSchema) {
  const response = await fetch(`${process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080"}/api/forms`, {
    method: "POST", credentials: "include", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ schema }),
  });
  if (!response.ok) throw await readApiError(response, "The form could not be approved.");
  return response.json() as Promise<ApprovedForm>;
}

async function saveSubmission(formId: string, values: FormValues) {
  const response = await fetch(`${process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080"}/api/forms/${formId}/submissions`, {
    method: "POST", credentials: "include", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ values }),
  });
  if (!response.ok) throw await readApiError(response, "The form submission could not be saved.");
  return response.json() as Promise<{ id: string; submittedAt: string }>;
}

function updateNode(nodes: UiNode[], fieldName: string, update: (node: UiNode) => UiNode): UiNode[] {
  return nodes.map((node) => {
    const withChildren = node.children ? { ...node, children: updateNode(node.children, fieldName, update) } : node;
    return withChildren.name === fieldName ? update(withChildren) : withChildren;
  });
}

function removeNode(nodes: UiNode[], fieldName: string): UiNode[] {
  return nodes.filter((node) => node.name !== fieldName)
    .map((node) => node.children ? { ...node, children: removeNode(node.children, fieldName) } : node);
}

function Field({ node, mode, register, values, onRequiredChange, onUniqueChange, onRemove }: {
  node: UiNode; mode: FormMode; register: UseFormRegister<FormValues>; values: FormValues;
  onRequiredChange: (name: string) => void; onUniqueChange: (name: string) => void; onRemove: (name: string) => void;
}) {
  if (!isNodeVisible(node, values)) return null;
  if (node.type === "section" || node.type === "columns") {
    return <fieldset className={node.type === "columns" ? "form-columns" : "form-section"}>
      <legend>{node.label}</legend>
      {node.children?.map((child) => <Field key={child.name ?? child.label} node={child} mode={mode} register={register} values={values}
        onRequiredChange={onRequiredChange} onUniqueChange={onUniqueChange} onRemove={onRemove} />)}
    </fieldset>;
  }

  const inputProps = { ...register(node.name ?? "field", {
    required: mode === "respondent" && node.type !== "checkboxGroup" ? Boolean(node.required) : false,
  }) };
  const options = node.options ?? [];
  return <div className="field">
    <div className="field-heading">
      <label htmlFor={node.name}>{node.label}{node.required ? <span aria-hidden="true"> *</span> : null}</label>
      {mode === "builder" && node.name ? <div className="field-controls" aria-label={`Controls for ${node.label}`}>
        <label className="field-control"><input type="checkbox" checked={Boolean(node.required)} onChange={() => onRequiredChange(node.name!)} />Required</label>
        <label className="field-control"><input type="checkbox" checked={Boolean(node.unique)} onChange={() => onUniqueChange(node.name!)} />Unique</label>
        <button type="button" className="remove-field" onClick={() => onRemove(node.name!)}>Remove</button>
      </div> : null}
    </div>
    {node.description ? <p className="field-description">{node.description}</p> : null}
    {node.type === "select" ? <select id={node.name} {...inputProps} defaultValue=""><option value="" disabled>Select an option</option>{options.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}</select>
      : node.type === "radio" ? <div className="option-list">{options.map((option) => <label className="option" key={option.value}><input type="radio" value={option.value} {...inputProps} />{option.label}</label>)}</div>
      : node.type === "checkboxGroup" ? <div className="option-list">{options.map((option) => <label className="option" key={option.value}><input id={`${node.name}-${option.value}`} type="checkbox" value={option.value} defaultChecked={Array.isArray(node.defaultValue) && node.defaultValue.includes(option.value)} {...inputProps} />{option.label}</label>)}</div>
      : node.type === "textarea" ? <textarea id={node.name} {...inputProps} rows={4} />
      : <input id={node.name} type={node.type === "number" ? "number" : "text"} {...inputProps} />}
  </div>;
}

export function FormRenderer({ schema, mode = "respondent", formId, onSchemaChange, onSubmitted }: FormRendererProps) {
  const { control, handleSubmit, register } = useForm<FormValues>();
  const values = useWatch({ control });
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [submitSuccess, setSubmitSuccess] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [approvedForm, setApprovedForm] = useState<ApprovedForm | null>(null);

  const changeSchema = (children: UiNode[]) => onSchemaChange?.({ ...schema, children });
  const toggleRequired = (name: string) => changeSchema(updateNode(schema.children, name, (node) => ({ ...node, required: !Boolean(node.required) })));
  const toggleUnique = (name: string) => {
    const changed = updateNode(schema.children, name, (node) => ({ ...node, unique: !Boolean(node.unique) }));
    const hasUnique = (nodes: UiNode[]): boolean => nodes.some((node) => node.name === name && node.unique)
      || nodes.some((node) => node.children ? hasUnique(node.children) : false);
    const selecting = hasUnique(changed);
    const clearOtherUnique = (nodes: UiNode[]): UiNode[] => nodes.map((node) => ({ ...node,
      unique: selecting && node.name !== name ? false : node.unique,
      children: node.children ? clearOtherUnique(node.children) : node.children,
    }));
    changeSchema(clearOtherUnique(changed));
  };
  const removeField = (name: string) => changeSchema(removeNode(schema.children, name));

  async function submit(values: FormValues) {
    setSubmitError(null); setSubmitSuccess(false); setIsSubmitting(true);
    try {
      if (mode === "builder") {
        setApprovedForm(await approveForm(schema));
      } else {
        if (!formId) throw new Error("This form is missing its public ID.");
        const result = buildValidationSchema(schema, values).safeParse(values);
        if (!result.success) throw new Error(result.error.issues[0]?.message ?? "Check the form fields.");
        await saveSubmission(formId, result.data);
        onSubmitted?.();
      }
      setSubmitSuccess(true);
    } catch (error) {
      console.error("[promptform] form action failed", error);
      setSubmitError(error instanceof Error ? error.message : "The request could not be completed.");
    } finally { setIsSubmitting(false); }
  }

  return <form className="rendered-form" onSubmit={handleSubmit(submit)}>
    <div className="form-heading"><p className="eyebrow">{mode === "builder" ? "Form layout" : "Live form"}</p><h2>{schema.title}</h2>{schema.description ? <p>{schema.description}</p> : null}</div>
    {schema.children.map((node) => <Field key={node.name ?? node.label} node={node} mode={mode} register={register} values={values}
      onRequiredChange={toggleRequired} onUniqueChange={toggleUnique} onRemove={removeField} />)}
    {mode === "builder" ? <p className="builder-hint">Mark exactly one field as Unique before approval. It can be an email, number, or any other field.</p> : null}
    {submitError ? <p className="error-message" role="alert">{submitError}</p> : null}
    {submitSuccess ? <p className="success-message" role="status">{mode === "builder" ? "Form approved successfully." : "Form submitted successfully."}</p> : null}
    {approvedForm ? <p className="share-message">Share this form: <a href={approvedForm.sharePath}>{approvedForm.sharePath}</a> · <a href={`/admin/forms/${approvedForm.id}`}>View responses</a></p> : null}
    <button className="submit-button" type="submit" disabled={isSubmitting}>{isSubmitting ? (mode === "builder" ? "Approving..." : "Saving...") : (mode === "builder" ? "Approve form" : "Submit form")}</button>
  </form>;
}
