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
  source: string
  sourceUrl: string | null
  status: DocumentStatus
  processingStage: DocumentProcessingStage
  errorMessage: string | null
  chunkCount: number
  uploadedAt: string
  updatedAt: string
}

export interface CmsDocumentPage {
  items: CmsDocumentItem[]
  total: number
  page: number
  pageSize: number
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

export async function uploadCmsDocument(file: File, sourceUrl?: string): Promise<CmsDocumentItem> {
  const formData = new FormData()
  formData.append('file', file)
  if (sourceUrl && sourceUrl.trim()) {
    formData.append('sourceUrl', sourceUrl.trim())
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
