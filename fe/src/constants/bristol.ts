/** 7 mức của thang Bristol Stool Chart - dùng chung cho form ghi nhận (UC0013) và biểu đồ
 * xu hướng (UC0014), tránh lặp lại mô tả ở nhiều nơi. */
export const BRISTOL_TYPE_LABELS: Record<number, string> = {
  1: 'Loại 1 – Cứng, rời rạc (táo bón nặng)',
  2: 'Loại 2 – Hình khúc, vón cục',
  3: 'Loại 3 – Hình khúc, có vết nứt bề mặt',
  4: 'Loại 4 – Mềm, mịn, hình chuối (lý tưởng)',
  5: 'Loại 5 – Cục mềm, rời, bờ rõ',
  6: 'Loại 6 – Bột nhão, bờ nham nhở',
  7: 'Loại 7 – Lỏng hoàn toàn, không có phần rắn (tiêu chảy)',
}

export const BRISTOL_TYPE_OPTIONS = Object.entries(BRISTOL_TYPE_LABELS).map(([value, label]) => ({
  value: Number(value),
  label,
}))

/** Nhãn ngắn gọn dùng cho trục Y biểu đồ - đủ để phân biệt, không dài như label đầy đủ. */
export function shortBristolLabel(type: number): string {
  return `Loại ${type}`
}
