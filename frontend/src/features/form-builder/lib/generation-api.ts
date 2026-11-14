import type { UiSchema } from "@/features/form-builder/lib/schema";

type StreamEvent = {
  event: string;
  data: { message?: string; schema?: UiSchema };
};

export async function generateSchema(prompt: string, onEvent: (event: StreamEvent) => void) {
  const response = await fetch(
    `${process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080"}/api/generate`,
    {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ prompt }),
    },
  );

  if (!response.ok || !response.body) throw new Error("The generation service is unavailable.");

  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  let buffer = "";

  while (true) {
    const { value, done } = await reader.read();
    buffer += decoder.decode(value ?? new Uint8Array(), { stream: !done });
    const messages = buffer.split("\n\n");
    buffer = messages.pop() ?? "";
    for (const message of messages) {
      const event = message.match(/^event: (.+)$/m)?.[1] ?? "message";
      const data = message.match(/^data: (.+)$/m)?.[1];
      if (data) onEvent({ event, data: JSON.parse(data) });
    }
    if (done) break;
  }
}
