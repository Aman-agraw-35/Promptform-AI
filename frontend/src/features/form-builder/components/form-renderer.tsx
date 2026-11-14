"use client";

import { useState } from "react";
import { useForm } from "react-hook-form";
import { useWatch } from "react-hook-form";
import { isNodeVisible, type FormValues } from "@/features/form-builder/lib/conditions";
import { buildValidationSchema } from "@/features/form-builder/lib/validation";
import type { UiNode, UiSchema } from "@/features/form-builder/lib/schema";

function Field({
  node,
  register,
  values,
}: {
  node: UiNode;
  register: ReturnType<typeof useForm>["register"];
  values: FormValues;
}) {
  if (!isNodeVisible(node, values)) return null;

  if (node.type === "section" || node.type === "columns") {
    return (
      <fieldset className={node.type === "columns" ? "form-columns" : "form-section"}>
        <legend>{node.label}</legend>
        {node.children?.map((child) => (
          <Field key={child.name ?? child.label} node={child} register={register} values={values} />
        ))}
      </fieldset>
    );
  }

  const inputProps = { ...register(node.name ?? "field", { required: node.required }) };
  const options = node.options ?? [];
  return (
    <div className="field">
      <label htmlFor={node.name}>
        {node.label}
        {node.required ? <span aria-hidden="true"> *</span> : null}
      </label>
      {node.description ? <p className="field-description">{node.description}</p> : null}
      {node.type === "select" ? (
        <select id={node.name} {...inputProps} defaultValue="">
          <option value="" disabled>
            Select an option
          </option>
          {options.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
      ) : node.type === "radio" ? (
        <div className="option-list">
          {options.map((option) => (
            <label className="option" key={option.value}>
              <input type="radio" value={option.value} {...inputProps} />
              {option.label}
            </label>
          ))}
        </div>
      ) : node.type === "checkboxGroup" ? (
        <div className="option-list">
          {options.map((option) => (
            <label className="option" key={option.value}>
              <input
                id={`${node.name}-${option.value}`}
                type="checkbox"
                value={option.value}
                {...inputProps}
              />
              {option.label}
            </label>
          ))}
        </div>
      ) : node.type === "textarea" ? (
        <textarea id={node.name} {...inputProps} rows={4} />
      ) : (
        <input id={node.name} type={node.type === "number" ? "number" : "text"} {...inputProps} />
      )}
    </div>
  );
}

export function FormRenderer({ schema }: { schema: UiSchema }) {
  const { control, handleSubmit, register } = useForm<FormValues>();
  const values = useWatch({ control });
  const [submitError, setSubmitError] = useState<string | null>(null);

  function submit(values: FormValues) {
    const result = buildValidationSchema(schema, values).safeParse(values);
    setSubmitError(
      result.success ? null : (result.error.issues[0]?.message ?? "Check the form fields."),
    );
  }

  return (
    <form className="rendered-form" onSubmit={handleSubmit(submit)}>
      <div className="form-heading">
        <p className="eyebrow">Live preview</p>
        <h2>{schema.title}</h2>
        {schema.description ? <p>{schema.description}</p> : null}
      </div>
      {schema.children.map((node) => (
        <Field key={node.name ?? node.label} node={node} register={register} values={values} />
      ))}
      {submitError ? (
        <p className="error-message" role="alert">
          {submitError}
        </p>
      ) : null}
      <button className="submit-button" type="submit">
        Submit form
      </button>
    </form>
  );
}
