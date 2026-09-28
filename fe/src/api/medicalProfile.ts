import { apiClient } from '../lib/axios'

export interface MedicalProfile {
  id: number | null
  medicalHistory: string | null
  allergies: string[]
  currentMedications: string[]
  updatedAt: string | null
  exists: boolean
}

export interface UpdateMedicalProfileRequest {
  medicalHistory: string
  allergies: string[]
  currentMedications: string[]
}

/** UC0009/UC0010 - luon tra ve 200, exists=false neu benh nhan chua khai bao lan nao. */
export async function getMedicalProfile(): Promise<MedicalProfile> {
  const { data } = await apiClient.get<MedicalProfile>('/patient/medical-profile')
  return data
}

/** UPSERT - dung chung cho ca khai bao lan dau (UC0009) lan cap nhat (UC0010). */
export async function updateMedicalProfile(payload: UpdateMedicalProfileRequest): Promise<MedicalProfile> {
  try {
    const { data } = await apiClient.put<MedicalProfile>('/patient/medical-profile', payload)
    return data
  } catch (error) {
    const message =
      (error as { response?: { data?: { message?: string } } })?.response?.data?.message ??
      'Không thể lưu hồ sơ bệnh lý. Vui lòng thử lại.'
    throw new Error(message)
  }
}
