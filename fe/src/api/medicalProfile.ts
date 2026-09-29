import { apiClient } from '../lib/axios'
import { extractErrorMessage } from '../lib/errors'

export type Gender = 'MALE' | 'FEMALE' | 'OTHER'

export interface MedicalProfile {
  id: number | null
  dateOfBirth: string | null
  gender: Gender | null
  heightCm: number | null
  weightKg: number | null
  medicalHistory: string | null
  allergies: string[]
  chronicConditions: string[]
  pastSurgeries: string[]
  currentMedications: string[]
  dietaryRestrictions: string[]
  updatedAt: string | null
  exists: boolean
}

export interface UpdateMedicalProfileRequest {
  dateOfBirth: string | null
  gender: Gender | null
  heightCm: number | null
  weightKg: number | null
  medicalHistory: string
  allergies: string[]
  chronicConditions: string[]
  pastSurgeries: string[]
  currentMedications: string[]
  dietaryRestrictions: string[]
}

/** UC0009/UC0010 - luon tra ve 200, exists=false neu benh nhan chua khai bao lan nao. */
export async function getMedicalProfile(): Promise<MedicalProfile> {
  try {
    const { data } = await apiClient.get<MedicalProfile>('/patient/medical-profile')
    return data
  } catch (error) {
    throw extractErrorMessage(error, 'Không thể tải hồ sơ bệnh lý. Vui lòng thử lại.')
  }
}

/** UPSERT - dung chung cho ca khai bao lan dau (UC0009) lan cap nhat (UC0010). */
export async function updateMedicalProfile(payload: UpdateMedicalProfileRequest): Promise<MedicalProfile> {
  try {
    const { data } = await apiClient.put<MedicalProfile>('/patient/medical-profile', payload)
    return data
  } catch (error) {
    throw extractErrorMessage(error, 'Không thể lưu hồ sơ bệnh lý. Vui lòng thử lại.')
  }
}
