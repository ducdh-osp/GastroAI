import { cmsApiClient } from '../lib/cmsAxios'
import { extractErrorMessage } from '../lib/errors'

/** UC0059 - cấu hình RAG/Triage chỉnh được qua CMS, chỉ Admin được xem/sửa. */
export interface AppRuntimeConfig {
  ragTopK: number
  ragSimilarityThreshold: number | null
  triageRealtimeAlertsEnabled: boolean
  updatedByType: string | null
  updatedById: number | null
}

export interface UpdateAppRuntimeConfigRequest {
  ragTopK: number
  ragSimilarityThreshold: number | null
  triageRealtimeAlertsEnabled: boolean
}

export async function getRuntimeConfig(): Promise<AppRuntimeConfig> {
  try {
    const { data } = await cmsApiClient.get<AppRuntimeConfig>('/cms/runtime-config')
    return data
  } catch (error) {
    throw extractErrorMessage(error, 'Không thể tải cấu hình hệ thống.')
  }
}

export async function updateRuntimeConfig(request: UpdateAppRuntimeConfigRequest): Promise<AppRuntimeConfig> {
  try {
    const { data } = await cmsApiClient.put<AppRuntimeConfig>('/cms/runtime-config', request)
    return data
  } catch (error) {
    throw extractErrorMessage(error, 'Không thể lưu cấu hình hệ thống.')
  }
}
