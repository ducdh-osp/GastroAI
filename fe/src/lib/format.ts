/** Định dạng ISO datetime -> "DD/MM/YYYY HH:mm" theo giờ Việt Nam, dùng chung nhiều trang. */
export function formatDateTime(iso: string): string {
  return new Date(iso).toLocaleString('vi-VN', {
    day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit',
  })
}

/** Rút gọn tiêu đề phiên chat, gỡ markdown thô và cắt theo ranh giới từ để không cụt giữa chữ. */
export function formatChatSessionTitle(raw: string, maxLength = 80): string {
  const plain = raw
    .replace(/[*_`~>#]+/g, '')
    .replace(/\s+/g, ' ')
    .trim()
  if (plain.length <= maxLength) return plain
  const sliced = plain.slice(0, maxLength)
  const lastSpace = sliced.lastIndexOf(' ')
  const clean = lastSpace >= Math.floor(maxLength * 0.5) ? sliced.slice(0, lastSpace) : sliced
  return `${clean.trimEnd()}…`
}

const SYMPTOM_LABELS_VI: Record<string, string> = {
  ABDOMINAL_PAIN: 'Đau bụng',
  DIARRHEA: 'Tiêu chảy',
  CONSTIPATION: 'Táo bón',
  NAUSEA: 'Buồn nôn',
  VOMITING: 'Nôn',
  HEARTBURN: 'Ợ nóng / trào ngược',
  BLOATING: 'Đầy bụng',
  OTHER: 'Khác',
}

/** Chuyển mã triệu chứng tiếng Anh (vd: OTHER) sang nhãn tiếng Việt đồng nhất trên các biểu đồ CMS. */
export function formatSymptomLabel(nameOrCode: string): string {
  return SYMPTOM_LABELS_VI[nameOrCode] ?? nameOrCode
}
