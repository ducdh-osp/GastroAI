import { CloseOutlined, FileTextOutlined, PaperClipOutlined, SendOutlined } from '@ant-design/icons'
import { Alert, Button } from 'antd'
import { useEffect, useRef, useState, type ChangeEvent, type KeyboardEvent } from 'react'
import type { Attachment } from './chat.types'

const MAX_LENGTH = 1000
const MAX_FILE_SIZE = 10 * 1024 * 1024
const ACCEPTED_DOCUMENT_TYPES = new Set([
  'application/pdf',
  'application/msword',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
])

interface ChatInputProps {
  disabled?: boolean
  onSend: (content: string, attachments: Attachment[]) => void
}

export function ChatInput({ disabled = false, onSend }: ChatInputProps) {
  const [content, setContent] = useState('')
  const [attachments, setAttachments] = useState<Attachment[]>([])
  const [fileError, setFileError] = useState<string | null>(null)
  const textareaRef = useRef<HTMLTextAreaElement>(null)
  const fileInputRef = useRef<HTMLInputElement>(null)
  const objectUrls = useRef(new Set<string>())

  useEffect(() => {
    const textarea = textareaRef.current
    if (!textarea) return
    textarea.style.height = 'auto'
    textarea.style.height = `${Math.min(textarea.scrollHeight, 136)}px`
  }, [content])

  useEffect(() => () => {
    objectUrls.current.forEach((url) => URL.revokeObjectURL(url))
  }, [])

  function submit() {
    const trimmedContent = content.trim()
    if (disabled || !trimmedContent || trimmedContent.length > MAX_LENGTH) return
    onSend(trimmedContent, attachments)
    setContent('')
    setAttachments([])
  }

  function handleKeyDown(event: KeyboardEvent<HTMLTextAreaElement>) {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault()
      submit()
    }
  }

  function handleFiles(event: ChangeEvent<HTMLInputElement>) {
    setFileError(null)
    const selectedFiles = Array.from(event.target.files ?? [])
    const acceptedFiles: File[] = []

    for (const file of selectedFiles) {
      const acceptedType = file.type.startsWith('image/') || ACCEPTED_DOCUMENT_TYPES.has(file.type)
      if (!acceptedType) {
        setFileError('Chỉ hỗ trợ tệp ảnh, PDF, DOC hoặc DOCX.')
        continue
      }
      if (file.size > MAX_FILE_SIZE) {
        setFileError('Mỗi tệp đính kèm không được vượt quá 10 MB.')
        continue
      }
      acceptedFiles.push(file)
    }

    setAttachments((current) => [
      ...current,
      ...acceptedFiles.map((file) => {
        const url = URL.createObjectURL(file)
        objectUrls.current.add(url)
        return {
          id: `${file.name}-${file.lastModified}-${crypto.randomUUID()}`,
          name: file.name,
          type: file.type,
          size: file.size,
          url,
          previewUrl: file.type.startsWith('image/') ? url : undefined,
        }
      }),
    ])
    event.target.value = ''
  }

  function removeAttachment(id: string) {
    setAttachments((current) => {
      const attachment = current.find((item) => item.id === id)
      if (attachment) {
        URL.revokeObjectURL(attachment.url)
        objectUrls.current.delete(attachment.url)
      }
      return current.filter((item) => item.id !== id)
    })
  }

  return (
    <div className="border-t border-slate-200 bg-white px-4 py-4 sm:px-6">
      <div className="mx-auto max-w-3xl">
        {fileError && <Alert className="mb-3" type="error" showIcon closable message={fileError} onClose={() => setFileError(null)} />}

        {attachments.length > 0 && (
          <div className="mb-3 flex flex-wrap gap-2" aria-label="Các tệp đã chọn">
            {attachments.map((attachment) => (
              <div key={attachment.id} className="relative flex items-center gap-2 rounded-lg border border-slate-200 bg-slate-50 p-1.5 pr-8">
                {attachment.previewUrl ? <img src={attachment.previewUrl} alt={attachment.name} className="h-10 w-10 rounded object-cover" /> : <FileTextOutlined className="px-2 text-2xl text-teal-700" />}
                <span className="max-w-40 truncate text-xs text-slate-700">{attachment.name}</span>
                <button type="button" aria-label={`Xóa tệp ${attachment.name}`} onClick={() => removeAttachment(attachment.id)} className="absolute right-1 top-1 rounded p-1 text-slate-400 hover:text-red-600 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-teal-500"><CloseOutlined /></button>
              </div>
            ))}
          </div>
        )}

        <div className="flex items-end gap-2 rounded-2xl border border-slate-300 bg-slate-50 p-2 transition focus-within:border-teal-500 focus-within:ring-2 focus-within:ring-teal-100">
          <input ref={fileInputRef} type="file" accept="image/*,.pdf,.doc,.docx" multiple className="hidden" onChange={handleFiles} />
          <Button type="text" shape="circle" icon={<PaperClipOutlined />} aria-label="Đính kèm ảnh hoặc tài liệu" disabled={disabled} onClick={() => fileInputRef.current?.click()} />
          <textarea ref={textareaRef} value={content} rows={1} maxLength={MAX_LENGTH} disabled={disabled} onChange={(event) => setContent(event.target.value)} onKeyDown={handleKeyDown} placeholder="Nhập câu hỏi sức khỏe của bạn..." aria-label="Nội dung câu hỏi" className="max-h-[136px] min-h-8 flex-1 resize-none bg-transparent px-1 py-1.5 text-[15px] leading-6 text-slate-800 outline-none placeholder:text-slate-400" />
          <Button type="primary" shape="circle" icon={<SendOutlined />} aria-label="Gửi câu hỏi" disabled={disabled || !content.trim()} onClick={submit} />
        </div>
        <div className="mt-1 flex justify-between px-1 text-xs text-slate-400"><span>Enter để gửi · Shift + Enter để xuống dòng</span><span className={content.length >= MAX_LENGTH ? 'text-red-600' : ''}>{content.length}/{MAX_LENGTH}</span></div>
      </div>
    </div>
  )
}
