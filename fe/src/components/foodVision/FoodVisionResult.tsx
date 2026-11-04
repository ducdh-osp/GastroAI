import {
  CheckCircleOutlined,
  ExclamationCircleOutlined,
  InfoCircleOutlined,
} from '@ant-design/icons'
import { Alert, Card, List, Progress, Tag, Typography } from 'antd'
import type { FodmapLevel, FoodVisionAnalysis, FoodVisionIngredient } from '../../api/foodVision'

const { Paragraph, Text, Title } = Typography

const LEVEL_CONFIG: Record<FodmapLevel, { color: string; label: string }> = {
  LOW: { color: 'success', label: 'FODMAP thấp' },
  MODERATE: { color: 'warning', label: 'FODMAP trung bình' },
  HIGH: { color: 'error', label: 'FODMAP cao' },
  UNKNOWN: { color: 'default', label: 'Chưa có dữ liệu' },
}

interface FoodVisionResultProps {
  analysis: FoodVisionAnalysis
}

function confidencePercent(confidence: number | null) {
  if (confidence === null || !Number.isFinite(confidence)) return null
  return Math.round(Math.min(1, Math.max(0, confidence)) * 100)
}

function FodmapTag({ level }: { level: FodmapLevel }) {
  const config = LEVEL_CONFIG[level] ?? LEVEL_CONFIG.UNKNOWN
  return <Tag color={config.color}>{config.label}</Tag>
}

function ingredientTitle(ingredient: FoodVisionIngredient) {
  return ingredient.normalizedName?.trim() || ingredient.detectedName
}

export function FoodVisionResult({ analysis }: FoodVisionResultProps) {
  const dishConfidence = confidencePercent(analysis.confidence)
  const ingredients = analysis.ingredients ?? []
  const warnings = analysis.warnings ?? []

  return (
    <div className="space-y-4" aria-live="polite">
      <Card className="rounded-2xl border-black/5 shadow-sm dark:border-white/10">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
          <div className="min-w-0">
            <Text type="secondary">Món ăn được nhận diện</Text>
            <Title level={3} className="mb-2! mt-1!">{analysis.dishName || 'Chưa xác định được tên món'}</Title>
            <FodmapTag level={analysis.overallFodmapLevel} />
          </div>
          {dishConfidence !== null && (
            <div className="w-full rounded-xl bg-slate-50 px-4 py-3 dark:bg-neutral-900 sm:w-48">
              <div className="mb-1 flex items-center justify-between text-xs">
                <Text type="secondary">Độ tin cậy</Text>
                <Text strong>{dishConfidence}%</Text>
              </div>
              <Progress percent={dishConfidence} size="small" showInfo={false} strokeColor="#0d9488" />
            </div>
          )}
        </div>
      </Card>

      <Card
        title="Thành phần nhận diện"
        className="rounded-2xl border-black/5 shadow-sm dark:border-white/10"
      >
        {ingredients.length > 0 ? (
          <List<FoodVisionIngredient>
            dataSource={ingredients}
            renderItem={(ingredient) => {
              const confidence = confidencePercent(ingredient.confidence)
              const normalizedTitle = ingredientTitle(ingredient)
              const showDetectedName = ingredient.normalizedName
                && ingredient.normalizedName.trim().toLocaleLowerCase('vi')
                  !== ingredient.detectedName.trim().toLocaleLowerCase('vi')

              return (
                <List.Item
                  extra={<FodmapTag level={ingredient.fodmapLevel} />}
                  className="items-start!"
                >
                  <List.Item.Meta
                    avatar={ingredient.fodmapLevel === 'UNKNOWN'
                      ? <ExclamationCircleOutlined className="mt-1 text-lg text-amber-500" />
                      : <CheckCircleOutlined className="mt-1 text-lg text-teal-600" />}
                    title={<Text strong>{normalizedTitle}</Text>}
                    description={(
                      <div className="space-y-1">
                        {showDetectedName && <div>AI nhận diện: {ingredient.detectedName}</div>}
                        {ingredient.servingSize && <div>Khẩu phần ước tính: {ingredient.servingSize}</div>}
                        {confidence !== null && <div>Độ tin cậy: {confidence}%</div>}
                        {ingredient.fodmapLevel === 'UNKNOWN' && (
                          <div>Không tìm thấy dữ liệu FODMAP phù hợp để đối chiếu.</div>
                        )}
                      </div>
                    )}
                  />
                </List.Item>
              )
            }}
          />
        ) : (
          <Alert
            type="warning"
            showIcon
            message="Chưa nhận diện được thành phần cụ thể trong ảnh."
          />
        )}
      </Card>

      {warnings.map((warning, index) => (
        <Alert key={`${warning}-${index}`} type="warning" showIcon message={warning} />
      ))}

      <Alert
        type="info"
        showIcon
        icon={<InfoCircleOutlined />}
        message="Kết quả chỉ mang tính tham khảo"
        description={(
          <Paragraph className="mb-0!">
            AI có thể nhận diện thiếu hoặc nhầm thành phần. Mức FODMAP còn phụ thuộc vào khẩu phần,
            cách chế biến và khả năng dung nạp của từng người.
          </Paragraph>
        )}
      />
    </div>
  )
}
