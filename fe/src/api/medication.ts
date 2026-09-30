import { apiClient } from '../lib/axios'
import { extractErrorMessage } from '../lib/errors'

export interface MedicationReminder {
  id: number
  medicineName: string
  dosage: string | null
  timeOfDay: string
  active: boolean
  confirmedToday: boolean
}

export interface MedicationReminderRequest {
  medicineName: string
  dosage: string | null
  timeOfDay: string
  active: boolean
}

export interface MedicationConfirmation {
  id: number
  reminderId: number
  confirmedAt: string
}

export interface MedicationConfirmationDetail {
  id: number
  reminderId: number
  medicineName: string
  confirmedAt: string
}

export interface MedicationConfirmationListResponse {
  items: MedicationConfirmationDetail[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export async function listMedicationReminders(): Promise<MedicationReminder[]> {
  try {
    const { data } = await apiClient.get<MedicationReminder[]>('/patient/medications')
    return data
  } catch (error) {
    throw extractErrorMessage(error, 'Không thể tải danh sách lịch nhắc thuốc. Vui lòng thử lại.')
  }
}

export async function createMedicationReminder(payload: MedicationReminderRequest): Promise<MedicationReminder> {
  try {
    const { data } = await apiClient.post<MedicationReminder>('/patient/medications', payload)
    return data
  } catch (error) {
    throw extractErrorMessage(error, 'Không thể thêm lịch nhắc thuốc. Vui lòng thử lại.')
  }
}

export async function updateMedicationReminder(id: number, payload: MedicationReminderRequest): Promise<MedicationReminder> {
  try {
    const { data } = await apiClient.put<MedicationReminder>(`/patient/medications/${id}`, payload)
    return data
  } catch (error) {
    throw extractErrorMessage(error, 'Không thể sửa lịch nhắc thuốc. Vui lòng thử lại.')
  }
}

export async function deleteMedicationReminder(id: number): Promise<void> {
  try {
    await apiClient.delete(`/patient/medications/${id}`)
  } catch (error) {
    throw extractErrorMessage(error, 'Không thể xoá lịch nhắc thuốc. Vui lòng thử lại.')
  }
}

export async function confirmMedicationDose(reminderId: number): Promise<MedicationConfirmation> {
  try {
    const { data } = await apiClient.post<MedicationConfirmation>(`/patient/medications/${reminderId}/confirmations`)
    return data
  } catch (error) {
    throw extractErrorMessage(error, 'Không thể ghi nhận đã uống thuốc. Vui lòng thử lại.')
  }
}

export async function listMedicationConfirmations(page = 0, size = 20): Promise<MedicationConfirmationListResponse> {
  try {
    const { data } = await apiClient.get<MedicationConfirmationListResponse>('/patient/medications/confirmations', {
      params: { page, size },
    })
    return data
  } catch (error) {
    throw extractErrorMessage(error, 'Không thể tải lịch sử xác nhận đã uống. Vui lòng thử lại.')
  }
}
