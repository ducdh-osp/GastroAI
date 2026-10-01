import { Alert, Card, Spin, Table, Tag, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { useEffect, useState } from 'react'
import { getDigestiveTimeline, getFoodDiaryTrend, listFoodDiary } from '../../api/foodDiary'
import type { DailyCountPoint, DigestiveTimelineItem, FoodDiaryEntry } from '../../api/foodDiary'
import { AppShell } from '../../components/layout/AppShell'
import { FoodDiaryTrendChart } from '../../components/foodDiary/FoodDiaryTrendChart'
import { formatDateTime } from '../../lib/format'
import { BRISTOL_TYPE_LABELS } from '../../constants/bristol'

const { Title, Text } = Typography

const MEAL_TYPE_LABELS = {
  BREAKFAST: 'Bữa sáng', LUNCH: 'Bữa trưa', DINNER: 'Bữa tối', SNACK: 'Bữa phụ', OTHER: 'Khác',
} as const

const columns: ColumnsType<FoodDiaryEntry> = [
  { title: 'Thời điểm ăn', dataIndex: 'eatenAt', key: 'eatenAt', width: 180, render: formatDateTime },
  { title: 'Món ăn', dataIndex: 'description', key: 'description' },
]

const timelineColumns: ColumnsType<DigestiveTimelineItem> = [
  { title: 'Thời điểm', dataIndex: 'occurredAt', key: 'occurredAt', width: 180, render: formatDateTime },
  {
    title: 'Loại', key: 'type', width: 120,
    render: (_, item) => item.type === 'MEAL'
      ? <Tag color="blue">{item.mealType ? MEAL_TYPE_LABELS[item.mealType] : 'Bữa ăn'}</Tag>
      : <Tag color="green">Bristol</Tag>,
  },
  {
    title: 'Nội dung', key: 'content',
    render: (_, item) => item.type === 'MEAL'
      ? <div><div>{item.description}</div>{item.symptomsAfterMeal && <Text type="secondary">Triệu chứng: {item.symptomsAfterMeal}{item.symptomOnsetMinutes !== null ? ` sau ${item.symptomOnsetMinutes} phút` : ''}</Text>}</div>
      : <div>{BRISTOL_TYPE_LABELS[item.bristolType ?? 0] ?? `Loại ${item.bristolType}`}{item.notes && <div><Text type="secondary">{item.notes}</Text></div>}</div>,
  },
]

export default function FoodDiaryHistoryPage() {
  const [points, setPoints] = useState<DailyCountPoint[]>([])
  const [recentEntries, setRecentEntries] = useState<FoodDiaryEntry[]>([])
  const [timeline, setTimeline] = useState<DigestiveTimelineItem[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    setLoading(true)
    setError(null)
    Promise.all([getFoodDiaryTrend(30), listFoodDiary(0, 10), getDigestiveTimeline(30)])
      .then(([trendPoints, recent, timelineItems]) => {
        setPoints(trendPoints)
        setRecentEntries(recent.items)
        setTimeline(timelineItems)
      })
      .catch((err: unknown) => setError(err instanceof Error ? err.message : 'Không thể tải lịch sử ăn uống.'))
      .finally(() => setLoading(false))
  }, [])

  return (
    <AppShell>
      <div className="mx-auto max-w-4xl">
        <div className="mb-6">
          <Text type="secondary">Theo dõi sức khỏe</Text>
          <Title level={2} className="mb-1! mt-1!">Lịch sử ăn uống</Title>
          <Text type="secondary">Số bữa ăn ghi nhận mỗi ngày trong 30 ngày gần nhất.</Text>
        </div>

        {error && <Alert type="error" message={error} showIcon className="mb-4" />}

        <Card className="mb-6 rounded-2xl border-black/5 shadow-sm">
          <Spin spinning={loading}>
            <FoodDiaryTrendChart points={points} />
          </Spin>
        </Card>

        <Card className="mb-6 rounded-2xl border-black/5 shadow-sm" title="Timeline ăn uống và Bristol (30 ngày)">
          <Spin spinning={loading}>
            <Table<DigestiveTimelineItem> columns={timelineColumns} dataSource={timeline} rowKey={(item) => `${item.type}-${item.id}`} size="small" pagination={{ pageSize: 10 }} locale={{ emptyText: 'Chưa có dữ liệu để đối chiếu' }} />
          </Spin>
        </Card>

        <Card className="rounded-2xl border-black/5 shadow-sm" title="Các mục gần đây">
          <Spin spinning={loading}>
            <Table<FoodDiaryEntry>
              columns={columns}
              dataSource={recentEntries}
              rowKey="id"
              size="small"
              pagination={false}
              locale={{ emptyText: 'Chưa có mục nào trong nhật ký ăn uống' }}
            />
          </Spin>
        </Card>
      </div>
    </AppShell>
  )
}
