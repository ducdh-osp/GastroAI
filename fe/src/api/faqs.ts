import { apiClient } from '../lib/axios'

export interface FaqItem {
  id: number
  question: string
  answer: string
  category: string
  sortOrder: number
}

export async function listFaqs(params: {
  q?: string
  category?: string
} = {}): Promise<FaqItem[]> {
  const { data } = await apiClient.get<FaqItem[]>('/faqs', { params })
  return data
}

export async function listFaqCategories(): Promise<string[]> {
  const { data } = await apiClient.get<string[]>('/faqs/categories')
  return data
}
