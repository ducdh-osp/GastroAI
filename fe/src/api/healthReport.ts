import { apiClient } from '../lib/axios'

export async function downloadHealthReport(from: string, to: string): Promise<void> {
  try {
    const { data: blob } = await apiClient.get<Blob>('/patient/health-report/pdf', {
      params: { from, to },
      responseType: 'blob',
    })

    const objectUrl = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = objectUrl
    link.download = `nhat-ky-suc-khoe_${from}_${to}.pdf`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    setTimeout(() => URL.revokeObjectURL(objectUrl), 1000)
  } catch (error) {
    throw await extractBlobErrorMessage(error, 'Không thể tải nhật ký sức khỏe. Vui lòng thử lại.')
  }
}

async function extractBlobErrorMessage(error: unknown, fallback: string): Promise<Error> {
  const response = (error as { response?: { data?: Blob } })?.response
  if (response?.data instanceof Blob) {
    try {
      const text = await response.data.text()
      const parsed = JSON.parse(text) as { message?: string }
      if (parsed.message) {
        return new Error(parsed.message)
      }
    } catch {
      // Khong parse duoc (vi du loi mang, khong co body JSON) - dung fallback ben duoi.
    }
  }
  return new Error(fallback)
}