import { AdminFormPage } from "./admin-form-page";

export default async function AdminFormResponsesPage({ params }: { params: Promise<{ formId: string }> }) {
  const { formId } = await params;
  return <AdminFormPage formId={formId} />;
}
