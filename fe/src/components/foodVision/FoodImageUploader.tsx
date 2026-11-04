import {
  CameraOutlined,
  CloseOutlined,
  FileImageOutlined,
  UploadOutlined,
} from '@ant-design/icons'
import { Alert, Button, Typography } from 'antd'
import { useEffect, useRef, useState, type ChangeEvent } from 'react'
import { FoodCameraModal } from './FoodCameraModal'

const { Text, Title } = Typography

const MAX_IMAGE_SIZE = 10 * 1024 * 1024
const ACCEPTED_IMAGE_TYPES = new Set(['image/jpeg', 'image/png', 'image/webp'])
const IMAGE_ACCEPT = 'image/jpeg,image/png,image/webp'

interface FoodImageUploaderProps {
  file: File | null
  disabled?: boolean
  onChange: (file: File | null) => void
}

function formatFileSize(size: number) {
  return `${(size / (1024 * 1024)).toFixed(size >= 1024 * 1024 ? 1 : 2)} MB`
}

export function FoodImageUploader({ file, disabled = false, onChange }: FoodImageUploaderProps) {
  const uploadInputRef = useRef<HTMLInputElement>(null)
  const previewUrlRef = useRef<string | null>(null)
  const [previewUrl, setPreviewUrl] = useState<string | null>(null)
  const [validationError, setValidationError] = useState<string | null>(null)
  const [cameraOpen, setCameraOpen] = useState(false)

  useEffect(() => () => {
    if (previewUrlRef.current) URL.revokeObjectURL(previewUrlRef.current)
  }, [])

  function replacePreview(nextFile: File | null) {
    if (previewUrlRef.current) URL.revokeObjectURL(previewUrlRef.current)
    const nextUrl = nextFile ? URL.createObjectURL(nextFile) : null
    previewUrlRef.current = nextUrl
    setPreviewUrl(nextUrl)
  }

  function selectFile(selectedFile?: File) {
    setValidationError(null)
    if (!selectedFile) return

    if (!ACCEPTED_IMAGE_TYPES.has(selectedFile.type)) {
      setValidationError('Chỉ hỗ trợ ảnh JPEG, PNG hoặc WebP.')
      return
    }
    if (selectedFile.size === 0) {
      setValidationError('Ảnh đã chọn không có dữ liệu. Vui lòng chọn ảnh khác.')
      return
    }
    if (selectedFile.size > MAX_IMAGE_SIZE) {
      setValidationError('Ảnh không được vượt quá 10 MB.')
      return
    }

    replacePreview(selectedFile)
    onChange(selectedFile)
  }

  function handleFileInput(event: ChangeEvent<HTMLInputElement>) {
    selectFile(event.target.files?.[0])
    // Cho phép chọn lại chính file vừa xoá hoặc vừa phân tích.
    event.target.value = ''
  }

  function removeFile() {
    setValidationError(null)
    replacePreview(null)
    onChange(null)
  }

  return (
    <div>
      <input
        ref={uploadInputRef}
        type="file"
        accept={IMAGE_ACCEPT}
        className="hidden"
        disabled={disabled}
        onChange={handleFileInput}
      />

      {validationError && (
        <Alert
          type="error"
          showIcon
          closable
          className="mb-4"
          message={validationError}
          onClose={() => setValidationError(null)}
        />
      )}

      {file && previewUrl ? (
        <div className="overflow-hidden rounded-2xl border border-slate-200 bg-slate-50 dark:border-white/10 dark:bg-neutral-900">
          <div className="relative bg-slate-100 dark:bg-neutral-950">
            <img
              src={previewUrl}
              alt="Ảnh món ăn đã chọn"
              className="h-72 w-full object-contain sm:h-96"
            />
            <div className="absolute right-3 top-3 z-10">
              <Button
                type="default"
                shape="circle"
                danger
                icon={<CloseOutlined />}
                aria-label="Xóa ảnh đã chọn"
                disabled={disabled}
                className="border-white/80 bg-white/95 shadow-md hover:bg-white! dark:border-white/20 dark:bg-neutral-900/95"
                onClick={removeFile}
              />
            </div>
          </div>
          <div className="flex flex-col gap-3 p-4 sm:flex-row sm:items-center sm:justify-between">
            <div className="min-w-0">
              <Text strong className="block truncate">{file.name}</Text>
              <Text type="secondary" className="text-xs">{formatFileSize(file.size)}</Text>
            </div>
            <div className="flex gap-2">
              <Button
                icon={<CameraOutlined />}
                disabled={disabled}
                onClick={() => setCameraOpen(true)}
              >
                Chụp lại
              </Button>
              <Button
                icon={<UploadOutlined />}
                disabled={disabled}
                onClick={() => uploadInputRef.current?.click()}
              >
                Đổi ảnh
              </Button>
            </div>
          </div>
        </div>
      ) : (
        <div className="rounded-2xl border-2 border-dashed border-teal-200 bg-teal-50/60 px-5 py-10 text-center dark:border-teal-900 dark:bg-teal-950/20 sm:px-8 sm:py-14">
          <span className="mx-auto flex h-16 w-16 items-center justify-center rounded-2xl bg-white text-3xl text-teal-600 shadow-sm dark:bg-neutral-800 dark:text-teal-400">
            <FileImageOutlined />
          </span>
          <Title level={4} className="mb-1! mt-5!">Thêm ảnh món ăn</Title>
          <Text type="secondary">
            Chụp rõ toàn bộ món ăn hoặc chọn một ảnh có sẵn trên thiết bị.
          </Text>
          <div className="mt-6 flex flex-col justify-center gap-3 sm:flex-row">
            <Button
              type="primary"
              size="large"
              icon={<CameraOutlined />}
              disabled={disabled}
              onClick={() => setCameraOpen(true)}
            >
              Chụp ảnh
            </Button>
            <Button
              size="large"
              icon={<UploadOutlined />}
              disabled={disabled}
              onClick={() => uploadInputRef.current?.click()}
            >
              Chọn từ thiết bị
            </Button>
          </div>
          <div className="mt-5 text-xs text-slate-500 dark:text-neutral-400">
            JPEG, PNG hoặc WebP · Tối đa 10 MB
          </div>
        </div>
      )}

      <FoodCameraModal
        open={cameraOpen}
        onCancel={() => setCameraOpen(false)}
        onCapture={(capturedFile) => {
          setCameraOpen(false)
          selectFile(capturedFile)
        }}
      />
    </div>
  )
}
