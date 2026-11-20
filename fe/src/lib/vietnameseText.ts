export function foldVietnamese(text: string): string {
  let result = ''
  for (const ch of text) {
    if (/\s/.test(ch)) {
      result += ' '
      continue
    }
    const folded = ch.toLowerCase().replace('đ', 'd').normalize('NFD').replace(/[\u0300-\u036f]/g, '')
    // Chữ đặc biệt làm thay đổi độ dài (hiếm) thì giữ nguyên để không lệch vị trí.
    result += folded.length === ch.length ? folded : ch
  }
  return result
}

/** Chữ thường, giữ dấu, khoảng trắng thành dấu cách - từng chữ một nên độ dài không đổi. */
function lowerKeepingAccents(text: string): string {
  let result = ''
  for (const ch of text) {
    const lowered = /\s/.test(ch) ? ' ' : ch.toLowerCase()
    result += lowered.length === ch.length ? lowered : ch
  }
  return result
}

/**
 * Các khoảng [bắt đầu, kết thúc) trong text khớp từ khoá, không phân biệt hoa/thường. Giống BE
 * (DocumentContentSearchService): gõ không dấu thì khớp mọi cách viết, gõ có dấu thì khớp đúng chữ đó.
 */
export function findMatches(text: string, query: string): [number, number][] {
  const q = query.normalize('NFC').trim().replace(/\s+/g, ' ')
  if (!q) return []
  const keepAccents = foldVietnamese(q) !== lowerKeepingAccents(q)
  const normalize = keepAccents ? lowerKeepingAccents : foldVietnamese
  const needle = normalize(q)
  const haystack = normalize(text.normalize('NFC'))
  const ranges: [number, number][] = []
  let from = 0
  while (from <= haystack.length) {
    const index = haystack.indexOf(needle, from)
    if (index < 0) break
    ranges.push([index, index + needle.length])
    from = index + needle.length
  }
  return ranges
}

/** Chữ cái thường (kể cả chữ có dấu) - chữ hoa / số / ký hiệu đều trả false. */
function isLowercaseLetter(ch: string): boolean {
  return ch !== ch.toUpperCase() && ch === ch.toLowerCase()
}

export interface TextBlock {
  text: string
  /** Dòng tiêu đề ngắn như "Tổng quan", "Nguyên nhân / Yếu tố nguy cơ" - hiển thị in đậm. */
  heading: boolean
}

/**
 * Chữ trích từ PDF bị xuống dòng theo khổ giấy. Nối lại thành đoạn văn liền theo quy luật của văn bản
 * tiếng Việt: dòng bị ngắt giữa câu thì dòng sau bắt đầu bằng chữ thường / dấu câu ("…như một van" +
 * "một chiều", hoặc mở ngoặc "(adenocarcinoma)") -> nối; câu mới hoặc gạch đầu dòng (dấu "•" thường mất khi trích) bắt đầu bằng chữ hoa
 * -> xuống dòng. Sau đó đánh dấu dòng tiêu đề.
 */
export function toReadableBlocks(text: string): TextBlock[] {
  const lines = text
    .normalize('NFC')
    .replace(/\r\n?/g, '\n')
    .split('\n')
    .map((line) => line.replace(/\s+/g, ' ').trim())

  const merged: { text: string; lines: number }[] = []
  let current: { text: string; lines: number } | null = null
  for (const line of lines) {
    if (!line) {
      if (current) merged.push(current)
      current = null
      continue
    }
    const first = line[0]
    const numberedItem = /^\d+[.)]\s/.test(line)
    const continuesSentence =
      !numberedItem && (isLowercaseLetter(first) || /[0-9,.;:()%\]…]/.test(first))
    // Dòng trước bỏ lửng giữa chừng (dấu phẩy, gạch nối, mở ngoặc) thì dòng sau là phần tiếp theo,
    // kể cả khi bắt đầu bằng chữ hoa (tên riêng như "Helicobacter").
    const previousOpen = current !== null && /[,(\-–/]$/.test(current.text)
    if (current && (continuesSentence || previousOpen)) {
      current = { text: `${current.text} ${line}`, lines: current.lines + 1 }
    } else {
      if (current) merged.push(current)
      current = { text: line, lines: 1 }
    }
  }
  if (current) merged.push(current)

  const blocks: TextBlock[] = []
  merged.forEach((block, index) => {
    const previous = blocks[index - 1]
    const next = merged[index + 1]
    // Sau tiêu đề phải là 1 câu / đoạn văn thật; 1 loạt dòng ngắn liền nhau ("Đau bụng", "Buồn nôn")
    // là danh sách, không phải tiêu đề.
    const nextIsSentence =
      next !== undefined && (next.lines > 1 || next.text.length > 60 || /[.!?:;]$/.test(next.text))
    // Tiêu đề: 1 dòng ngắn, không dấu câu cuối, không dấu phẩy (gạch đầu dòng hay có phẩy), theo sau là
    // 1 câu thật, và đứng sau 1 câu đã kết thúc hoặc 1 tiêu đề khác (gạch đầu dòng thì đứng sau gạch đầu dòng).
    const heading =
      block.lines === 1 &&
      block.text.length <= 60 &&
      !/[.,:;!?]$/.test(block.text) &&
      !block.text.includes(',') &&
      !/^([-•*+]|\d+[.)])\s/.test(block.text) &&
      !isLowercaseLetter(block.text[0]) &&
      nextIsSentence &&
      (!previous || previous.heading || /[.!?:]$/.test(previous.text))
    blocks.push({ text: block.text, heading })
  })
  return blocks
}

/**
 * Các câu trích ngắn quanh từng chỗ khớp trong 1 đoạn (giống kết quả tìm của Google). Mỗi câu trích nằm
 * gọn trong 1 dòng / gạch đầu dòng, dài cố định; chỗ khớp đã nằm trong câu trích trước thì không lặp lại.
 */
export function findExcerpts(text: string, query: string, radius = 70): { excerpts: string[]; matchCount: number } {
  const excerpts: string[] = []
  let matchCount = 0
  for (const block of toReadableBlocks(text)) {
    const line = block.text
    const ranges = findMatches(line, query)
    matchCount += ranges.length
    let coveredUntil = -1
    for (const [startMatch, endMatch] of ranges) {
      if (startMatch < coveredUntil) continue
      let start = Math.max(0, startMatch - radius)
      let end = Math.min(line.length, endMatch + radius)
      // Cắt ở dấu cách gần nhất để không đứt giữa chữ.
      if (start > 0) {
        const space = line.indexOf(' ', start)
        if (space !== -1 && space < startMatch) start = space + 1
      }
      if (end < line.length) {
        const space = line.lastIndexOf(' ', end)
        if (space > endMatch) end = space
      }
      excerpts.push(`${start > 0 ? '…' : ''}${line.slice(start, end)}${end < line.length ? '…' : ''}`)
      coveredUntil = end
    }
  }
  return { excerpts, matchCount }
}

/** 1 khối để hiển thị: tiêu đề, đoạn văn, hoặc bảng nhãn - giá trị (file PDF dạng bảng). */
export type DisplayBlock =
  | { kind: 'heading'; text: string }
  | { kind: 'paragraph'; text: string }
  | { kind: 'pairs'; rows: [string, string][] }

/** Ô bảng: 1 dòng ngắn, không phải câu (không phẩy, không dấu chấm câu cuối, không gạch đầu dòng). */
function isTableCell(block: TextBlock): boolean {
  return (
    block.text.length <= 40 &&
    !block.text.includes(',') &&
    !/[.;!?]$/.test(block.text) &&
    !/^([-•*+]|\d+[.)])\s/.test(block.text)
  )
}

/** Giá trị kiểu bảng: để trống ("—") hoặc là số / số đo ("13.5 g/dL", "<5"). */
function looksLikeValue(text: string): boolean {
  return /^[—–-]$/.test(text) || /^[<>≤≥]?\d/.test(text)
}

/**
 * Thử đọc 1 loạt ô liền nhau thành bảng nhãn - giá trị. Trả về số ô dùng hết (kể cả tên bảng) và các
 * hàng, hoặc null nếu không giống bảng. Các cặp cuối không có giá trị kiểu bảng ("Hồ sơ bệnh lý" /
 * "Nhật ký Bristol" ngay sau bảng) bị cắt ra, để phần đó hiện như chữ thường.
 */
function readTable(cells: string[]): { used: number; title: string | null; rows: [string, string][] } | null {
  let best: { used: number; title: string | null; rows: [string, string][] } | null = null
  // Ô đầu có thể là tên bảng ("Thông tin bệnh nhân") - thử cả 2 cách xếp cặp, lấy cách ra nhiều hàng hơn.
  for (const titleCount of [0, 1]) {
    const body = cells.slice(titleCount)
    let pairCount = Math.floor(body.length / 2)
    while (pairCount > 0 && !looksLikeValue(body[pairCount * 2 - 1])) pairCount--
    const rows: [string, string][] = []
    for (let pair = 0; pair < pairCount; pair++) rows.push([body[pair * 2], body[pair * 2 + 1]])
    const valueLike = rows.filter(([, value]) => looksLikeValue(value)).length
    if (rows.length >= 2 && valueLike * 2 >= rows.length && (!best || rows.length > best.rows.length)) {
      best = { used: titleCount + pairCount * 2, title: titleCount ? cells[0] : null, rows }
    }
  }
  return best
}

/**
 * Như toReadableBlocks, nhưng gom các dòng rất ngắn liền nhau của file dạng bảng ("Họ tên" / "Bệnh nhân
 * Test" / "Số điện thoại" / "—") thành bảng 2 cột nhãn - giá trị. Chỉ gom khi chắc là bảng: từ 2 cặp trở
 * lên và ít nhất nửa số giá trị là "—" hoặc số - danh sách triệu chứng ngắn ("Đau bụng", "Buồn nôn")
 * không bị gom nhầm.
 */
export function toDisplayBlocks(text: string): DisplayBlock[] {
  const blocks = toReadableBlocks(text)
  const result: DisplayBlock[] = []
  let index = 0
  while (index < blocks.length) {
    let end = index
    while (end < blocks.length && isTableCell(blocks[end])) end++
    const table = readTable(blocks.slice(index, end).map((block) => block.text))
    if (table) {
      if (table.title) result.push({ kind: 'heading', text: table.title })
      result.push({ kind: 'pairs', rows: table.rows })
      index += table.used
    } else {
      const block = blocks[index]
      result.push(block.heading ? { kind: 'heading', text: block.text } : { kind: 'paragraph', text: block.text })
      index++
    }
  }
  return result
}