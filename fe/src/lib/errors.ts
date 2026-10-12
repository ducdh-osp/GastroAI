/** Trích message lỗi từ response axios (nếu BE có trả), rơi về fallback nếu không. */
export function extractErrorMessage(error: unknown, fallback: string): Error {
  const message = (error as { response?: { data?: { message?: string } } })?.response?.data?.message ?? fallback
  return new Error(message)
}
