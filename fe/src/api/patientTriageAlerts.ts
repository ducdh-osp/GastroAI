import { apiClient } from '../lib/axios'
import { extractErrorMessage } from '../lib/errors'

export type TriageCheckInResponse = 'SAFE' | 'NEED_HELP'

export interface TriageCheckInResult {
  alertId: number
  response: TriageCheckInResponse
  /** Ca đã đóng từ trước (bấm lại trên thông báo cũ) - không có gì thay đổi. */
  alreadyClosed: boolean
}

/** Phát ra khi bệnh nhân đã trả lời ở 1 chỗ (popup hoặc trang Thông báo) để chỗ kia tự đóng. */
export const TRIAGE_ANSWERED_EVENT = 'gastroai:triage-answered'

/**
 * SCR-AD-13 bước 1 - trả lời "Bạn có ổn không?". [An toàn] đóng cảnh báo, [Cần hỗ trợ] báo ngay
 * cho nhân sự GastroAI. Không có trang riêng: trả lời ngay trên popup hoặc trang Thông báo.
 */
export async function answerTriageCheckIn(
  alertId: number,
  response: TriageCheckInResponse,
): Promise<TriageCheckInResult> {
  try {
    const { data } = await apiClient.post<TriageCheckInResult>(`/patient/triage-alerts/${alertId}/check-in`, {
      response,
    })
    return data
  } catch (error) {
    throw extractErrorMessage(error, 'Không gửi được câu trả lời. Vui lòng thử lại.')
  }
}
