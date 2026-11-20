/**
 * Mốc thời gian SCR-AD-13 - phải khớp mặc định BE (app.triage.intervention-after-minutes,
 * app.triage.failsafe-after-minutes). Đổi cấu hình BE thì sửa ở đây cho đồng hồ đếm ngược đúng.
 */
export const INTERVENTION_AFTER_MS = 3 * 60_000
export const FAILSAFE_AFTER_MS = 5 * 60_000

/** Còn bao nhiêu ms tới mốc (from + afterMs); null nếu chưa có mốc bắt đầu. Âm = đã quá mốc. */
export function msUntil(fromIso: string | null, afterMs: number, now: number): number | null {
  if (!fromIso) return null
  return new Date(fromIso).getTime() + afterMs - now
}

/** Đếm ngược dạng m:ss; quá mốc thì 0:00 (bộ hẹn giờ BE quét mỗi 20 giây nên có thể trễ chút). */
export function formatCountdown(ms: number): string {
  const totalSeconds = Math.max(0, Math.ceil(ms / 1000))
  const minutes = Math.floor(totalSeconds / 60)
  const seconds = totalSeconds % 60
  return `${minutes}:${seconds.toString().padStart(2, '0')}`
}
