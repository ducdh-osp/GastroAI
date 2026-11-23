import { cmsApiClient } from '../lib/cmsAxios'

export type DocumentStatus = 'PENDING' | 'PROCESSING' | 'DONE' | 'ERROR'
export type DocumentProcessingStage =
  | 'PENDING'
  | 'EXTRACTION'
  | 'CHUNKING'
  | 'EMBEDDING'
  | 'DONE'
  | 'ERROR'

export interface CmsDocumentItem {
  id: number
  title: string
  source: string | null
  sourceUrl: string | null
  status: DocumentStatus
  processingStage: DocumentProcessingStage
  errorMessage: string | null
  chunkCount: number
  uploadedAt: string
  updatedAt: string
  /** Chatbot đang dùng tài liệu này (có bản đã xác nhận và không bị gỡ). */
  inUse: boolean
  /** Dòng đang hiện là bản thay thế chờ xử lý / chờ xác nhận; bản cũ vẫn phục vụ chatbot. */
  replacing: boolean
  unpublished: boolean
  /** Dòng đang hiện đã được Biên tập xác nhận nội dung chưa (SCR-AD-04 §3.1b). */
  contentApproved: boolean
}

export async function listCmsDocuments(params: {
  q?: string
  status?: DocumentStatus
  /** Ngày cập nhật, dạng YYYY-MM-DD theo giờ Việt Nam. */
  updatedFrom?: string
  updatedTo?: string
  page?: number
  size?: number
} = {}): Promise<CmsDocumentPage> {
  const { data } = await cmsApiClient.get<CmsDocumentPage>('/documents', { params })
  return data
}

export async function uploadCmsDocument(file: File, sourceUrl?: string, title?: string): Promise<CmsDocumentItem> {
  const formData = new FormData()
  formData.append('file', file)
  if (sourceUrl && sourceUrl.trim()) {
    formData.append('sourceUrl', sourceUrl.trim())
  }
  if (title && title.trim()) {
    formData.append('title', title.trim())
  }

  const { data } = await cmsApiClient.post<CmsDocumentItem>('/documents', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  })
  return data
}

export async function retryCmsDocument(documentId: number): Promise<void> {
  await cmsApiClient.post(`/documents/${documentId}/retry`)
}

export async function deleteCmsDocument(documentId: number): Promise<void> {
  await cmsApiClient.delete(`/documents/${documentId}`)
}

export interface CmsDocumentStats {
  total: number
  ready: number
  processing: number
  error: number
}

export interface CmsDocumentPage {
  items: CmsDocumentItem[]
  total: number
  page: number
  pageSize: number
  stats: CmsDocumentStats
}

/** UC0055 - 1 tài liệu khớp từ khoá: số đoạn khớp + câu trích quanh chỗ khớp đầu tiên. */
export interface CmsDocumentSearchHit {
  documentId: number
  title: string
  matchCount: number
  snippet: string
}

/** Tối đa 20 tài liệu khớp nhiều nhất; totalDocuments lớn hơn items.length là còn tài liệu chưa hiện. */
export interface CmsDocumentSearchResult {
  items: CmsDocumentSearchHit[]
  totalDocuments: number
}

/**
 * UC0049 / SCR-AD-04 §3.2 - tải bản thay thế. Bản đang dùng vẫn phục vụ AI cho tới khi Biên tập
 * xác nhận bản mới; lúc đó bản cũ bị xoá vĩnh viễn (không giữ lịch sử phiên bản).
 */
export async function uploadCmsDocumentReplacement(documentId: number, file: File): Promise<void> {
  const formData = new FormData()
  formData.append('file', file)
  await cmsApiClient.post(`/documents/${documentId}/replacement`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

/** SCR-AD-04 §3.1b - nội dung 1 bản để đối chiếu trước khi xác nhận. */
export interface CmsDocumentReview {
  id: number
  title: string
  status: DocumentStatus
  /** Chatbot đang dùng đúng bản này. */
  inUse: boolean
  contentApproved: boolean
  contentApprovedBy: string | null
  contentApprovedAt: string | null
  /** PDF xem bằng iframe; DOCX hoặc không có file gốc thì xem fullText. */
  originalFileType: 'PDF' | 'DOCX' | null
  /** true = bản thay thế: xác nhận sẽ xoá vĩnh viễn bản đang dùng (SCR-AD-04 §3.2). */
  replacesExisting: boolean
  fullText: string | null
  chunks: { chunkIndex: number; content: string }[]
}

export async function getCmsDocumentReview(documentId: number): Promise<CmsDocumentReview> {
  const { data } = await cmsApiClient.get<CmsDocumentReview>(`/documents/${documentId}/review`)
  return data
}

/** Tải file gốc kèm phiên đăng nhập, trả về Blob để tạo đường dẫn blob: cho iframe. */
export async function getCmsDocumentFile(documentId: number): Promise<Blob> {
  const { data } = await cmsApiClient.get<Blob>(`/documents/${documentId}/file`, { responseType: 'blob' })
  return data
}

/** SCR-AD-04 §3.1b - chỉ Biên tập gọi được (BE trả 403 với Admin). */
export async function approveCmsDocumentContent(documentId: number): Promise<void> {
  await cmsApiClient.post(`/documents/${documentId}/approve`)
}
export async function updateCmsDocumentInfo(
  documentId: number,
  payload: { title: string; sourceUrl: string | null },
): Promise<void> {
  await cmsApiClient.patch(`/documents/${documentId}`, payload)
}
export async function publishCmsDocument(documentId: number): Promise<void> {
  await cmsApiClient.post(`/documents/${documentId}/publish`)
}

export async function unpublishCmsDocument(documentId: number): Promise<void> {
  await cmsApiClient.post(`/documents/${documentId}/unpublish`)
}

export async function searchCmsDocumentContent(q: string): Promise<CmsDocumentSearchResult> {
  const { data } = await cmsApiClient.get<CmsDocumentSearchResult>('/documents/search', { params: { q } })
  return data
}

/** Các đoạn khớp từ khoá trong 1 tài liệu - gọi khi bấm "Xem các đoạn". */
export async function searchCmsDocumentChunks(
  documentId: number,
  q: string,
): Promise<{ chunkIndex: number; content: string }[]> {
  const { data } = await cmsApiClient.get<{ chunkIndex: number; content: string }[]>(
    `/documents/${documentId}/search`,
    { params: { q } },
  )
  return data
}