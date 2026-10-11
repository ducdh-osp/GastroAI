import { apiClient } from '../lib/axios'
import { isAxiosError } from 'axios'

export interface KnowledgeArticleSummary {
  slug: string
  title: string
  summary: string
  category: string
  readingMinutes: number
}

export interface KnowledgeArticleDetail extends KnowledgeArticleSummary {
  content: string
  sourceName: string
  sourceUrl: string
  sourceCheckedAt: string
}

export interface KnowledgeArticlePage {
  articles: KnowledgeArticleSummary[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export async function listKnowledgeArticles(params: {
  q?: string
  category?: string
  page?: number
  size?: number
} = {}): Promise<KnowledgeArticlePage> {
  const { data } = await apiClient.get<KnowledgeArticlePage>('/articles', { params })
  return data
}

export async function listKnowledgeArticleCategories(): Promise<string[]> {
  const { data } = await apiClient.get<string[]>('/articles/categories')
  return data
}

export async function getKnowledgeArticle(slug: string): Promise<KnowledgeArticleDetail> {
  try {
    const { data } = await apiClient.get<KnowledgeArticleDetail>(`/articles/${encodeURIComponent(slug)}`)
    return data
  } catch (error) {
    if (isAxiosError(error) && error.response?.status === 404) throw new Error('ARTICLE_NOT_FOUND')
    throw new Error('ARTICLE_LOAD_FAILED')
  }
}
