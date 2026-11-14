export type FieldType = "text" | "number" | "textarea" | "select" | "radio" | "checkboxGroup";

export type FormOption = {
  value: string;
  label: string;
};

export type UiNode = {
  type: FieldType | "section";
  name?: string;
  label: string;
  description?: string;
  required?: boolean;
  options?: FormOption[];
  children?: UiNode[];
};

export type UiSchema = {
  title: string;
  description?: string;
  children: UiNode[];
};
