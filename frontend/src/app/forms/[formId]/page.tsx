import { PublicFormClient } from "./public-form-client";

export default async function PublicFormPage({ params }: { params: Promise<{ formId: string }> }) {
  const { formId } = await params;
  return <PublicFormClient formId={formId} />;
}
