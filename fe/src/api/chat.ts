import { apiClient, getAuthToken } from '../lib/axios'
import { downloadFile } from '../lib/download'
export type SenderType = 'patient' | 'assistant' | 'system'

export type MessageStatus = 'sending' | 'sent' | 'failed' | 'replying'

export type RatingValue = 'HELPFUL' | 'UNHELPFUL'

export interface Attachment {
  id: string
  name: string
  type: string
  size: number
  url: string
  previewUrl?: string
  /** File gốc, chỉ có khi vừa chọn trong phiên này (chưa gửi/chưa tải lại từ lịch sử) - dùng để gửi bytes thật lên BE. */
  file?: File
}

/** Khớp ChatAttachmentResponse bên BE (ChatHistoryController). */
export interface ChatAttachmentDto {
  id: number
  originalFilename: string
  contentType: string
  sizeBytes: number
  /** Đường dẫn tương đối API (không có domain) - phải gọi qua apiClient để có header Authorization, không dùng trực tiếp làm <img src>. */
  url: string
}

export interface SourceRef {
  documentTitle: string
  snippet: string
  sourceUrl?: string | null
}

export interface Message {
  id: string
  /** DB-side message id — dùng để gọi API rating. null nếu là tin nhắn tạm thời chưa lưu DB. */
  dbMessageId?: number | null
  sender: SenderType
  content: string
  createdAt: string
  status: MessageStatus
  attachments?: Attachment[]
  sources?: SourceRef[]
  relatedQuestions?: string[]
  emergency?: boolean
  matchedGroups?: string[]
  /** Đánh giá hiện tại của người dùng cho tin nhắn AI này. */
  rating?: RatingValue | null
}

export interface SendMessageRequest {
  content: string
  attachments?: Attachment[]
  isRetry?: boolean
  /** sessionId để tiếp tục phiên hiện tại. null = bắt đầu phiên mới. */
  sessionId?: number | null
}

export interface ChatService {
  sendMessage: (request: SendMessageRequest) => Promise<Message & { sessionId?: number | null }>
}

export interface ChatSessionSummary {
  id: number
  title: string
  createdAt: string
  updatedAt: string
}

export interface ChatSessionListResponse {
  sessions: ChatSessionSummary[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface ChatMessageDetail {
  id: number
  sender: SenderType
  content: string
  createdAt: string
  emergency: boolean
  sources: SourceRef[]
  relatedQuestions: string[]
  matchedGroups: string[]
  rating: RatingValue | null
  attachments: ChatAttachmentDto[]
}

const TRIAGE_GROUP_LABELS: Record<string, string> = {
  XUAT_HUYET_TIEU_HOA: 'nghi xuất huyết tiêu hóa',
  DAU_BUNG_CAP_TINH: 'đau bụng dữ dội, cấp tính',
  TAC_RUOT: 'nghi tắc ruột',
  SOC_MAT_MAU: 'dấu hiệu sốc/mất máu',
  NHIEM_TRUNG_NANG: 'nghi nhiễm trùng nặng/viêm ruột thừa',
  DAU_LAN_CO_QUAN_KHAC: 'đau lan sang vùng khác (lưng/vai/ngực)',
  KEM_HO_HAP_TIM_MACH: 'kèm khó thở/đau ngực',
  TIEU_DUONG_KEM_NON: 'tiểu đường kèm nôn nhiều',
  VANG_DA_KEM_DAU_BUNG: 'vàng da kèm đau bụng',
  MAT_NUOC_NANG: 'mất nước nặng do tiêu chảy kéo dài',
}

/** Chuỗi mô tả các nhóm dấu hiệu khẩn cấp đã khớp (dùng trong banner cảnh báo), null nếu rỗng/không có. */
export function describeMatchedGroups(matchedGroups?: string[]): string | null {
  if (!matchedGroups || matchedGroups.length === 0) return null
  return matchedGroups.map((group) => TRIAGE_GROUP_LABELS[group] ?? group).join(', ')
}

export const QUICK_PROMPTS = [
  'Tư vấn triệu chứng sốt',
  'Hỏi lịch uống thuốc',
  'Đau bụng nên làm gì?',
  'Chế độ ăn cho người đau dạ dày',
] as const

async function sendMessage(request: SendMessageRequest): Promise<Message & { sessionId?: number | null }> {
  try {
    const filesToSend = (request.attachments ?? []).map((a) => a.file).filter((f): f is File => f != null)

    if (filesToSend.length === 0) {
      const { data } = await apiClient.post<Message & { sessionId?: number | null }>('/chat/messages', {
        content: request.content,
        sessionId: request.sessionId ?? null,
      })
      return data
    }

    const formData = new FormData()
    formData.append(
      'request',
      new Blob([JSON.stringify({ content: request.content, sessionId: request.sessionId ?? null })], {
        type: 'application/json',
      }),
    )
    filesToSend.forEach((file) => formData.append('files', file))

    const { data } = await apiClient.post<Message & { sessionId?: number | null }>(
      '/chat/messages/with-attachments',
      formData,
    )
    return data
  } catch (error) {
    const message =
      (error as { response?: { data?: { message?: string } } })?.response?.data?.message ??
      'Không thể kết nối mạng. Vui lòng kiểm tra kết nối và thử lại.'
    throw new Error(message)
  }
}

/**
 * Tải lại 1 file đính kèm từ lịch sử thành blob URL dùng được cho <img>/<a> - không dùng
 * attachment.url (đường dẫn API) trực tiếp vì endpoint đó cần header Authorization mà
 * browser không tự gắn khi load ảnh/mở link thường (xem ChatHistoryController.downloadAttachment).
 */
async function loadAttachmentForDisplay(dto: ChatAttachmentDto): Promise<Attachment> {
  const { data: blob } = await apiClient.get<Blob>(dto.url, { responseType: 'blob' })
  const objectUrl = URL.createObjectURL(blob)
  return {
    id: String(dto.id),
    name: dto.originalFilename,
    type: dto.contentType,
    size: dto.sizeBytes,
    url: objectUrl,
    previewUrl: dto.contentType.startsWith('image/') ? objectUrl : undefined,
  }
}

/** Tải lại toàn bộ đính kèm của 1 tin nhắn - rỗng trả về nhanh, không gọi API nào. */
export async function loadAttachmentsForDisplay(dtos: ChatAttachmentDto[]): Promise<Attachment[]> {
  if (dtos.length === 0) return []
  return Promise.all(dtos.map(loadAttachmentForDisplay))
}

/** Gọi thật BE (UC0017): BE gọi RagQueryService.answer() (Gemini/RAG thật), không còn là mock. */
export const chatService: ChatService = { sendMessage }

// ─────────────────────────── Streaming (SSE qua fetch) ────────────────────────────────

/** Khớp với StreamingDoneEvent của BE — gửi kèm sự kiện "done", báo kết thúc luồng. */
export interface StreamingDone {
  sources: SourceRef[]
  relatedQuestions: string[]
  emergency: boolean
  matchedGroups: string[]
  sessionId: number | null
  assistantMessageId: number | null
}

export interface StreamMessageCallbacks {
  onToken: (text: string) => void
  onDone: (done: StreamingDone) => void
  onError: (message: string) => void
}

/** Tách 1 sự kiện SSE trọn vẹn (đã bỏ \n\n cuối) thành tên sự kiện + nội dung data đã nối dòng. */
function parseSseEvent(rawEvent: string): { eventName: string; data: string } {
  let eventName = ''
  const dataLines: string[] = []

  for (const line of rawEvent.split('\n')) {
    if (line.startsWith('event:')) {
      eventName = line.slice('event:'.length)
    } else if (line.startsWith('data:')) {
      // Khong trim(): Spring ghi "data:" khong co dau cach phia sau, va token Gemini
      // thuong bat dau bang dau cach (vd " bung") - trim se lam chu dinh lien vao nhau.
      dataLines.push(line.slice('data:'.length))
    }
  }

  // Cau tra loi co markdown nen co xuong dong - Spring tach 1 token co xuong dong thanh
  // nhieu dong "data:" rieng, phai noi lai bang \n de khong lam dinh danh sach/doan van.
  return { eventName, data: dataLines.join('\n') }
}

/**
 * Gửi câu hỏi và nhận câu trả lời AI theo kiểu streaming (SSE) qua `fetch` +
 * `response.body.getReader()`. Không dùng EventSource (chỉ GET, không gắn được header
 * Authorization) hay axios (đợi nhận hết mới trả, không đọc từng phần được).
 */
export async function streamMessage(
  request: SendMessageRequest,
  { onToken, onDone, onError }: StreamMessageCallbacks,
  signal?: AbortSignal,
): Promise<void> {
  const baseURL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api/v1'
  const token = getAuthToken()
  const filesToSend = (request.attachments ?? []).map((a) => a.file).filter((f): f is File => f != null)

  let url: string
  let body: BodyInit
  const headers: Record<string, string> = {
    Accept: 'text/event-stream',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
  }

  if (filesToSend.length === 0) {
    url = `${baseURL}/chat/messages/stream`
    headers['Content-Type'] = 'application/json'
    body = JSON.stringify({ content: request.content, sessionId: request.sessionId ?? null })
  } else {
    url = `${baseURL}/chat/messages/stream-with-attachments`
    // KHONG tu dat Content-Type: trinh duyet tu sinh "multipart/form-data; boundary=..."
    // dung - dat tay se mat boundary va BE khong doc duoc.
    const formData = new FormData()
    formData.append(
      'request',
      new Blob([JSON.stringify({ content: request.content, sessionId: request.sessionId ?? null })], {
        type: 'application/json',
      }),
    )
    filesToSend.forEach((file) => formData.append('files', file))
    body = formData
  }

  let response: Response
  try {
    response = await fetch(url, {
      method: 'POST',
      headers,
      body,
      signal,
    })
  } catch (error) {
    if ((error as { name?: string })?.name === 'AbortError') return
    onError('Không thể kết nối mạng. Vui lòng kiểm tra kết nối và thử lại.')
    return
  }

  if (!response.ok || !response.body) {
    if (response.status === 401) {
      onError('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.')
      return
    }
    // File sai dinh dang/qua lon bi tu choi truoc khi mo SSE (xem ChatAttachmentProcessor) -
    // BE tra JSON binh thuong (400/413) voi message cu the, uu tien hien thi message do.
    try {
      const errorBody = (await response.json()) as { message?: string }
      onError(errorBody.message || 'Không thể nhận câu trả lời từ AI. Vui lòng thử lại.')
    } catch {
      onError('Không thể nhận câu trả lời từ AI. Vui lòng thử lại.')
    }
    return
  }
  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''
  let finished = false

  try {
    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })

      let separatorIndex = buffer.indexOf('\n\n')
      while (separatorIndex !== -1) {
        const rawEvent = buffer.slice(0, separatorIndex)
        buffer = buffer.slice(separatorIndex + 2)

        if (rawEvent) {
          const { eventName, data } = parseSseEvent(rawEvent)

          if (eventName === 'done') {
            finished = true
            try {
              onDone(JSON.parse(data) as StreamingDone)
            } catch {
              onError('Không đọc được phản hồi từ AI. Vui lòng thử lại.')
            }
          } else if (eventName === 'error') {
            // BE da bao loi ro rang (vd Gemini loi that) - danh dau ket thuc va thoat
            // ngay, khong de doan kiem tra "mat ket noi" ben duoi ghi de bang thong
            // bao sai (lam nguoi dung tuong la loi mang cua ho).
            finished = true
            onError(data || 'Không thể nhận được câu trả lời từ AI. Vui lòng thử lại.')
            return
          } else {
            onToken(data)
          }
        }

        separatorIndex = buffer.indexOf('\n\n')
      }
    }
  } catch (error) {
    if ((error as { name?: string })?.name === 'AbortError') return
    onError('Mất kết nối trong lúc nhận câu trả lời. Vui lòng thử lại.')
    return
  }

  if (!finished) {
    onError('Mất kết nối trước khi nhận được câu trả lời đầy đủ. Vui lòng thử lại.')
  }
}

// ─────────────────────────── Chat History API ────────────────────────────────

/** Danh sách phiên chat của bệnh nhân đang đăng nhập, mới nhất trước. */
export async function listChatSessions(page = 0, size = 20): Promise<ChatSessionListResponse> {
  const { data } = await apiClient.get<ChatSessionListResponse>('/chat/sessions', {
    params: { page, size },
  })
  return data
}

/** Toàn bộ tin nhắn trong 1 phiên chat. */
export async function getChatSessionMessages(sessionId: number): Promise<ChatMessageDetail[]> {
  const { data } = await apiClient.get<ChatMessageDetail[]>(`/chat/sessions/${sessionId}`)
  return data
}

/** Xóa một phiên chat cùng toàn bộ tin nhắn và đánh giá liên quan. */
export async function deleteChatSession(sessionId: number): Promise<void> {
  await apiClient.delete(`/chat/sessions/${sessionId}`)
}

/** Đánh giá câu trả lời AI (UPSERT — có thể đổi ý). */
export async function rateMessage(messageId: number, rating: RatingValue): Promise<void> {
  await apiClient.post(`/chat/messages/${messageId}/rating`, { rating })
}
/** Tải PDF toàn bộ 1 phiên chat (UC0027). */
export async function downloadChatSessionPdf(sessionId: number): Promise<void> {
  await downloadFile(`/chat/sessions/${sessionId}/pdf`, `phien-chat-${sessionId}.pdf`)
}