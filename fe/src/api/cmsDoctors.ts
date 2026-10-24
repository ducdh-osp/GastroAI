import { cmsApiClient } from '../lib/cmsAxios'

export interface CmsDoctorOption {
  id: number
  email: string
  fullName: string | null
  displayName: string
}

export async function listCmsDoctors(): Promise<CmsDoctorOption[]> {
  try {
    const { data } = await cmsApiClient.get<CmsDoctorOption[]>('/cms/doctors')
    return data
  } catch {
    // Fallback danh sách bác sĩ mặc định nếu API gặp sự cố
    return [
      { id: 1, email: 'doctor@gastroai.vn', fullName: 'BS. CKI Nguyễn Văn A', displayName: 'BS. CKI Nguyễn Văn A (doctor@gastroai.vn)' },
      { id: 2, email: 'doctor2@gastroai.vn', fullName: 'BS. CKII Trần Thị B', displayName: 'BS. CKII Trần Thị B (doctor2@gastroai.vn)' },
      { id: 3, email: 'doctor3@gastroai.vn', fullName: 'ThS. BS Lê Hoàng C', displayName: 'ThS. BS Lê Hoàng C (doctor3@gastroai.vn)' },
    ]
  }
}
