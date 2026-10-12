import { cmsApiClient } from '../lib/cmsAxios'

export interface CmsFaqItem {
  id: number
  question: string
  answer: string
  category: string
  sortOrder: number
  published: boolean
  createdAt: string
  updatedAt: string
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

export async function setCmsFaqPublished(id: number, published: boolean): Promise<CmsFaqItem> {
  const { data } = await cmsApiClient.patch<CmsFaqItem>(`/cms/faqs/${id}/publish`, { published })
  return data
}

export async function deleteCmsFaq(id: number): Promise<void> {
  await cmsApiClient.delete(`/cms/faqs/${id}`)
}
