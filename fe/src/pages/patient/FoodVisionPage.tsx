import { ScanOutlined, StopOutlined } from '@ant-design/icons'
import { Alert, Button, Card, Spin, Typography } from 'antd'
import { useEffect, useRef, useState } from 'react'
import { analyzeFoodImage, type FoodVisionAnalysis } from '../../api/foodVision'
import { FoodImageUploader } from '../../components/foodVision/FoodImageUploader'
import { FoodVisionResult } from '../../components/foodVision/FoodVisionResult'
import { AppShell } from '../../components/layout/AppShell'

const { Paragraph, Text, Title } = Typography

export default function FoodVisionPage() {
  const [image, setImage] = useState<File | null>(null)
  const [analysis, setAnalysis] = useState<FoodVisionAnalysis | null>(null)
  const [analyzing, setAnalyzing] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const requestControllerRef = useRef<AbortController | null>(null)

  useEffect(() => () => requestControllerRef.current?.abort(), [])

  function handleImageChange(nextImage: File | null) {
    requestControllerRef.current?.abort()
    requestControllerRef.current = null
    setAnalyzing(false)
    setImage(nextImage)
    setAnalysis(null)
    setError(null)
  }

  async function handleAnalyze() {
    if (!image || analyzing) return

    const controller = new AbortController()
    requestControllerRef.current?.abort()
    requestControllerRef.current = controller
    setAnalyzing(true)
    setAnalysis(null)
    setError(null)

    try {
      const result = await analyzeFoodImage(image, controller.signal)
      if (!controller.signal.aborted) setAnalysis(result)
    } catch (caughtError) {
      if (!controller.signal.aborted) {
        setError(caughtError instanceof Error
          ? caughtError.message
          : 'Không thể phân tích ảnh món ăn. Vui lòng thử lại.')
      }
    } finally {
      if (!controller.signal.aborted) {
        setAnalyzing(false)
        requestControllerRef.current = null
      }
    }
  }

  function cancelAnalysis() {
    requestControllerRef.current?.abort()
    requestControllerRef.current = null
    setAnalyzing(false)
  }

  return (
    <AppShell>
      <div className="mx-auto max-w-5xl">
        <div className="mb-6">
          <Text type="secondary">Công cụ dinh dưỡng</Text>
          <Title level={2} className="mb-1! mt-1!">Nhận diện món ăn qua ảnh</Title>
          <Text type="secondary">
            Chụp hoặc tải ảnh món ăn để nhận diện thành phần và tham khảo mức FODMAP ước tính.
          </Text>
        </div>

        <Alert
          type="info"
          showIcon
          className="mb-5"
          message="Mẹo để có kết quả tốt hơn"
          description="Chụp đủ sáng, giữ món ăn ở giữa khung hình và hạn chế để nhiều món không liên quan trong cùng một ảnh."
        />

        <Card className="rounded-2xl border-black/5 shadow-sm dark:border-white/10">
          <FoodImageUploader file={image} disabled={analyzing} onChange={handleImageChange} />

          {image && (
            <div className="mt-5 flex flex-col gap-3 sm:flex-row sm:justify-end">
              {analyzing && (
                <Button icon={<StopOutlined />} onClick={cancelAnalysis}>
                  Hủy phân tích
                </Button>
              )}
              <Button
                type="primary"
                size="large"
                icon={<ScanOutlined />}
                loading={analyzing}
                disabled={analyzing}
                onClick={() => void handleAnalyze()}
              >
                {analyzing ? 'Đang phân tích...' : analysis ? 'Phân tích lại' : 'Phân tích món ăn'}
              </Button>
            </div>
          )}

          {analyzing && (
            <div className="mt-5 rounded-xl bg-teal-50 px-4 py-6 text-center dark:bg-teal-950/20" aria-live="polite">
              <Spin />
              <Paragraph className="mb-0! mt-3! font-medium">
                AI đang nhận diện món ăn và đối chiếu dữ liệu FODMAP…
              </Paragraph>
              <Text type="secondary" className="text-xs">Quá trình này có thể mất một chút thời gian.</Text>
            </div>
          )}
        </Card>

        {error && (
          <Alert
            type="error"
            showIcon
            closable
            className="mt-5"
            message="Không thể phân tích ảnh"
            description={error}
            onClose={() => setError(null)}
            action={image && !analyzing
              ? <Button size="small" danger onClick={() => void handleAnalyze()}>Thử lại</Button>
              : undefined}
          />
        )}

        {analysis && (
          <div className="mt-5">
            <FoodVisionResult analysis={analysis} />
          </div>
        )}
      </div>
    </AppShell>
  )
}
