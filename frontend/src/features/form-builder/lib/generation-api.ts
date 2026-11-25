import type { UiSchema } from "@/features/form-builder/lib/schema";

type StreamEvent = {
  event: string;
  data: { message?: string; schema?: UiSchema };
};

export async function generateSchema(prompt: string, onEvent: (event: StreamEvent) => void) {
  const apiUrl = `${process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080"}/api/generate`;
  console.info("[promptform] generation request started", { apiUrl, promptChars: prompt.length });
  const response = await fetch(apiUrl, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ prompt }),
  });

  console.info("[promptform] generation response received", {
    status: response.status,
    contentType: response.headers.get("content-type"),
  });
  if (!response.ok || !response.body) {
    console.error("[promptform] generation request failed before SSE", { status: response.status });
    throw new Error("The generation service is unavailable.");
  }

  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  let buffer = "";

  while (true) {
    const { value, done } = await reader.read();
    buffer += decoder.decode(value ?? new Uint8Array(), { stream: !done });
    const messages = buffer.split("\n\n");
    buffer = messages.pop() ?? "";
    for (const message of messages) {
      const event = message.match(/^event:\s*(.+)$/m)?.[1] ?? "message";
      const data = message.match(/^data:\s*(.+)$/m)?.[1];
      if (data) {
        const parsedData = JSON.parse(data);
        console.info("[promptform] generation SSE event", { event, data: parsedData });
        onEvent({ event, data: parsedData });
      }
    }
    if (done) break;
  }
  console.info("[promptform] generation stream closed");
}
