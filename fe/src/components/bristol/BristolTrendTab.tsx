import { Alert, Card, Col, Empty, Row, Segmented, Spin, Statistic, Typography } from 'antd'
import { useEffect, useRef, useState } from 'react'
import { getBristolTrend } from '../../api/bristol'
import type { BristolLogPoint } from '../../api/bristol'
import { BRISTOL_TYPE_LABELS } from '../../constants/bristol'
import { BristolTrendChart } from './BristolTrendChart'

const { Text, Title } = Typography

interface BristolTrendTabProps {
  days: 7 | 30 | 90
  onDaysChange: (days: 7 | 30 | 90) => void
  onPointSelect: (loggedAt: string) => void
}

export function BristolTrendTab({ days, onDaysChange, onPointSelect }: BristolTrendTabProps) {
  const [points, setPoints] = useState<BristolLogPoint[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const requestId = useRef(0)

  useEffect(() => {
    const currentRequestId = ++requestId.current
    setLoading(true)
    setError(null)
    getBristolTrend(days)
      .then((data) => {
        if (requestId.current === currentRequestId) setPoints(data)
      })
      .catch((err: unknown) => {
        if (requestId.current === currentRequestId) {
          setError(err instanceof Error ? err.message : 'Không thể tải xu hướng Bristol.')
        }
      })
      .finally(() => {
        if (requestId.current === currentRequestId) setLoading(false)
      })
  }, [days])

  const normalCount = points.filter((point) => point.bristolType >= 3 && point.bristolType <= 5).length
  const normalRate = points.length === 0 ? 0 : normalCount / points.length * 100
  const latest = points.at(-1)

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <Title level={4} className="mb-1!">Xu hướng Bristol</Title>
          <Text type="secondary">Chọn một điểm trên biểu đồ để xem các ghi nhận của ngày đó.</Text>
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
        <Row gutter={[24, 24]}>
          <Col xs={24} sm={8}>
            <Card className="h-full rounded-2xl border-black/5 shadow-sm">
              <Statistic title={`Tổng lần trong ${days} ngày`} value={points.length} suffix="lần" />
            </Card>
          </Col>
          <Col xs={24} sm={8}>
            <Card className="h-full rounded-2xl border-black/5 shadow-sm">
              <Statistic title="Trong khoảng loại 3–5" value={normalRate} precision={0} suffix="%" />
              <Text type="secondary">{normalCount}/{points.length} lần ghi nhận</Text>
            </Card>
          </Col>
          <Col xs={24} sm={8}>
            <Card className="h-full rounded-2xl border-black/5 shadow-sm">
              <Statistic title="Ghi nhận gần nhất" value={latest ? `Loại ${latest.bristolType}` : '—'} />
              {latest && <Text type="secondary">{BRISTOL_TYPE_LABELS[latest.bristolType]}</Text>}
            </Card>
          </Col>
        </Row>

        <Card className="mt-6 rounded-2xl border-black/5 shadow-sm" title={`Diễn biến trong ${days} ngày gần nhất`}>
          {!loading && points.length === 0 ? (
            <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="Chưa có dữ liệu trong khoảng thời gian này" />
          ) : (
            <BristolTrendChart points={points} onPointSelect={onPointSelect} />
          )}
        </Card>
      </Spin>
    </div>
  )
}
