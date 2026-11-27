import {
  BoldOutlined,
  ClearOutlined,
  ItalicOutlined,
  LinkOutlined,
  OrderedListOutlined,
  PictureOutlined,
  UnorderedListOutlined,
} from '@ant-design/icons'
import { Button, Input, Popover, Select, Space, Tooltip, message } from 'antd'
import { useCallback, useEffect, useRef, useState } from 'react'
import { htmlToMarkdown, markdownToHtml } from '../../lib/markdownRichText'

export interface RichTextEditorProps {
  id?: string
  value?: string
  onChange?: (value: string) => void
  format?: 'markdown' | 'html'
  placeholder?: string
  minHeight?: number
  enableHeadings?: boolean
  enableImages?: boolean
  disabled?: boolean
}

type BlockTag = 'p' | 'h1' | 'h2' | 'h3' | 'blockquote'

/**
 * Trinh soan thao van ban truc quan (WYSIWYG Rich Text Editor).
 * Nguoi dung dinh dang bang nut truc quan (Tieu de, In dam, In nghieng, Danh sach, Chen anh),
 * Du lieu co the phat ra Markdown sach hoac HTML de phuc vu cac noi dung da luu theo tung dinh dang.
 */
export function RichTextEditor({
  id,
  value = '',
  onChange,
  format = 'markdown',
  placeholder = 'Nhập nội dung tại đây...',
  minHeight = 200,
  enableHeadings = false,
  enableImages = false,
  disabled = false,
}: RichTextEditorProps) {
  const editorRef = useRef<HTMLDivElement | null>(null)
  const fileInputRef = useRef<HTMLInputElement | null>(null)
  const lastEmittedRef = useRef<string | null>(null)
  const savedRangeRef = useRef<Range | null>(null)

  const [isEmpty, setIsEmpty] = useState(() => !value.trim())
  const [activeFormats, setActiveFormats] = useState({
    bold: false,
    italic: false,
    ul: false,
    ol: false,
    block: 'p' as BlockTag,
  })

  const [linkPopoverOpen, setLinkPopoverOpen] = useState(false)
  const [linkUrl, setLinkUrl] = useState('')
  const [linkText, setLinkText] = useState('')

  const syncFromDom = useCallback(() => {
    const el = editorRef.current
    if (!el) return
    const nextValue = format === 'html' ? el.innerHTML : htmlToMarkdown(el.innerHTML)
    const empty = !el.textContent?.trim() && !el.querySelector('img')
    setIsEmpty(empty)
    lastEmittedRef.current = nextValue
    onChange?.(nextValue)
  }, [format, onChange])

  // Chi cap nhat innerHTML khi gia tri thay doi tu ben ngoai (tai ban ghi, reset form, tai anh)
  useEffect(() => {
    const el = editorRef.current
    if (!el) return
    const incoming = value ?? ''
    if (lastEmittedRef.current === incoming) {
      return
    }
    el.innerHTML = format === 'html' ? incoming : markdownToHtml(incoming)
    lastEmittedRef.current = incoming
    setIsEmpty(!incoming.trim())
  }, [format, value])

  const updateToolbarState = useCallback(() => {
    const el = editorRef.current
    if (!el) return
    const selection = window.getSelection()
    if (!selection || selection.rangeCount === 0) return
    const anchorNode = selection.anchorNode
    if (!anchorNode || !el.contains(anchorNode)) return

    savedRangeRef.current = selection.getRangeAt(0).cloneRange()

    let block: BlockTag = 'p'
    let cursor: Node | null = anchorNode
    while (cursor && cursor !== el) {
      if (cursor.nodeType === Node.ELEMENT_NODE) {
        const tag = (cursor as Element).tagName.toLowerCase()
        if (tag === 'h1' || tag === 'h2' || tag === 'h3' || tag === 'blockquote') {
          block = tag
          break
        }
      }
      cursor = cursor.parentNode
    }

    setActiveFormats({
      bold: document.queryCommandState('bold'),
      italic: document.queryCommandState('italic'),
      ul: document.queryCommandState('insertUnorderedList'),
      ol: document.queryCommandState('insertOrderedList'),
      block,
    })
  }, [])

  useEffect(() => {
    document.addEventListener('selectionchange', updateToolbarState)
    return () => document.removeEventListener('selectionchange', updateToolbarState)
  }, [updateToolbarState])

  function restoreFocusAndSelection() {
    const el = editorRef.current
    if (!el) return
    el.focus()
    const selection = window.getSelection()
    if (savedRangeRef.current && selection && el.contains(savedRangeRef.current.startContainer)) {
      selection.removeAllRanges()
      selection.addRange(savedRangeRef.current)
    }
  }

  function runCommand(command: string, arg?: string) {
    if (disabled) return
    restoreFocusAndSelection()
    document.execCommand(command, false, arg)
    syncFromDom()
    updateToolbarState()
  }

  function handleBlockChange(block: BlockTag) {
    if (disabled) return
    restoreFocusAndSelection()
    document.execCommand('formatBlock', false, block)
    syncFromDom()
    updateToolbarState()
  }

  function handlePaste(e: React.ClipboardEvent<HTMLDivElement>) {
    e.preventDefault()
    if (disabled) return
    const plain = e.clipboardData.getData('text/plain')
    if (plain) {
      document.execCommand('insertText', false, plain)
      syncFromDom()
    }
  }

  function handleInsertLink() {
    const rawUrl = linkUrl.trim()
    if (!rawUrl) {
      setLinkPopoverOpen(false)
      return
    }
    const normalizedUrl = /^https?:\/\//i.test(rawUrl)
      || rawUrl.startsWith('/')
      || rawUrl.startsWith('mailto:')
      || /^\{\{[A-Za-z][A-Za-z0-9_]*\}\}$/.test(rawUrl)
      ? rawUrl
      : `https://${rawUrl}`
    restoreFocusAndSelection()
    const selection = window.getSelection()
    const selectedText = selection?.toString().trim() ?? ''
    const display = linkText.trim() || selectedText || normalizedUrl

    const anchor = document.createElement('a')
    anchor.setAttribute('href', normalizedUrl)
    if (format === 'markdown') {
      anchor.target = '_blank'
      anchor.rel = 'noopener noreferrer'
    }
    anchor.textContent = display

    if (selection && selection.rangeCount > 0 && editorRef.current?.contains(selection.anchorNode)) {
      const range = selection.getRangeAt(0)
      range.deleteContents()
      range.insertNode(anchor)
      range.setStartAfter(anchor)
      range.collapse(true)
      selection.removeAllRanges()
      selection.addRange(range)
    } else {
      editorRef.current?.appendChild(anchor)
    }

    setLinkPopoverOpen(false)
    setLinkUrl('')
    setLinkText('')
    syncFromDom()
  }

  function handleImageSelected(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0]
    e.target.value = ''
    if (!file) return
    if (!file.type.startsWith('image/')) {
      message.error('Vui lòng chọn tệp hình ảnh hợp lệ.')
      return
    }
    if (file.size > 2 * 1024 * 1024) {
      message.error('Ảnh minh họa tối đa 2MB.')
      return
    }
    const reader = new FileReader()
    reader.onload = () => {
      const dataUrl = typeof reader.result === 'string' ? reader.result : ''
      if (!dataUrl || !editorRef.current) return

      restoreFocusAndSelection()
      const p = document.createElement('p')
      const img = document.createElement('img')
      img.src = dataUrl
      img.alt = file.name.replace(/\.[^.]+$/, '') || 'Hình minh họa'
      p.appendChild(img)

      const selection = window.getSelection()
      if (selection && selection.rangeCount > 0 && editorRef.current.contains(selection.anchorNode)) {
        const range = selection.getRangeAt(0)
        range.collapse(false)
        range.insertNode(p)
        range.setStartAfter(p)
        range.collapse(true)
        selection.removeAllRanges()
        selection.addRange(range)
      } else {
        editorRef.current.insertBefore(p, editorRef.current.firstChild)
      }

      syncFromDom()
      message.success('Đã chèn ảnh vào nội dung.')
    }
    reader.readAsDataURL(file)
  }

  return (
    <div
      className={`overflow-hidden rounded-xl border transition-colors ${
        disabled
          ? 'cursor-not-allowed border-slate-200 bg-slate-50 opacity-70 dark:border-neutral-800 dark:bg-neutral-900'
          : 'border-slate-300 bg-white focus-within:border-teal-600 dark:border-neutral-700 dark:bg-neutral-900 dark:focus-within:border-teal-500'
      }`}
    >
      {/* Toolbar */}
      <div className="flex flex-wrap items-center gap-1 border-b border-slate-200 bg-slate-50/90 px-2.5 py-2 dark:border-neutral-800 dark:bg-neutral-800/70">
        {enableHeadings && (
          <>
            <Select<BlockTag>
              size="small"
              value={activeFormats.block}
              onChange={handleBlockChange}
              disabled={disabled}
              className="w-40! shrink-0"
              style={{ width: 156, flexShrink: 0 }}
              options={[
                { value: 'p', label: 'Đoạn văn thường' },
                { value: 'h1', label: 'Tiêu đề lớn' },
                { value: 'h2', label: 'Tiêu đề mục' },
                { value: 'h3', label: 'Mục nhỏ' },
                { value: 'blockquote', label: 'Trích dẫn' },
              ]}
            />
            <span className="mx-1 inline-block h-4 w-px shrink-0 bg-slate-200 dark:bg-neutral-700" aria-hidden="true" />
          </>
        )}

        <Tooltip title="In đậm (Ctrl+B)">
          <Button
            size="small"
            type={activeFormats.bold ? 'primary' : 'text'}
            ghost={activeFormats.bold}
            icon={<BoldOutlined />}
            disabled={disabled}
            onMouseDown={(e) => e.preventDefault()}
            onClick={() => runCommand('bold')}
          />
        </Tooltip>

        <Tooltip title="In nghiêng (Ctrl+I)">
          <Button
            size="small"
            type={activeFormats.italic ? 'primary' : 'text'}
            ghost={activeFormats.italic}
            icon={<ItalicOutlined />}
            disabled={disabled}
            onMouseDown={(e) => e.preventDefault()}
            onClick={() => runCommand('italic')}
          />
        </Tooltip>

        <span className="mx-1 inline-block h-4 w-px shrink-0 bg-slate-200 dark:bg-neutral-700" aria-hidden="true" />

        <Tooltip title="Danh sách gạch đầu dòng">
          <Button
            size="small"
            type={activeFormats.ul ? 'primary' : 'text'}
            ghost={activeFormats.ul}
            icon={<UnorderedListOutlined />}
            disabled={disabled}
            onMouseDown={(e) => e.preventDefault()}
            onClick={() => runCommand('insertUnorderedList')}
          />
        </Tooltip>

        <Tooltip title="Danh sách đánh số">
          <Button
            size="small"
            type={activeFormats.ol ? 'primary' : 'text'}
            ghost={activeFormats.ol}
            icon={<OrderedListOutlined />}
            disabled={disabled}
            onMouseDown={(e) => e.preventDefault()}
            onClick={() => runCommand('insertOrderedList')}
          />
        </Tooltip>

        <span className="mx-1 inline-block h-4 w-px shrink-0 bg-slate-200 dark:bg-neutral-700" aria-hidden="true" />

        <Popover
          trigger="click"
          open={linkPopoverOpen}
          onOpenChange={(open) => {
            setLinkPopoverOpen(open)
            if (open) {
              const selected = window.getSelection()?.toString().trim() ?? ''
              setLinkText(selected)
              setLinkUrl('')
            }
          }}
          content={
            <div className="flex w-64 flex-col gap-2">
              <Input
                size="small"
                placeholder="Văn bản hiển thị (tuỳ chọn)"
                value={linkText}
                onChange={(e) => setLinkText(e.target.value)}
              />
              <Input
                size="small"
                placeholder="https://..."
                value={linkUrl}
                onChange={(e) => setLinkUrl(e.target.value)}
                onPressEnter={handleInsertLink}
              />
              <Space className="justify-end">
                <Button size="small" onClick={() => setLinkPopoverOpen(false)}>
                  Huỷ
                </Button>
                <Button size="small" type="primary" onClick={handleInsertLink}>
                  Chèn
                </Button>
              </Space>
            </div>
          }
        >
          <Tooltip title="Chèn liên kết">
            <Button
              size="small"
              type="text"
              icon={<LinkOutlined />}
              disabled={disabled}
              onMouseDown={(e) => e.preventDefault()}
            />
          </Tooltip>
        </Popover>

        {enableImages && (
          <>
            <input
              ref={fileInputRef}
              type="file"
              accept="image/*"
              className="hidden!"
              style={{ display: 'none' }}
              onChange={handleImageSelected}
            />
            <Tooltip title="Chèn ảnh vào vị trí con trỏ">
              <Button
                size="small"
                type="text"
                icon={<PictureOutlined />}
                disabled={disabled}
                onMouseDown={(e) => e.preventDefault()}
                onClick={() => fileInputRef.current?.click()}
              >
                Chèn ảnh
              </Button>
            </Tooltip>
          </>
        )}

        <Tooltip title="Xoá định dạng vùng chọn">
          <Button
            size="small"
            type="text"
            icon={<ClearOutlined />}
            disabled={disabled}
            onMouseDown={(e) => e.preventDefault()}
            onClick={() => {
              runCommand('formatBlock', 'p')
              runCommand('removeFormat')
            }}
          />
        </Tooltip>
      </div>

      {/* Editable surface */}
      <div className="relative">
        {isEmpty && (
          <div
            aria-hidden="true"
            className="pointer-events-none absolute left-4 top-3.5 text-sm text-slate-400 dark:text-neutral-500"
          >
            {placeholder}
          </div>
        )}
        <div
          id={id}
          ref={editorRef}
          contentEditable={!disabled}
          suppressContentEditableWarning
          onInput={syncFromDom}
          onBlur={syncFromDom}
          onPaste={handlePaste}
          onKeyUp={updateToolbarState}
          onMouseUp={updateToolbarState}
          style={{ minHeight }}
          className="rte-surface max-h-[520px] overflow-y-auto px-4 py-3 text-sm leading-relaxed text-slate-800 outline-none dark:text-slate-100 [&_a]:text-teal-600 [&_a]:underline [&_blockquote]:my-2 [&_blockquote]:border-l-2 [&_blockquote]:border-teal-500/50 [&_blockquote]:pl-3 [&_blockquote]:italic [&_blockquote]:opacity-90 [&_h1]:mb-2 [&_h1]:mt-3 [&_h1]:text-xl [&_h1]:font-bold [&_h2]:mb-2 [&_h2]:mt-3 [&_h2]:text-lg [&_h2]:font-bold [&_h3]:mb-1.5 [&_h3]:mt-2.5 [&_h3]:text-base [&_h3]:font-semibold [&_img]:mx-auto [&_img]:my-3 [&_img]:block [&_img]:max-h-[320px] [&_img]:max-w-full [&_img]:rounded-xl [&_img]:border [&_img]:border-slate-200 [&_img]:bg-white [&_img]:p-1.5 [&_img]:shadow-xs [&_ol]:my-2 [&_ol]:list-decimal [&_ol]:space-y-1 [&_ol]:pl-5 [&_p]:my-1.5 [&_ul]:my-2 [&_ul]:list-disc [&_ul]:space-y-1 [&_ul]:pl-5"
        />
      </div>
    </div>
  )
}

