import { FireOutlined, TrophyOutlined } from '@ant-design/icons'
import { Alert, Card, Progress, Select, Spin, Tooltip, Typography } from 'antd'
import { useEffect, useRef, useState } from 'react'
import { getAdherence } from '../../api/adherence'
import type { AdherenceResponse } from '../../api/adherence'
import { AppShell } from '../../components/layout/AppShell'

const { Title, Text } = Typography

const DAYS_OPTIONS = [
  { value: 7, label: '7 ngày qua' },
  { value: 30, label: '30 ngày qua' },
  { value: 90, label: '90 ngày qua' },
]

function formatDay(iso: string): string {
  const [year, month, day] = iso.split('-')
  return `${day}/${month}/${year}`
}

export default function AdherencePage() {
  const [days, setDays] = useState(30)
  const [data, setData] = useState<AdherenceResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const requestId = useRef(0)

  useEffect(() => {
    const currentRequestId = ++requestId.current
    setLoading(true)
    setError(null)
    getAdherence(days)
      .then((result) => {
        if (currentRequestId !== requestId.current) return
        setData(result)
      })
      .catch((err: unknown) => {
        if (currentRequestId !== requestId.current) return
        setError(err instanceof Error ? err.message : 'Không thể tải streak và tỷ lệ tuân thủ.')
      })
      .finally(() => {
        if (currentRequestId === requestId.current) setLoading(false)
      })
  }, [days])

  return (
    <AppShell>
      <div className="mx-auto max-w-4xl">
        <div className="mb-6 flex flex-wrap items-end justify-between gap-3">
          <div>
            <Text type="secondary">Theo dõi sức khỏe</Text>
            <Title level={2} className="mb-1! mt-1!">Streak &amp; tỷ lệ tuân thủ</Title>
            <Text type="secondary">
              Một ngày được tính là đã ghi khi bạn có ít nhất một mục nhật ký ăn uống hoặc nhật ký Bristol.
            </Text>
          </div>
          <Select value={days} onChange={setDays} options={DAYS_OPTIONS} style={{ width: 160 }} />
        </div>

        {error && <Alert type="error" message={error} showIcon className="mb-4" />}

        <Spin spinning={loading}>
          {data && (
            <>
              <div className="mb-4 grid grid-cols-1 gap-4 sm:grid-cols-3">
                <Card className="rounded-2xl border-black/5 shadow-sm">
                  <Text type="secondary"><FireOutlined /> Streak hiện tại</Text>
                  <div className="mt-1 text-3xl font-semibold">{data.currentStreak} ngày</div>
                  {!data.loggedToday && data.currentStreak > 0 && (
                    <Text type="secondary" className="text-xs">
                      Hôm nay bạn chưa ghi - ghi một mục để giữ streak.
                    </Text>
                  )}
                </Card>
                <Card className="rounded-2xl border-black/5 shadow-sm">
                  <Text type="secondary"><TrophyOutlined /> Streak dài nhất</Text>
                  <div className="mt-1 text-3xl font-semibold">{data.longestStreak} ngày</div>
                  <Text type="secondary" className="text-xs">Trong 365 ngày gần nhất.</Text>
                </Card>
                <Card className="rounded-2xl border-black/5 shadow-sm">
                  <Text type="secondary">Tỷ lệ tuân thủ</Text>
                  <div className="mt-1 text-3xl font-semibold">{data.adherencePercent.toFixed(1)}%</div>
                  <Text type="secondary" className="text-xs">
                    {data.loggedDays}/{data.totalDays} ngày có ghi nhật ký.
                  </Text>
                </Card>
              </div>

              <Card className="mb-4 rounded-2xl border-black/5 shadow-sm">
                <Progress percent={Math.round(data.adherencePercent)} status="active" />
              </Card>

              <Card className="rounded-2xl border-black/5 shadow-sm" title={`Chi tiết ${data.totalDays} ngày gần nhất`}>
                <div className="flex flex-wrap gap-1.5">
                  {data.days.map((point) => (
                    <Tooltip
                      key={point.date}
                      title={`${formatDay(point.date)}: ${point.foodDiaryCount} bữa ăn, ${point.bristolCount} lần Bristol`}
                    >
                      <span
                        aria-label={`${formatDay(point.date)} ${point.logged ? 'đã ghi' : 'chưa ghi'}`}
                        className="inline-block h-5 w-5 rounded"
                        style={{ background: point.logged ? '#13a89e' : '#e5e7eb' }}
                      />
                    </Tooltip>
                  ))}
                </div>
                <div className="mt-3 flex gap-4 text-xs text-gray-500 dark:text-white">
                  <span><span className="mr-1 inline-block h-3 w-3 rounded align-middle" style={{ background: '#13a89e' }} />Đã ghi</span>
                  <span><span className="mr-1 inline-block h-3 w-3 rounded align-middle" style={{ background: '#e5e7eb' }} />Chưa ghi</span>
                </div>
              </Card>
            </>
          )}
        </Spin>
      </div>
    </AppShell>
  )
}
