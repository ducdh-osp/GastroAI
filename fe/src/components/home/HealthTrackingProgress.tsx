import {
  CalendarOutlined,
  CheckCircleOutlined,
  CoffeeOutlined,
  ExperimentOutlined,
  FireOutlined,
  TrophyOutlined,
} from '@ant-design/icons'
import { Alert, Button, Card, Progress, Select, Spin, Tooltip, Typography } from 'antd'
import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { getAdherence } from '../../api/adherence'
import type { AdherenceResponse } from '../../api/adherence'

const { Text, Title } = Typography

const DAYS_OPTIONS = [
  { value: 7, label: '7 ngày qua' },
  { value: 30, label: '30 ngày qua' },
  { value: 90, label: '90 ngày qua' },
]

function formatDay(iso: string): string {
  const [year, month, day] = iso.split('-')
  return `${day}/${month}/${year}`
}

export function HealthTrackingProgress() {
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
        if (currentRequestId === requestId.current) setData(result)
      })
      .catch((err: unknown) => {
        if (currentRequestId === requestId.current) {
          setError(err instanceof Error ? err.message : 'Không thể tải tiến độ theo dõi sức khỏe.')
        }
      })
      .finally(() => {
        if (currentRequestId === requestId.current) setLoading(false)
      })
  }, [days])

  return (
    <section aria-labelledby="tracking-progress-title">
      <div className="mb-4 flex flex-wrap items-end justify-between gap-3">
        <div>
          <Text type="secondary">Thói quen hằng ngày</Text>
          <Title id="tracking-progress-title" level={3} className="mb-1! mt-1!">
            Tiến độ theo dõi sức khỏe
          </Title>
          <Text type="secondary">
            Mỗi ngày có ít nhất một ghi nhận ăn uống hoặc Bristol sẽ được tính là một ngày duy trì.
          </Text>
        </div>
        <Select
          aria-label="Khoảng thời gian theo dõi"
          value={days}
          onChange={setDays}
          options={DAYS_OPTIONS}
          style={{ width: 150 }}
        />
      </div>

      {error && <Alert type="error" message={error} showIcon className="mb-4" />}

      <Spin spinning={loading}>
        {data && (
          <div className="space-y-4">
            {!data.loggedToday && (
              <Card className="overflow-hidden rounded-2xl border-teal-200/70 bg-gradient-to-r from-teal-50 to-emerald-50 shadow-sm dark:border-teal-500/20 dark:from-teal-950/30 dark:to-emerald-950/20">
                <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-center">
                  <div>
                    <Text strong className="block text-base">Hôm nay bạn chưa ghi nhật ký</Text>
                    <Text type="secondary">
                      {data.currentStreak > 0
                        ? `Thêm một ghi nhận để tiếp tục chuỗi ${data.currentStreak} ngày đang duy trì.`
                        : 'Thêm một ghi nhận để bắt đầu thói quen theo dõi từ hôm nay.'}
                    </Text>
                  </div>
                  <div className="flex flex-wrap gap-2">
                    <Link to="/food-diary">
                      <Button type="primary" icon={<CoffeeOutlined />}>Ghi bữa ăn</Button>
                    </Link>
                    <Link to="/bristol">
                      <Button icon={<ExperimentOutlined />}>Ghi Bristol</Button>
                    </Link>
                  </div>
                </div>
              </Card>
            )}

            <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
              <Card className="rounded-2xl border-black/5 shadow-sm">
                <div className="mb-3 flex h-10 w-10 items-center justify-center rounded-xl bg-orange-100 text-xl text-orange-600 dark:bg-orange-500/15 dark:text-orange-300">
                  <FireOutlined />
                </div>
                <Text type="secondary">Chuỗi ngày duy trì</Text>
                <div className="mt-1 text-3xl font-semibold">{data.currentStreak} ngày</div>
                <Text type="secondary" className="text-xs">
                  {data.loggedToday ? 'Hôm nay đã có ghi nhận.' : 'Hãy ghi một mục trong hôm nay.'}
                </Text>
              </Card>

              <Card className="rounded-2xl border-black/5 shadow-sm">
                <div className="mb-3 flex h-10 w-10 items-center justify-center rounded-xl bg-amber-100 text-xl text-amber-600 dark:bg-amber-500/15 dark:text-amber-300">
                  <TrophyOutlined />
                </div>
                <Text type="secondary">Kỷ lục duy trì</Text>
                <div className="mt-1 text-3xl font-semibold">{data.longestStreak} ngày</div>
                <Text type="secondary" className="text-xs">Tính trong 365 ngày gần nhất.</Text>
              </Card>

              <Card className="rounded-2xl border-black/5 shadow-sm">
                <div className="mb-3 flex h-10 w-10 items-center justify-center rounded-xl bg-teal-100 text-xl text-teal-700 dark:bg-teal-500/15 dark:text-teal-300">
                  <CheckCircleOutlined />
                </div>
                <Text type="secondary">Mức độ ghi nhật ký</Text>
                <div className="mt-1 text-3xl font-semibold">{data.adherencePercent.toFixed(1)}%</div>
                <Text type="secondary" className="text-xs">
                  {data.loggedDays}/{data.totalDays} ngày có ghi nhận.
                </Text>
              </Card>
            </div>

            <Card className="rounded-2xl border-black/5 shadow-sm">
              <div className="mb-4 flex flex-wrap items-center justify-between gap-2">
                <div>
                  <Text strong className="block text-base"><CalendarOutlined /> Lịch ghi nhật ký</Text>
                  <Text type="secondary" className="text-xs">{data.totalDays} ngày gần nhất</Text>
                </div>
                <Text strong>{data.loggedDays}/{data.totalDays} ngày</Text>
              </div>

              <Progress
                percent={Math.round(data.adherencePercent)}
                strokeColor={{ from: '#0d9488', to: '#22c55e' }}
                trailColor="rgba(148, 163, 184, 0.2)"
              />

              <div className="mt-5 flex flex-wrap gap-1.5">
                {data.days.map((point) => (
                  <Tooltip
                    key={point.date}
                    title={`${formatDay(point.date)}: ${point.foodDiaryCount} bữa ăn, ${point.bristolCount} lần Bristol`}
                  >
                    <span
                      aria-label={`${formatDay(point.date)} ${point.logged ? 'đã ghi' : 'chưa ghi'}`}
                      className={`inline-block h-5 w-5 rounded-md transition-transform hover:scale-125 ${
                        point.logged
                          ? 'bg-teal-500 shadow-[0_2px_6px_rgba(13,148,136,0.25)]'
                          : 'bg-slate-200 dark:bg-slate-700'
                      }`}
                    />
                  </Tooltip>
                ))}
              </div>
              <div className="mt-4 flex flex-wrap gap-4 text-xs text-slate-500 dark:text-slate-400">
                <span><span className="mr-1.5 inline-block h-3 w-3 rounded align-middle bg-teal-500" />Đã ghi</span>
                <span><span className="mr-1.5 inline-block h-3 w-3 rounded align-middle bg-slate-200 dark:bg-slate-700" />Chưa ghi</span>
              </div>
            </Card>
          </div>
        )}
      </Spin>
    </section>
  )
}
