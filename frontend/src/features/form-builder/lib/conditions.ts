import type { UiNode } from "@/features/form-builder/lib/schema";

export type FormValues = Record<string, unknown>;

export function isNodeVisible(node: UiNode, values: FormValues): boolean {
  const condition = node.visibleWhen;
  if (!condition) return true;

  const actualValue = String(values[condition.field] ?? "");
  if (condition.equals !== undefined) return actualValue === condition.equals;
  if (condition.notEquals !== undefined) return actualValue !== condition.notEquals;
  if (condition.in !== undefined) return condition.in.includes(actualValue);
  return false;
}
