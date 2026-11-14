export type FieldType = "text" | "number" | "textarea" | "select" | "radio" | "checkboxGroup";

export type VisibleWhen = {
  field: string;
  equals?: string;
  notEquals?: string;
  in?: string[];
};

export type ValidationRules = {
  min?: number;
  max?: number;
  pattern?: string;
};

export type FormOption = {
  value: string;
  label: string;
};

export type UiNode = {
  type: FieldType | "section" | "columns";
  name?: string;
  label: string;
  description?: string;
  required?: boolean;
  defaultValue?: unknown;
  options?: FormOption[];
  children?: UiNode[];
  visibleWhen?: VisibleWhen;
  validation?: ValidationRules;
};

export type UiSchema = {
  title: string;
  description?: string;
  children: UiNode[];
};
