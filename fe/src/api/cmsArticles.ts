import { cmsApiClient } from '../lib/cmsAxios'

/** UC0065 - Tóm tắt bài viết cho bảng quản trị CMS. */
export interface CmsArticleSummary {
  id: number
  slug: string
  title: string
  summary: string
  category: string
  readingMinutes: number
  published: boolean
  medicalReviewer: string | null
  medicalReviewedAt: string | null
  sortOrder: number
  updatedAt: string
}

/** UC0065 - Chi tiết đầy đủ bài viết cho form soạn thảo CMS. */
export interface CmsArticleDetail extends CmsArticleSummary {
  content: string
  sourceName: string
  sourceUrl: string
  sourceCheckedAt: string
  createdAt: string
}

export interface CmsArticlePage {
  items: CmsArticleSummary[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface CmsArticleInput {
  title: string
  slug?: string | null
  summary: string
  category: string
  readingMinutes?: number | null
  content: string
  sourceName: string
  sourceUrl: string
  sourceCheckedAt?: string | null
  published?: boolean
  medicalReviewer?: string | null
  sortOrder?: number | null
}

export interface CmsArticlePublishInput {
  published: boolean
  medicalReviewer?: string | null
}

export async function listCmsArticles(params: {
  q?: string
  category?: string
  published?: boolean
  page?: number
  size?: number
} = {}): Promise<CmsArticlePage> {
  const { data } = await cmsApiClient.get<CmsArticlePage>('/cms/articles', { params })
  return data
}

export async function getCmsArticle(id: number): Promise<CmsArticleDetail> {
  const { data } = await cmsApiClient.get<CmsArticleDetail>(`/cms/articles/${id}`)
  return data
}

export async function createCmsArticle(input: CmsArticleInput): Promise<CmsArticleDetail> {
  const { data } = await cmsApiClient.post<CmsArticleDetail>('/cms/articles', input)
  return data
}

export async function updateCmsArticle(id: number, input: CmsArticleInput): Promise<CmsArticleDetail> {
  const { data } = await cmsApiClient.put<CmsArticleDetail>(`/cms/articles/${id}`, input)
  return data
}

export async function publishCmsArticle(id: number, input: CmsArticlePublishInput): Promise<CmsArticleDetail> {
  const { data } = await cmsApiClient.patch<CmsArticleDetail>(`/cms/articles/${id}/publish`, input)
  return data
}

export async function deleteCmsArticle(id: number): Promise<void> {
  await cmsApiClient.delete(`/cms/articles/${id}`)
}

export function extractArticleError(error: unknown, fallback: string): string {
  return (error as { response?: { data?: { message?: string } } })?.response?.data?.message ?? fallback
}
