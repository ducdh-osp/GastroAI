function escapeHtml(text: string): string {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

function isSafeImageSrc(src: string): boolean {
  const trimmed = src.trim()
  return (
    trimmed.startsWith('http://') ||
    trimmed.startsWith('https://') ||
    trimmed.startsWith('/') ||
    trimmed.startsWith('data:image/')
  )
}

function isSafeLinkHref(href: string): boolean {
  const trimmed = href.trim()
  return (
    trimmed.startsWith('http://') ||
    trimmed.startsWith('https://') ||
    trimmed.startsWith('/') ||
    trimmed.startsWith('mailto:')
  )
}

function formatInlineMarkdown(raw: string): string {
  const tokens: string[] = []
  const stash = (html: string) => {
    const idx = tokens.length
    tokens.push(html)
    return `\uE000T${idx}\uE001`
  }

  let text = escapeHtml(raw)

  // Stash images and links first so underscores or asterisks in URLs are untouched
  text = text.replace(/!\[([^\]]*)\]\(([^)\s]+)\)/g, (_, alt: string, src: string) => {
    const decodedSrc = src.replace(/&amp;/g, '&')
    if (!isSafeImageSrc(decodedSrc)) return alt
    return stash(`<img src="${escapeHtml(decodedSrc)}" alt="${escapeHtml(alt)}" />`)
  })

  text = text.replace(/\[([^\]]+)\]\(([^)\s]+)\)/g, (_, label: string, href: string) => {
    const decodedHref = href.replace(/&amp;/g, '&')
    if (!isSafeLinkHref(decodedHref)) return label
    return stash(`<a href="${escapeHtml(decodedHref)}" target="_blank" rel="noopener noreferrer">${label}</a>`)
  })

  text = text.replace(/`([^`]+)`/g, (_, code: string) => stash(`<code>${code}</code>`))

  // Bold before italic
  text = text.replace(/\*\*([^*]+?)\*\*/g, '<strong>$1</strong>')
  text = text.replace(/__([^_]+?)__/g, '<strong>$1</strong>')
  text = text.replace(/\*([^*\n]+?)\*/g, '<em>$1</em>')

  // Restore tokens
  text = text.replace(/\uE000T(\d+)\uE001/g, (_, idxStr: string) => tokens[Number(idxStr)] ?? '')
  return text
}

/**
 * Chuyen doi Markdown luu trong CSDL thanh HTML sach de chinh sua tren RichTextEditor (WYSIWYG).
 * Ho tro heading (#..###), danh sach (-, 1.), trich dan (>), in dam, in nghieng, link va anh.
 */
export function markdownToHtml(markdown: string): string {
  if (!markdown || !markdown.trim()) {
    return ''
  }

  const lines = markdown.replace(/\r\n/g, '\n').split('\n')
  const blocks: string[] = []
  let i = 0

  while (i < lines.length) {
    const line = lines[i]
    const trimmed = line.trim()

    if (!trimmed) {
      i++
      continue
    }

    const headingMatch = /^(#{1,3})\s+(.*)$/.exec(trimmed)
    if (headingMatch) {
      const level = headingMatch[1].length
      blocks.push(`<h${level}>${formatInlineMarkdown(headingMatch[2].trim())}</h${level}>`)
      i++
      continue
    }

    if (/^>\s?/.test(trimmed)) {
      const quoteLines: string[] = []
      while (i < lines.length && /^>\s?/.test(lines[i].trim())) {
        quoteLines.push(lines[i].trim().replace(/^>\s?/, ''))
        i++
      }
      blocks.push(`<blockquote><p>${quoteLines.map(formatInlineMarkdown).join('<br>')}</p></blockquote>`)
      continue
    }

    if (/^[-*+]\s+/.test(trimmed)) {
      const items: string[] = []
      while (i < lines.length && /^[-*+]\s+/.test(lines[i].trim())) {
        items.push(`<li>${formatInlineMarkdown(lines[i].trim().replace(/^[-*+]\s+/, ''))}</li>`)
        i++
      }
      blocks.push(`<ul>${items.join('')}</ul>`)
      continue
    }

    if (/^\d+\.\s+/.test(trimmed)) {
      const items: string[] = []
      while (i < lines.length && /^\d+\.\s+/.test(lines[i].trim())) {
        items.push(`<li>${formatInlineMarkdown(lines[i].trim().replace(/^\d+\.\s+/, ''))}</li>`)
        i++
      }
      blocks.push(`<ol>${items.join('')}</ol>`)
      continue
    }

    // Standalone image line
    const imageOnlyMatch = /^!\[([^\]]*)\]\(([^)\s]+)\)$/.exec(trimmed)
    if (imageOnlyMatch && isSafeImageSrc(imageOnlyMatch[2])) {
      blocks.push(`<p><img src="${escapeHtml(imageOnlyMatch[2])}" alt="${escapeHtml(imageOnlyMatch[1])}" /></p>`)
      i++
      continue
    }

    // Regular paragraph lines
    const paraLines: string[] = []
    while (
      i < lines.length &&
      lines[i].trim() !== '' &&
      !/^(#{1,3})\s+/.test(lines[i].trim()) &&
      !/^>\s?/.test(lines[i].trim()) &&
      !/^[-*+]\s+/.test(lines[i].trim()) &&
      !/^\d+\.\s+/.test(lines[i].trim())
    ) {
      paraLines.push(formatInlineMarkdown(lines[i]))
      i++
    }
    blocks.push(`<p>${paraLines.join('<br>')}</p>`)
  }

  return blocks.join('')
}

function wrapWithWhitespacePreserved(inner: string, prefix: string, suffix: string): string {
  if (!inner) return ''
  const leading = /^\s*/.exec(inner)?.[0] ?? ''
  const trailing = /\s*$/.exec(inner)?.[0] ?? ''
  const core = inner.trim()
  if (!core) return inner
  return `${leading}${prefix}${core}${suffix}${trailing}`
}

function serializeChildren(node: Node): string {
  let out = ''
  node.childNodes.forEach((child) => {
    out += serializeNode(child)
  })
  return out
}

function serializeInlineListContent(li: Element): string {
  const raw = serializeChildren(li)
  return raw
    .replace(/\u00a0/g, ' ')
    .replace(/\n+/g, ' ')
    .trim()
}

function isBlockTag(tag: string): boolean {
  return ['P', 'DIV', 'H1', 'H2', 'H3', 'H4', 'H5', 'H6', 'UL', 'OL', 'BLOCKQUOTE', 'HR'].includes(tag)
}

function serializeNode(node: Node): string {
  if (node.nodeType === Node.TEXT_NODE) {
    return (node.textContent ?? '').replace(/\u00a0/g, ' ')
  }
  if (node.nodeType !== Node.ELEMENT_NODE) {
    return ''
  }

  const el = node as HTMLElement
  const tag = el.tagName.toUpperCase()

  switch (tag) {
    case 'BR':
      return '\n'
    case 'HR':
      return '\n\n---\n\n'
    case 'STRONG':
    case 'B': {
      const inner = serializeChildren(el)
      return wrapWithWhitespacePreserved(inner, '**', '**')
    }
    case 'EM':
    case 'I': {
      const inner = serializeChildren(el)
      return wrapWithWhitespacePreserved(inner, '*', '*')
    }
    case 'U':
      return serializeChildren(el)
    case 'CODE': {
      const inner = serializeChildren(el)
      return wrapWithWhitespacePreserved(inner, '`', '`')
    }
    case 'SPAN': {
      let inner = serializeChildren(el)
      const weight = el.style?.fontWeight
      const isBold = weight === 'bold' || (Number(weight) >= 600 && !Number.isNaN(Number(weight)))
      const isItalic = el.style?.fontStyle === 'italic'
      if (isItalic) inner = wrapWithWhitespacePreserved(inner, '*', '*')
      if (isBold) inner = wrapWithWhitespacePreserved(inner, '**', '**')
      return inner
    }
    case 'A': {
      const href = el.getAttribute('href')?.trim() ?? ''
      const inner = serializeChildren(el).trim() || href
      if (!href || !isSafeLinkHref(href)) return inner
      return `[${inner}](${href})`
    }
    case 'IMG': {
      const src = el.getAttribute('src')?.trim() ?? ''
      if (!src || !isSafeImageSrc(src)) return ''
      const alt = (el.getAttribute('alt') ?? 'Hinh minh hoa').replace(/[[\]]/g, '').trim() || 'Hinh minh hoa'
      return `![${alt}](${src})`
    }
    case 'H1':
    case 'H2':
    case 'H3': {
      const level = Number(tag.slice(1))
      const text = serializeChildren(el).replace(/\n+/g, ' ').trim()
      if (!text) return ''
      return `\n\n${'#'.repeat(level)} ${text}\n\n`
    }
    case 'BLOCKQUOTE': {
      const inner = serializeChildren(el).trim()
      if (!inner) return ''
      const quoted = inner
        .split('\n')
        .map((line) => `> ${line.trim()}`)
        .join('\n')
      return `\n\n${quoted}\n\n`
    }
    case 'UL': {
      const lines: string[] = []
      el.childNodes.forEach((child) => {
        if (child.nodeType === Node.ELEMENT_NODE && (child as Element).tagName.toUpperCase() === 'LI') {
          const item = serializeInlineListContent(child as Element)
          if (item) lines.push(`- ${item}`)
        }
      })
      return lines.length > 0 ? `\n\n${lines.join('\n')}\n\n` : ''
    }
    case 'OL': {
      const lines: string[] = []
      let index = 1
      el.childNodes.forEach((child) => {
        if (child.nodeType === Node.ELEMENT_NODE && (child as Element).tagName.toUpperCase() === 'LI') {
          const item = serializeInlineListContent(child as Element)
          if (item) lines.push(`${index++}. ${item}`)
        }
      })
      return lines.length > 0 ? `\n\n${lines.join('\n')}\n\n` : ''
    }
    case 'P':
    case 'DIV': {
      // Handle nested block elements cleanly inside contentEditable divs
      const hasChildBlock = Array.from(el.children).some((child) => isBlockTag(child.tagName.toUpperCase()))
      if (hasChildBlock) {
        return `\n\n${serializeChildren(el)}\n\n`
      }
      const raw = serializeChildren(el)
      const cleaned = raw.replace(/\n+$/g, '').trim()
      if (!cleaned) return ''
      return `\n\n${cleaned}\n\n`
    }
    default:
      return serializeChildren(el)
  }
}

/**
 * Chuyen doi HTML tu RichTextEditor sang Markdown sach de tuong thich hoan toan voi
 * MarkdownContent, RAG/tim kiem va du lieu cu trong CSDL ma nguoi dung khong phai go cu phap ky thuat.
 */
export function htmlToMarkdown(html: string): string {
  if (!html || !html.trim()) {
    return ''
  }
  const parser = new DOMParser()
  const doc = parser.parseFromString(html, 'text/html')
  const raw = serializeChildren(doc.body)
  return raw
    .replace(/\u00a0/g, ' ')
    .replace(/[ \t]+\n/g, '\n')
    .replace(/\n{3,}/g, '\n\n')
    .trim()
}

