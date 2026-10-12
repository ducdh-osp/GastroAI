import { apiClient } from './axios'

export async function downloadFile(
  url: string,
  filename: string,
  params?: Record<string, string>,
): Promise<void> {
  try {
    const { data: blob } = await apiClient.get<Blob>(url, {
      params,
      responseType: 'blob',
    })

    const objectUrl = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = objectUrl
    link.download = filename
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    // Khong revoke ngay - mot so trinh duyet (Firefox, Safari) huy luot tai dang chay neu
    // object URL bi thu hoi qua som. Doi 1 giay cho browser kip bat dau tai roi moi giai
    // phong bo nho.
    setTimeout(() => URL.revokeObjectURL(objectUrl), 1000)
  } catch (error) {
    throw await extractBlobErrorMessage(error, 'Không thể tải file. Vui lòng thử lại.')
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