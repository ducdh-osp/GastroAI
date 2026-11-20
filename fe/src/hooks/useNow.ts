import { useEffect, useState } from 'react'

/** Thời điểm hiện tại, cập nhật mỗi giây khi active - dùng cho đồng hồ đếm ngược. */
export function useNow(active: boolean, intervalMs = 1000): number {
  const [now, setNow] = useState(() => Date.now())
  useEffect(() => {
    if (!active) return
    const timer = window.setInterval(() => setNow(Date.now()), intervalMs)
    return () => window.clearInterval(timer)
  }, [active, intervalMs])
  return now
}
