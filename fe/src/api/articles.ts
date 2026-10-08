import { apiClient } from '../lib/axios'

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

export async function getKnowledgeArticle(slug: string): Promise<KnowledgeArticleDetail> {
  const { data } = await apiClient.get<KnowledgeArticleDetail>(`/articles/${encodeURIComponent(slug)}`)
  return data
}
