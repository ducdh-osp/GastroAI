export function isAtMostUtf8Bytes(value: string, maxBytes: number): boolean {
  return new TextEncoder().encode(value).length <= maxBytes
}
