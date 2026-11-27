import { HeartOutlined, ReloadOutlined } from '@ant-design/icons'
import { Button, Card, Result, Skeleton, Typography } from 'antd'
import { useCallback, useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Line,
  LineChart,
  ReferenceLine,
  ResponsiveContainer,
  Tooltip as RechartsTooltip,
  XAxis,
  YAxis,
} from 'recharts'
import { HealthTrackingProgress } from '../../components/home/HealthTrackingProgress'
import { AppShell } from '../../components/layout/AppShell'
import { useAuth } from '../../stores/authStore'
import { getBristolTrend, type BristolLogPoint } from '../../api/bristol'
import { getFoodDiaryTrend, type DailyCountPoint } from '../../api/foodDiary'
import { getFoodSensitivityProfile, type FoodSensitivityProfileResponse } from '../../api/foodSensitivity'
import { foodSensitivityColor } from '../../lib/foodSensitivityColor'

const TEAL = '#0d9488'

function formatShortDay(iso: string): string {
  const d = new Date(iso)
  return `${d.getDate()}/${d.getMonth() + 1}`
}

// Khung 14 ngày gần nhất để luôn vẽ đủ trục/lưới biểu đồ kể cả chưa có dữ liệu — tránh vỡ bố
// cục khi Bristol/nhật ký ăn uống chưa được ghi nhận ngày nào.
function last14Days(): string[] {
  const days: string[] = []
  for (let i = 13; i >= 0; i -= 1) {
    const d = new Date()
    d.setDate(d.getDate() - i)
    days.push(d.toISOString().slice(0, 10))
  }
  return days
}

const { Title, Paragraph, Text } = Typography

export default function HomePage() {
  const { email, fullName } = useAuth()
  const [bristolTrend, setBristolTrend] = useState<BristolLogPoint[] | null>(null)
  const [foodTrend, setFoodTrend] = useState<DailyCountPoint[] | null>(null)
  const [sensitivity, setSensitivity] = useState<FoodSensitivityProfileResponse | null>(null)
  const [sensitivityError, setSensitivityError] = useState(false)
  const sensitivityRequestId = useRef(0)

  // Tách riêng để nút "Thử lại" gọi lại được; requestId bỏ kết quả của lần gọi cũ / khi đã rời trang.
  const loadSensitivity = useCallback(() => {
    const currentRequestId = ++sensitivityRequestId.current
    setSensitivity(null)
    setSensitivityError(false)
    getFoodSensitivityProfile(90)
      .then((res) => {
        if (currentRequestId === sensitivityRequestId.current) setSensitivity(res)
      })
      .catch(() => {
        if (currentRequestId === sensitivityRequestId.current) setSensitivityError(true)
      })
  }, [])

  useEffect(() => {
    let active = true
    getBristolTrend(14)
      .then((res) => active && setBristolTrend(res))
      .catch(() => active && setBristolTrend([]))
    getFoodDiaryTrend(14)
      .then((res) => active && setFoodTrend(res))
      .catch(() => active && setFoodTrend([]))
    loadSensitivity()
    return () => {
      active = false
      sensitivityRequestId.current += 1
    }
  }, [loadSensitivity])

  const bristolByDay = new Map((bristolTrend ?? []).map((p) => [p.loggedAt.slice(0, 10), p.bristolType]))
  const bristolChartData = last14Days().map((date) => ({
    day: formatShortDay(date),
    bristolType: bristolByDay.get(date) ?? null,
  }))
  const foodByDay = new Map((foodTrend ?? []).map((p) => [p.date, p.count]))
  const foodChartData = last14Days().map((date) => ({ day: formatShortDay(date), count: foodByDay.get(date) ?? 0 }))

  return (
    <AppShell>
      <div className="w-full grid gap-10 pb-2">
        <Card className="overflow-hidden rounded-2xl border border-teal-200/70 bg-gradient-to-br from-white via-teal-50/70 to-emerald-100/60 shadow-[0_10px_30px_rgba(15,118,110,0.08)] dark:border-teal-500/20! dark:from-slate-950 dark:via-teal-950/30 dark:to-emerald-950/30">
          <div className="flex items-start gap-4 py-2">
            <div className="hidden h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-teal-600 text-xl text-white shadow-lg shadow-teal-600/20 sm:flex">
              <HeartOutlined />
            </div>
            <div>
              <Text type="secondary">Tổng quan hôm nay</Text>
              <Title level={2} className="mb-2! mt-1!">Xin chào, {fullName ?? email} 👋</Title>
              <Paragraph type="secondary" className="mb-0! max-w-3xl">
                Theo dõi đều đặn giúp bạn nhận ra thay đổi trong sức khỏe tiêu hóa và cung cấp thông tin hữu ích
                hơn khi trao đổi với bác sĩ.
              </Paragraph>
            </div>
          </div>
        </Card>

        <HealthTrackingProgress />

        <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
          <Card className="rounded-2xl border-black/5 shadow-sm dark:border-white/10! dark:bg-neutral-900!" title="Xu hướng Bristol (14 ngày gần nhất)">
            {bristolTrend === null ? (
              <Skeleton active paragraph={{ rows: 3 }} />
            ) : (
              <ResponsiveContainer width="100%" height={220}>
                <LineChart data={bristolChartData} margin={{ left: -20, right: 8, top: 8 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" vertical={false} />
                  <XAxis dataKey="day" tick={{ fontSize: 11 }} stroke="#94a3b8" />
                  <YAxis domain={[1, 7]} ticks={[1, 2, 3, 4, 5, 6, 7]} tick={{ fontSize: 11 }} stroke="#94a3b8" width={24} />
                  <RechartsTooltip
                    formatter={(value) => (value === null ? ['Chưa ghi', 'Thang Bristol'] : [`Loại ${value}`, 'Thang Bristol'])}
                    labelFormatter={(label) => `Ngày ${label}`}
                  />
                  <Line
                    type="monotone"
                    dataKey="bristolType"
                    stroke={TEAL}
                    strokeWidth={2}
                    dot={{ r: 3, fill: TEAL }}
                    connectNulls={false}
                  />
                </LineChart>
              </ResponsiveContainer>
            )}
          </Card>

          <Card className="rounded-2xl border-black/5 shadow-sm dark:border-white/10! dark:bg-neutral-900!" title="Nhật ký ăn uống (14 ngày gần nhất)">
            {foodTrend === null ? (
              <Skeleton active paragraph={{ rows: 3 }} />
            ) : (
              <ResponsiveContainer width="100%" height={220}>
                <BarChart data={foodChartData} margin={{ left: -20, right: 8, top: 8 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" vertical={false} />
                  <XAxis dataKey="day" tick={{ fontSize: 11 }} stroke="#94a3b8" />
                  <YAxis allowDecimals={false} tick={{ fontSize: 11 }} stroke="#94a3b8" width={24} />
                  <RechartsTooltip formatter={(value) => [`${value} bữa`, 'Số lần ghi']} labelFormatter={(label) => `Ngày ${label}`} />
                  <Bar dataKey="count" fill={TEAL} radius={[4, 4, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            )}
          </Card>
        </div>

        <Card
          className="mt-12! rounded-2xl border-black/5 shadow-sm dark:border-white/10! dark:bg-neutral-900!"
          title="Nhạy cảm thực phẩm trong 90 ngày"
          extra={
            <Link to="/food-sensitivity">
              <Button size="small">Xem đầy đủ</Button>
            </Link>
          }
        >
          {sensitivityError ? (
            <Result
              status="warning"
              title="Không tải được dữ liệu"
              subTitle="Đã có lỗi khi tải phân tích nhạy cảm thực phẩm. Vui lòng thử lại."
              extra={
                <Button icon={<ReloadOutlined />} onClick={loadSensitivity}>
                  Thử lại
                </Button>
              }
            />
          ) : sensitivity === null ? (
            <Skeleton active paragraph={{ rows: 3 }} />
          ) : !sensitivity.hasEnoughData || sensitivity.topCorrelations.length === 0 ? (
            <Text type="secondary" className="text-sm">
              Chưa đủ dữ liệu để phân tích — ghi thêm nhật ký ăn uống kèm triệu chứng để AI gợi ý món ăn hay đi
              kèm triệu chứng của riêng bạn.
            </Text>
          ) : (
            <>
              <Text type="secondary" className="mb-3 block text-sm">
                Tỷ lệ bữa ăn kèm triệu chứng tính chung: {sensitivity.baselineSymptomRatePercent.toFixed(0)}%
              </Text>
              <ResponsiveContainer width="100%" height={Math.max(160, sensitivity.topCorrelations.slice(0, 6).length * 40)}>
                <BarChart
                  data={sensitivity.topCorrelations.slice(0, 6)}
                  layout="vertical"
                  margin={{ top: 24, right: 24, left: 0, bottom: 0 }}
                >
                  <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" horizontal={false} />
                  <XAxis type="number" domain={[0, 100]} tickFormatter={(v: number) => `${v}%`} tick={{ fontSize: 11 }} stroke="#94a3b8" />
                  <YAxis type="category" dataKey="foodKeyword" width={100} tick={{ fontSize: 12 }} stroke="#94a3b8" />
                  <RechartsTooltip
                    formatter={(value, _name, item) => {
                      const percent = typeof value === 'number' ? value : Number(value)
                      const payload = (item as { payload?: { timesWithSymptom: number; timesEaten: number } }).payload
                      const counts = payload ? ` (${payload.timesWithSymptom}/${payload.timesEaten} bữa)` : ''
                      return [`${percent.toFixed(0)}%${counts}`, 'Tỷ lệ kèm triệu chứng']
                    }}
                  />
                  <ReferenceLine
                    x={sensitivity.baselineSymptomRatePercent}
                    stroke="#94a3b8"
                    strokeWidth={1.5}
                    strokeDasharray="4 4"
                    label={{
                      value: 'Mức nền',
                      position: 'top',
                      fontSize: 11,
                      fontWeight: 600,
                      fill: '#f8fafc',
                    }}
                  />
                  <Bar dataKey="correlationPercent" radius={[0, 4, 4, 0]}>
                    {sensitivity.topCorrelations.slice(0, 6).map((item) => (
                      <Cell key={item.foodKeyword} fill={foodSensitivityColor(item, sensitivity.baselineSymptomRatePercent)} />
                    ))}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            </>
          )}
        </Card>
      </div>
    </AppShell>
  )
}
