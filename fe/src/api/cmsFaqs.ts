import { cmsApiClient } from '../lib/cmsAxios'

export interface CmsFaqItem {
  id: number
  question: string
  answer: string
  category: string
  sortOrder: number
  published: boolean
  medicalReviewer: string | null
  medicalReviewedAt: string | null
  createdAt: string
  updatedAt: string
  version?: number
}

export interface CmsFaqPageResponse {
  items: CmsFaqItem[]
  total: number
  page: number
  pageSize: number
}

export interface CmsFaqFormValues {
  question: string
  answer: string
  category: string
  sortOrder?: number
  published?: boolean
  /** Bắt buộc khi xuất bản. Chỉ gửi khi bác sĩ thực sự duyệt (lại) nội dung lần này. */
  medicalReviewer?: string | null
  version?: number | null
}

export interface CmsFaqPublishInput {
  published: boolean
  medicalReviewer?: string | null
  version?: number | null
}

export async function listCmsFaqs(params: {
  q?: string
  category?: string
  published?: boolean
  page?: number
  size?: number
} = {}): Promise<CmsFaqPageResponse> {
  const { data } = await cmsApiClient.get<CmsFaqPageResponse>('/cms/faqs', { params })
  return data
}

export async function listCmsFaqCategories(): Promise<string[]> {
  const { data } = await cmsApiClient.get<string[]>('/cms/faqs/categories')
  return data
}

export async function getCmsFaq(id: number): Promise<CmsFaqItem> {
  const { data } = await cmsApiClient.get<CmsFaqItem>(`/cms/faqs/${id}`)
  return data
}

export async function createCmsFaq(payload: CmsFaqFormValues): Promise<CmsFaqItem> {
  const { data } = await cmsApiClient.post<CmsFaqItem>('/cms/faqs', payload)
  return data
}

export async function updateCmsFaq(id: number, payload: CmsFaqFormValues): Promise<CmsFaqItem> {
  const { data } = await cmsApiClient.put<CmsFaqItem>(`/cms/faqs/${id}`, payload)
  return data
}

export async function setCmsFaqPublished(id: number, payload: CmsFaqPublishInput): Promise<CmsFaqItem> {
  const { data } = await cmsApiClient.patch<CmsFaqItem>(`/cms/faqs/${id}/publish`, payload)
  return data
}

export async function deleteCmsFaq(id: number): Promise<void> {
  await cmsApiClient.delete(`/cms/faqs/${id}`)
}
