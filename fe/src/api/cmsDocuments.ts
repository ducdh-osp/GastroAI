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
  versionNo: number
  currentVersionNo: number | null
  versionCount: number
  unpublished: boolean
}

export async function listCmsDocuments(params: {
  q?: string
  status?: DocumentStatus
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

export interface CmsDocumentVersion {
  id: number
  versionNo: number
  title: string
  status: DocumentStatus
  currentVersion: boolean
  uploadedAt: string
  errorMessage: string | null
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

export interface CmsDocumentSearchHit {
  documentId: number
  title: string
  chunkIndex: number
  snippet: string
  content: string
}

export interface CmsDocumentSearchResult {
  items: CmsDocumentSearchHit[]
  truncated: boolean
}

export async function uploadCmsDocumentVersion(documentId: number, file: File): Promise<void> {
  const formData = new FormData()
  formData.append('file', file)
  await cmsApiClient.post(`/documents/${documentId}/versions`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export async function listCmsDocumentVersions(documentId: number): Promise<CmsDocumentVersion[]> {
  const { data } = await cmsApiClient.get<CmsDocumentVersion[]>(`/documents/${documentId}/versions`)
  return data
}

export async function activateCmsDocumentVersion(documentId: number, versionId: number): Promise<void> {
  await cmsApiClient.post(`/documents/${documentId}/versions/${versionId}/activate`)
}
export async function deleteCmsDocumentVersion(documentId: number, versionId: number): Promise<void> {
  await cmsApiClient.delete(`/documents/${documentId}/versions/${versionId}`)
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