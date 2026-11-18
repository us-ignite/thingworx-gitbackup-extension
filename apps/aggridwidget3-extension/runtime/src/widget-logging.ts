/** Emit a widget diagnostic while preserving each component's existing prefix. */
export function writeWidgetLog(
  enabled: unknown,
  scope: string,
  prefix: string,
  message: unknown,
): void {
  if (enabled) console.log(`${scope}${prefix}${String(message)}`);
}

/** Emit a message and its associated object together when diagnostics are enabled. */
export function writeWidgetLogObject(
  enabled: unknown,
  scope: string,
  prefix: string,
  message: unknown,
  object: unknown,
): void {
  if (!enabled) return;
  writeWidgetLog(true, scope, prefix, message);
  console.table(object);
}
