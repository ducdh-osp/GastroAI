import { CameraOutlined, ReloadOutlined } from '@ant-design/icons'
import { Alert, Button, Modal, Spin, Typography } from 'antd'
import { useEffect, useRef, useState } from 'react'

const { Text } = Typography
const MAX_CAPTURE_DIMENSION = 1920

interface FoodCameraModalProps {
  open: boolean
  onCancel: () => void
  onCapture: (file: File) => void
}

function cameraErrorMessage(error: unknown) {
  const name = (error as { name?: string })?.name
  if (name === 'NotAllowedError' || name === 'SecurityError') {
    return 'Bạn chưa cho phép truy cập camera. Hãy cấp quyền camera trong trình duyệt rồi thử lại.'
  }
  if (name === 'NotFoundError' || name === 'DevicesNotFoundError') {
    return 'Không tìm thấy camera trên thiết bị này.'
  }
  if (name === 'NotReadableError' || name === 'TrackStartError') {
    return 'Không thể mở camera. Camera có thể đang được ứng dụng khác sử dụng.'
  }
  if (name === 'OverconstrainedError' || name === 'ConstraintNotSatisfiedError') {
    return 'Camera trên thiết bị không hỗ trợ cấu hình được yêu cầu.'
  }
  return 'Không thể mở camera. Vui lòng kiểm tra quyền truy cập và thử lại.'
}

function captureDimensions(videoWidth: number, videoHeight: number) {
  const largestDimension = Math.max(videoWidth, videoHeight)
  if (largestDimension <= MAX_CAPTURE_DIMENSION) {
    return { width: videoWidth, height: videoHeight }
  }
  const scale = MAX_CAPTURE_DIMENSION / largestDimension
  return {
    width: Math.round(videoWidth * scale),
    height: Math.round(videoHeight * scale),
  }
}

export function FoodCameraModal({ open, onCancel, onCapture }: FoodCameraModalProps) {
  const videoRef = useRef<HTMLVideoElement>(null)
  const streamRef = useRef<MediaStream | null>(null)
  const cameraRequestIdRef = useRef(0)
  const [starting, setStarting] = useState(false)
  const [ready, setReady] = useState(false)
  const [capturing, setCapturing] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => () => {
    cameraRequestIdRef.current += 1
    streamRef.current?.getTracks().forEach((track) => track.stop())
    streamRef.current = null
  }, [])

  function stopCamera() {
    cameraRequestIdRef.current += 1
    streamRef.current?.getTracks().forEach((track) => track.stop())
    streamRef.current = null
    if (videoRef.current) videoRef.current.srcObject = null
    setStarting(false)
    setReady(false)
    setCapturing(false)
  }

  async function startCamera() {
    const requestId = cameraRequestIdRef.current + 1
    cameraRequestIdRef.current = requestId
    streamRef.current?.getTracks().forEach((track) => track.stop())
    streamRef.current = null
    setError(null)
    setReady(false)
    setStarting(true)

    if (!navigator.mediaDevices?.getUserMedia) {
      setStarting(false)
      setError('Trình duyệt không hỗ trợ camera hoặc trang chưa chạy qua HTTPS/localhost.')
      return
    }

    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        audio: false,
        video: {
          facingMode: { ideal: 'environment' },
          width: { ideal: 1920 },
          height: { ideal: 1080 },
        },
      })

      if (cameraRequestIdRef.current !== requestId) {
        stream.getTracks().forEach((track) => track.stop())
        return
      }

      streamRef.current = stream
      if (videoRef.current) {
        videoRef.current.srcObject = stream
        await videoRef.current.play()
      }
    } catch (caughtError) {
      if (cameraRequestIdRef.current === requestId) {
        setError(cameraErrorMessage(caughtError))
      }
    } finally {
      if (cameraRequestIdRef.current === requestId) setStarting(false)
    }
  }

  function handleCancel() {
    stopCamera()
    setError(null)
    onCancel()
  }

  async function handleCapture() {
    const video = videoRef.current
    if (!video || !ready || video.videoWidth === 0 || video.videoHeight === 0) return

    setCapturing(true)
    setError(null)
    try {
      const { width, height } = captureDimensions(video.videoWidth, video.videoHeight)
      const canvas = document.createElement('canvas')
      canvas.width = width
      canvas.height = height
      const context = canvas.getContext('2d')
      if (!context) throw new Error('Canvas is unavailable')
      context.drawImage(video, 0, 0, width, height)

      const blob = await new Promise<Blob>((resolve, reject) => {
        canvas.toBlob(
          (result) => result ? resolve(result) : reject(new Error('Could not encode image')),
          'image/jpeg',
          0.9,
        )
      })
      const timestamp = new Date().toISOString().replace(/[:.]/g, '-')
      const file = new File([blob], `anh-mon-an-${timestamp}.jpg`, {
        type: 'image/jpeg',
        lastModified: Date.now(),
      })

      stopCamera()
      onCapture(file)
    } catch {
      setCapturing(false)
      setError('Không thể lưu ảnh vừa chụp. Vui lòng thử lại.')
    }
  }

  return (
    <Modal
      title="Chụp ảnh món ăn"
      open={open}
      width={760}
      centered
      destroyOnHidden
      maskClosable={false}
      onCancel={handleCancel}
      afterOpenChange={(isOpen) => {
        if (isOpen) void startCamera()
      }}
      footer={(
        <div className="flex items-center justify-end gap-2">
          <Button onClick={handleCancel}>Hủy</Button>
          <Button
            type="primary"
            size="large"
            icon={<CameraOutlined />}
            loading={capturing}
            disabled={!ready || starting || Boolean(error)}
            onClick={() => void handleCapture()}
          >
            Chụp ảnh
          </Button>
        </div>
      )}
    >
      {error && (
        <Alert
          type="error"
          showIcon
          className="mb-4"
          message={error}
          action={(
            <Button size="small" icon={<ReloadOutlined />} onClick={() => void startCamera()}>
              Thử lại
            </Button>
          )}
        />
      )}

      <div className="relative flex min-h-72 items-center justify-center overflow-hidden rounded-2xl bg-black sm:min-h-96">
        <video
          ref={videoRef}
          autoPlay
          muted
          playsInline
          aria-label="Hình ảnh trực tiếp từ camera"
          className="max-h-[65vh] w-full object-contain"
          onCanPlay={() => setReady(true)}
        />
        {starting && (
          <div className="absolute inset-0 flex flex-col items-center justify-center gap-3 bg-black/70 text-white" aria-live="polite">
            <Spin size="large" />
            <span>Đang mở camera…</span>
          </div>
        )}
        {!starting && !ready && !error && (
          <Text className="absolute text-white/70!">Đang chờ tín hiệu camera…</Text>
        )}
      </div>
      <Text type="secondary" className="mt-3 block text-center text-xs">
        Đặt món ăn ở giữa khung hình và chụp trong điều kiện đủ sáng.
      </Text>
    </Modal>
  )
}
