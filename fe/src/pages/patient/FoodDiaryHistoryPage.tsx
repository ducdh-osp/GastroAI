import { Alert, Card, Spin, Table, Tabs, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { useCallback, useEffect, useState } from 'react'
import { getFoodDiaryTrend, listFoodDiary } from '../../api/foodDiary'
import type { DailyCountPoint, FoodDiaryEntry, MealType } from '../../api/foodDiary'
import { AppShell } from '../../components/layout/AppShell'
import { FoodDiaryTrendChart } from '../../components/foodDiary/FoodDiaryTrendChart'
import { usePagedList } from '../../hooks/usePagedList'
import { formatDateTime } from '../../lib/format'

const { Title, Text } = Typography
const PAGE_SIZE = 10

const MEAL_TYPE_LABELS: Record<MealType, string> = {
  BREAKFAST: 'Bữa sáng',
  LUNCH: 'Bữa trưa',
  DINNER: 'Bữa tối',
  SNACK: 'Bữa phụ',
  OTHER: 'Khác',
}

const columns: ColumnsType<FoodDiaryEntry> = [
  { title: 'Thời điểm ăn', dataIndex: 'eatenAt', key: 'eatenAt', width: 170, render: formatDateTime },
  { title: 'Loại bữa', dataIndex: 'mealType', key: 'mealType', width: 110, render: (value: MealType) => MEAL_TYPE_LABELS[value] },
  { title: 'Món ăn', dataIndex: 'description', key: 'description' },
  {
    title: 'Triệu chứng sau ăn', dataIndex: 'symptomsAfterMeal', key: 'symptomsAfterMeal',
    render: (value: string | null) => value || <Text type="secondary">—</Text>,
  },
]

export default function FoodDiaryHistoryPage() {
  const [points, setPoints] = useState<DailyCountPoint[]>([])
  const [trendLoading, setTrendLoading] = useState(true)
  const [trendError, setTrendError] = useState<string | null>(null)
  const fetchHistory = useCallback((page: number, size: number) => listFoodDiary(page, size), [])
  const history = usePagedList({
    fetchPage: fetchHistory,
    pageSize: PAGE_SIZE,
    loadErrorMessage: 'Không thể tải lịch sử ăn uống.',
  })

  useEffect(() => {
    let cancelled = false
    getFoodDiaryTrend(30)
      .then((trendPoints) => { if (!cancelled) setPoints(trendPoints) })
      .catch((error: unknown) => {
        if (!cancelled) setTrendError(error instanceof Error ? error.message : 'Không thể tải biểu đồ ăn uống.')
      })
      .finally(() => { if (!cancelled) setTrendLoading(false) })
    return () => { cancelled = true }
  }, [])

  return (
    <AppShell>
      <div className="mx-auto max-w-5xl">
        <div className="mb-6">
          <Text type="secondary">Theo dõi sức khỏe</Text>
          <Title level={2} className="mb-1! mt-1!">Lịch sử ăn uống</Title>
          <Text type="secondary">Xem riêng biểu đồ tổng quan và danh sách đầy đủ các bữa ăn đã ghi nhận.</Text>
        </div>

        <Tabs
          defaultActiveKey="chart"
          type="card"
          items={[
            {
              key: 'chart',
              label: 'Biểu đồ',
              children: (
                <>
                  {trendError && <Alert type="error" message={trendError} showIcon className="mb-4" />}
                  <Card className="rounded-2xl border-black/5 shadow-sm" title="Số bữa ăn trong 30 ngày gần nhất">
                    <Spin spinning={trendLoading}>
                      {points.length === 0 && !trendLoading ? (
                        <Text type="secondary">Chưa có dữ liệu để hiển thị biểu đồ.</Text>
                      ) : (
                        <FoodDiaryTrendChart points={points} />
                      )}
                    </Spin>
                  </Card>
                </>
              ),
            },
            {
              key: 'history',
              label: 'Lịch sử',
              children: (
                <>
                  {history.error && <Alert type="error" message={history.error} showIcon className="mb-4" />}
                  <Card className="rounded-2xl border-black/5 shadow-sm" title="Các bữa ăn đã ghi nhận">
                    <Table<FoodDiaryEntry>
                      columns={columns}
                      dataSource={history.items}
                      rowKey="id"
                      size="small"
                      loading={history.loading}
                      scroll={{ x: 760 }}
                      pagination={{
                        current: history.page + 1,
                        pageSize: PAGE_SIZE,
                        total: history.totalElements,
                        showSizeChanger: false,
                        onChange: (nextPage) => history.setPage(nextPage - 1),
                      }}
                      locale={{ emptyText: 'Chưa có mục nào trong nhật ký ăn uống' }}
                    />
                  </Card>
                </>
              ),
            },
          ]}
        />
      </div>
    </AppShell>
  )
}
