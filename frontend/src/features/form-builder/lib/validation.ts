import { z } from "zod";
import { isNodeVisible, type FormValues } from "@/features/form-builder/lib/conditions";
import type { UiNode, UiSchema } from "@/features/form-builder/lib/schema";

function visibleFields(nodes: UiNode[], values: FormValues): UiNode[] {
  return nodes.flatMap((node) => {
    if (!isNodeVisible(node, values)) return [];
    if (node.children) return visibleFields(node.children, values);
    return node.name ? [node] : [];
  });
}

export function buildValidationSchema(schema: UiSchema, values: FormValues) {
  const shape: Record<string, z.ZodTypeAny> = {};

  for (const node of visibleFields(schema.children, values)) {
    const rules = node.validation;
    let field: z.ZodTypeAny;

    if (node.type === "number") {
      let numericField = z.number();
      if (rules?.min != null) numericField = numericField.min(rules.min, `Must be at least ${rules.min}.`);
      if (rules?.max != null) numericField = numericField.max(rules.max, `Must be at most ${rules.max}.`);
      field = z.preprocess(
        (value) => (value === "" || value === undefined || value === null ? undefined : Number(value)),
        numericField,
      );
    } else if (node.type === "checkboxGroup") {
      const optionValues = new Set((node.options ?? []).map((option) => option.value));
      const checkboxBase = node.required
        ? z.array(z.string()).min(1, "Choose at least one option.")
        : z.array(z.string());
      const checkboxArray = checkboxBase.superRefine((selected, context) => {
        if (selected.some((value) => !optionValues.has(value))) {
          context.addIssue({ code: z.ZodIssueCode.custom, message: "Choose only listed options." });
        }
      });
      field = z.preprocess(
        (value) => (value === undefined || value === null || value === "" ? [] : Array.isArray(value) ? value : [value]),
        checkboxArray,
      );
    } else {
      let textField = z.string();
      if (rules?.pattern) textField = textField.regex(new RegExp(rules.pattern), "Enter a valid value.");
      if (rules?.min != null) textField = textField.min(rules.min, `Must contain at least ${rules.min} characters.`);
      if (rules?.max != null) textField = textField.max(rules.max, `Must contain at most ${rules.max} characters.`);
      if (node.type === "select" || node.type === "radio") {
        const optionValues = new Set((node.options ?? []).map((option) => option.value));
        field = textField.refine(
          (value) => (!node.required && value === "") || optionValues.has(value),
          "Choose a listed option.",
        );
      } else {
        field = textField;
      }
    }

    if (!node.required) field = field.optional();

    shape[node.name!] = field;
  }

  return z.object(shape).passthrough();
}
