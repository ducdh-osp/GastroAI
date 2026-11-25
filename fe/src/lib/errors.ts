interface ErrorResponseLike {
  response?: {
    status?: number
    data?: { message?: string; requestId?: string }
    headers?: Record<string, unknown> & { get?: (name: string) => unknown }
  }
}

/**
 * Mã tra cứu BE trả về cho request lỗi (body "requestId", rơi về header X-Request-Id nếu axios
 * đọc được — header chỉ đọc được khi CORS expose nó). Dùng để người dùng báo lỗi, đội dev tìm
 * đúng dòng log.
 */
export function requestIdOf(error: unknown): string | undefined {
  const response = (error as ErrorResponseLike)?.response
  const fromBody = response?.data?.requestId
  if (typeof fromBody === 'string' && fromBody) return fromBody
  const headers = response?.headers
  const fromHeader = typeof headers?.get === 'function' ? headers.get('x-request-id') : headers?.['x-request-id']
  return typeof fromHeader === 'string' && fromHeader ? fromHeader : undefined
}

/**
 * Trích message lỗi từ response axios (nếu BE có trả), rơi về fallback nếu không. Lỗi hệ thống
 * (5xx) hoặc không có message từ BE thì gắn thêm mã tra cứu để người dùng báo lại; lỗi 4xx có
 * message rõ ràng thì giữ nguyên câu như cũ.
 */
export function extractErrorMessage(error: unknown, fallback: string): Error {
  const response = (error as ErrorResponseLike)?.response
  const backendMessage = response?.data?.message
  let message = backendMessage ?? fallback
  const requestId = requestIdOf(error)
  if (requestId && ((response?.status ?? 0) >= 500 || !backendMessage)) {
    message += ` (Mã tra cứu: ${requestId})`
  }
  return new Error(message)
}
