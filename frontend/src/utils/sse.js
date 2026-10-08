/**
 * Reads a Server-Sent Events response body and calls onEvent(name, data)
 * for every complete event. Uses fetch instead of EventSource so the
 * Authorization header can be sent.
 */
export async function readEventStream(body, onEvent) {
  const reader = body.getReader();
  const decoder = new TextDecoder();
  let buffer = "";

  while (true) {
    const { value, done } = await reader.read();
    if (done) {
      return;
    }

    buffer += decoder.decode(value, { stream: true });
    const events = buffer.split(/\r?\n\r?\n/);
    buffer = events.pop() ?? "";

    for (const rawEvent of events) {
      const lines = rawEvent.split(/\r?\n/);
      const dataLines = lines.filter((line) => line.startsWith("data:"));
      if (dataLines.length === 0) {
        continue;
      }
      const nameLine = lines.find((line) => line.startsWith("event:"));
      const name = nameLine ? nameLine.slice(6).trim() : "message";
      const data = dataLines.map((line) => line.slice(5).trim()).join("\n");
      onEvent(name, data);
    }
  }
}
