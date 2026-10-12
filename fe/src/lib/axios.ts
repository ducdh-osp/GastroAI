import axios from 'axios'

const baseURL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api/v1'

export const apiClient = axios.create({
  baseURL,
  // 60s (không phải 10s mặc định): /chat/messages gọi Gemini tuần tự 2-3 lượt
  // (embedding câu hỏi + sinh câu trả lời + sinh câu hỏi gợi ý), cộng dồn dễ vượt 10s.
  timeout: 60000,
})

/** Đọc JWT hiện tại từ localStorage — dùng chung cho interceptor axios và fetch streaming. */
export function getAuthToken(): string | null {
  try {
    const raw = localStorage.getItem('gastroai.auth')
    if (!raw) return null
    const { token } = JSON.parse(raw) as { token?: string }
    return token ?? null
  } catch {
    return null
  }
}

// Tự động gắn Authorization header từ token đang lưu trong localStorage
apiClient.interceptors.request.use((config) => {
  const token = getAuthToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})