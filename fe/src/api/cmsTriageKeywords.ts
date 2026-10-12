import { cmsApiClient } from '../lib/cmsAxios'
import { extractErrorMessage } from '../lib/errors'

/** UC0054 - 1 từ khóa dấu hiệu cấp cứu thuộc 1 trong 13 nhóm cố định. */
export interface TriageKeyword {
  id: number
  groupCode: string
  groupLabel: string
  keyword: string
  createdAt: string
  updatedAt: string
}

/** UC0054 - 1 trong 13 nhóm cố định cho dropdown khi thêm/sửa từ khóa - không CRUD được nhóm. */
export interface TriageKeywordGroup {
  code: string
  label: string
}

export interface TriageKeywordRequest {
  groupCode: string
  keyword: string
}

export async function listTriageKeywords(): Promise<TriageKeyword[]> {
  try {
    const { data } = await cmsApiClient.get<TriageKeyword[]>('/cms/triage-keywords')
    return data
  } catch (error) {
    throw extractErrorMessage(error, 'Không thể tải danh sách từ khóa Triage.')
  }
}

export async function listTriageKeywordGroups(): Promise<TriageKeywordGroup[]> {
  try {
    const { data } = await cmsApiClient.get<TriageKeywordGroup[]>('/cms/triage-keywords/groups')
    return data
  } catch (error) {
    throw extractErrorMessage(error, 'Không thể tải danh sách nhóm từ khóa.')
  }
}

export async function createTriageKeyword(request: TriageKeywordRequest): Promise<TriageKeyword> {
  try {
    const { data } = await cmsApiClient.post<TriageKeyword>('/cms/triage-keywords', request)
    return data
  } catch (error) {
    throw extractErrorMessage(error, 'Không thể thêm từ khóa này.')
  }
}

export async function updateTriageKeyword(id: number, request: TriageKeywordRequest): Promise<TriageKeyword> {
  try {
    const { data } = await cmsApiClient.put<TriageKeyword>(`/cms/triage-keywords/${id}`, request)
    return data
  } catch (error) {
    throw extractErrorMessage(error, 'Không thể sửa từ khóa này.')
  }
}

export async function deleteTriageKeyword(id: number): Promise<void> {
  try {
    await cmsApiClient.delete(`/cms/triage-keywords/${id}`)
  } catch (error) {
    throw extractErrorMessage(error, 'Không thể xóa từ khóa này.')
  }
}
