"use client";

import { useForm } from "react-hook-form";
import type { UiNode, UiSchema } from "@/features/form-builder/lib/schema";

function Field({
  node,
  register,
}: {
  node: UiNode;
  register: ReturnType<typeof useForm>["register"];
}) {
  if (node.type === "section") {
    return (
      <fieldset className="form-section">
        <legend>{node.label}</legend>
        {node.children?.map((child) => (
          <Field key={child.name ?? child.label} node={child} register={register} />
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
              <input type="checkbox" value={option.value} {...inputProps} />
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
  const { register } = useForm();
  return (
    <form className="rendered-form" onSubmit={(event) => event.preventDefault()}>
      <div className="form-heading">
        <p className="eyebrow">Live preview</p>
        <h2>{schema.title}</h2>
        {schema.description ? <p>{schema.description}</p> : null}
      </div>
      {schema.children.map((node) => (
        <Field key={node.name ?? node.label} node={node} register={register} />
      ))}
      <button className="submit-button" type="submit">
        Submit form
      </button>
    </form>
  );
}
