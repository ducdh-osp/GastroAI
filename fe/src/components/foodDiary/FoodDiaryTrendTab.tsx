import { Alert, Card, Col, Empty, Row, Segmented, Spin, Statistic, Typography } from 'antd'
import { useEffect, useRef, useState } from 'react'
import { getFoodDiaryTrend } from '../../api/foodDiary'
import type { DailyCountPoint } from '../../api/foodDiary'
import { FoodDiaryTrendChart } from './FoodDiaryTrendChart'

const { Text, Title } = Typography

const METRIC_CARD_CLASS =
  'h-full overflow-hidden rounded-2xl border border-slate-200/80 bg-white shadow-[0_8px_24px_rgba(15,23,42,0.06)] transition-all duration-200 hover:-translate-y-0.5 hover:shadow-[0_14px_32px_rgba(15,23,42,0.1)] dark:border-white/10 dark:bg-slate-900/80'
const CONTENT_CARD_CLASS =
  'overflow-hidden rounded-2xl border border-slate-200/80 bg-white shadow-[0_10px_30px_rgba(15,23,42,0.07)] [&_.ant-card-head]:border-slate-100 [&_.ant-card-head]:bg-slate-50/80 [&_.ant-card-head-title]:py-4 dark:border-white/10 dark:bg-slate-900/80 dark:[&_.ant-card-head]:border-white/10 dark:[&_.ant-card-head]:bg-white/[0.03]'

interface FoodDiaryTrendTabProps {
  days: 7 | 30 | 90
  onDaysChange: (days: 7 | 30 | 90) => void
  onDaySelect: (date: string) => void
  title?: string
  titleLevel?: 2 | 3 | 4 | 5
  description?: string
}

export function FoodDiaryTrendTab({
  days,
  onDaysChange,
  onDaySelect,
  title = 'Xu hướng ghi nhận bữa ăn',
  titleLevel = 4,
  description = 'Chọn một cột trên biểu đồ để xem các bữa của ngày đó.',
}: FoodDiaryTrendTabProps) {
  const [points, setPoints] = useState<DailyCountPoint[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const requestId = useRef(0)

  useEffect(() => {
    const currentRequestId = ++requestId.current
    setLoading(true)
    setError(null)
    getFoodDiaryTrend(days)
      .then((data) => {
        if (requestId.current === currentRequestId) setPoints(data)
      })
      .catch((err: unknown) => {
        if (requestId.current === currentRequestId) {
          setError(err instanceof Error ? err.message : 'Không thể tải xu hướng ăn uống.')
        }
      })
      .finally(() => {
        if (requestId.current === currentRequestId) setLoading(false)
      })
  }, [days])

  const totalMeals = points.reduce((sum, point) => sum + point.count, 0)
  const recordedDays = points.filter((point) => point.count > 0).length
  const averagePerRecordedDay = recordedDays === 0 ? 0 : totalMeals / recordedDays

  return (
    <div className="grid gap-6 pb-2">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <Title level={titleLevel} className="mb-1! mt-1!">{title}</Title>
          <Text type="secondary">{description}</Text>
        </div>
        <Segmented
          value={days}
          options={[
            { value: 7, label: '7 ngày' },
            { value: 30, label: '30 ngày' },
            { value: 90, label: '90 ngày' },
          ]}
          onChange={(value) => onDaysChange(value as 7 | 30 | 90)}
        />
      </div>

      {error && <Alert type="error" message={error} showIcon />}

      <Spin spinning={loading}>
        <div className="grid gap-6">
          <Row gutter={[24, 24]}>
            <Col xs={24} sm={8}>
              <Card className={METRIC_CARD_CLASS}>
                <Statistic title={`Tổng bữa trong ${days} ngày`} value={totalMeals} suffix="bữa" />
              </Card>
            </Col>
            <Col xs={24} sm={8}>
              <Card className={METRIC_CARD_CLASS}>
                <Statistic title="Số ngày có ghi nhật ký" value={recordedDays} suffix={`/ ${days}`} />
              </Card>
            </Col>
            <Col xs={24} sm={8}>
              <Card className={METRIC_CARD_CLASS}>
                <Statistic
                  title="Trung bình mỗi ngày có ghi"
                  value={averagePerRecordedDay}
                  precision={1}
                  suffix="bữa"
                />
              </Card>
            </Col>
          </Row>

          <Card
            className={CONTENT_CARD_CLASS}
            title={`Số bữa ăn trong ${days} ngày gần nhất`}
          >
            {!loading && totalMeals === 0 ? (
              <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="Chưa có dữ liệu trong khoảng thời gian này" />
            ) : (
              <FoodDiaryTrendChart points={points} onDaySelect={onDaySelect} />
            )}
          </Card>
        </div>
      </Spin>
    </div>
  )
}
