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
      let numericField = z.coerce.number();
      if (rules?.min !== undefined) numericField = numericField.min(rules.min);
      if (rules?.max !== undefined) numericField = numericField.max(rules.max);
      field = numericField;
    } else {
      let textField = z.string();
      if (rules?.pattern) textField = textField.regex(new RegExp(rules.pattern));
      if (rules?.min !== undefined) textField = textField.min(rules.min);
      if (rules?.max !== undefined) textField = textField.max(rules.max);
      field = textField;
    }

    if (!node.required) field = field.optional();

    shape[node.name!] = field;
  }

  return z.object(shape).passthrough();
}
