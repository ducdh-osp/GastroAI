import axios from 'axios'

const baseURL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api/v1'

export const apiClient = axios.create({
  baseURL,
  // 60s (không phải 10s mặc định): /chat/messages gọi Gemini tuần tự 2-3 lượt
  // (embedding câu hỏi + sinh câu trả lời + sinh câu hỏi gợi ý), cộng dồn dễ vượt 10s.
  timeout: 60000,
})

// Tự động gắn Authorization header từ token đang lưu trong localStorage
apiClient.interceptors.request.use((config) => {
  try {
    const raw = localStorage.getItem('gastroai.auth')
    if (raw) {
      const { token } = JSON.parse(raw) as { token?: string }
      if (token) {
        config.headers.Authorization = `Bearer ${token}`
      }
    }
  } catch {
    // bỏ qua nếu parse lỗi
  }
  return config
})
