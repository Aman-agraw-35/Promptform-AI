export type FieldType = "text" | "number" | "textarea" | "select" | "radio" | "checkboxGroup";

export type VisibleWhen = {
  field: string;
  equals?: string;
  notEquals?: string;
  in?: string[];
};

export type ValidationRules = {
  min?: number | null;
  max?: number | null;
  pattern?: string | null;
};

export type FormOption = {
  value: string;
  label: string;
};

export type UiNode = {
  type: FieldType | "section" | "columns";
  name?: string;
  label: string;
  description?: string | null;
  required?: boolean | null;
  unique?: boolean | null;
  defaultValue?: unknown;
  options?: FormOption[];
  children?: UiNode[];
  visibleWhen?: VisibleWhen | null;
  validation?: ValidationRules | null;
};

export type UiSchema = {
  title: string;
  description?: string | null;
  children: UiNode[];
};
