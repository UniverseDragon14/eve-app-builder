export type SafeResult<T> =
  | { ok: true; data: T }
  | { ok: false; error: string };

export async function safeCall<T>(
  name: string,
  fn: () => Promise<T>
): Promise<SafeResult<T>> {
  try {
    const data = await fn();
    return { ok: true, data };
  } catch (err) {
    const error = err instanceof Error ? err.message : String(err);
    console.error(`[EVE ERROR] ${name}:`, error);
    return { ok: false, error };
  }
}

export function rollbackPoint(label: string) {
  return {
    label,
    createdAt: new Date().toISOString(),
    status: "ready",
  };
}
