import { downloadFile } from '../lib/download'

/** Tải PDF nhật ký sức khỏe từ BE và cho browser lưu xuống máy người dùng (UC0019). */
export async function downloadHealthReport(from: string, to: string): Promise<void> {
  await downloadFile('/patient/health-report/pdf', `nhat-ky-suc-khoe_${from}_${to}.pdf`, { from, to })
}