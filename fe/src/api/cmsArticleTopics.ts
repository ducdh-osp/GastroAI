import { cmsApiClient } from '../lib/cmsAxios'

/** UC0066 - 1 chủ đề bài viết; articleCount là số bài viết đang dùng chủ đề này. */
export interface ArticleTopic {
  id: number
  name: string
  sortOrder: number
  articleCount: number
}

export interface ArticleTopicInput {
  name: string
  sortOrder?: number | null
}

export async function listArticleTopics(): Promise<ArticleTopic[]> {
  const { data } = await cmsApiClient.get<ArticleTopic[]>('/cms/article-topics')
  return data
}

export async function createArticleTopic(input: ArticleTopicInput): Promise<ArticleTopic> {
  const { data } = await cmsApiClient.post<ArticleTopic>('/cms/article-topics', input)
  return data
}

export async function updateArticleTopic(id: number, input: ArticleTopicInput): Promise<ArticleTopic> {
  const { data } = await cmsApiClient.put<ArticleTopic>(`/cms/article-topics/${id}`, input)
  return data
}

/** 409 nếu còn bài viết đang dùng chủ đề - message từ BE đã đủ rõ để hiển thị thẳng. */
export async function deleteArticleTopic(id: number): Promise<void> {
  await cmsApiClient.delete(`/cms/article-topics/${id}`)
}

export function extractArticleTopicError(error: unknown, fallback: string): string {
  return (error as { response?: { data?: { message?: string } } })?.response?.data?.message ?? fallback
}
