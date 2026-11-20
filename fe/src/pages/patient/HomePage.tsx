import { FireOutlined, TrophyOutlined } from '@ant-design/icons'
import { Button, Card, Modal, Progress, Segmented, Skeleton, Tooltip, Typography } from 'antd'
import { useEffect, useState, type ReactNode } from 'react'
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
import { AppShell } from '../../components/layout/AppShell'
import { useAuth } from '../../stores/authStore'
import { getAdherence, type AdherenceResponse } from '../../api/adherence'
import { getBristolTrend, type BristolLogPoint } from '../../api/bristol'
import { getFoodDiaryTrend, type DailyCountPoint } from '../../api/foodDiary'
import { getFoodSensitivityProfile, type FoodSensitivityProfileResponse } from '../../api/foodSensitivity'
import { foodSensitivityColor } from '../../lib/foodSensitivityColor'

const TEAL = '#0d9488'
const TEAL_LIGHT = '#5eead4'

function formatDay(iso: string): string {
  const [year, month, day] = iso.split('-')
  return `${day}/${month}/${year}`
}

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

function StatPill({ icon, label, value, hint }: { icon: ReactNode; label: string; value: ReactNode; hint?: string }) {
  return (
    <div className="rounded-2xl border border-white/15 bg-white/10 px-4 py-3 backdrop-blur-sm">
      <div className="flex items-center gap-2 text-white/70">
        <span className="text-base">{icon}</span>
        <Text className="text-xs font-medium tracking-wide text-white/70 uppercase">{label}</Text>
      </div>
      <div className="mt-1 text-2xl font-bold text-white">{value}</div>
      {hint ? <Text className="text-xs text-white/60">{hint}</Text> : null}
    </div>
  )
}

function StreakGrid({ days }: { days: AdherenceResponse['days'] }) {
  return (
    <div className="grid grid-cols-10 gap-1.5 sm:grid-cols-15">
      {days.map((point) => (
        <Tooltip
          key={point.date}
          title={`${formatDay(point.date)}: ${point.foodDiaryCount} bữa ăn, ${point.bristolCount} lần Bristol`}
        >
          <span
            aria-label={`${formatDay(point.date)} ${point.logged ? 'đã ghi' : 'chưa ghi'}`}
            className="block aspect-square w-full rounded"
            style={{ background: point.logged ? TEAL : '#e5e7eb' }}
          />
        </Tooltip>
      ))}
    </div>
  )
}

export default function HomePage() {
  const { email, fullName } = useAuth()
  const displayName = fullName ?? email
  const [adherence, setAdherence] = useState<AdherenceResponse | null>(null)
  const [loadingAdherence, setLoadingAdherence] = useState(true)
  const [streakDetailOpen, setStreakDetailOpen] = useState(false)
  const [modalDays, setModalDays] = useState(30)
  const [modalAdherence, setModalAdherence] = useState<AdherenceResponse | null>(null)
  const [loadingModal, setLoadingModal] = useState(false)

  const [bristolTrend, setBristolTrend] = useState<BristolLogPoint[] | null>(null)
  const [foodTrend, setFoodTrend] = useState<DailyCountPoint[] | null>(null)
  const [sensitivity, setSensitivity] = useState<FoodSensitivityProfileResponse | null>(null)

  useEffect(() => {
    let active = true
    getAdherence(30)
      .then((res) => {
        if (active) setAdherence(res)
      })
      .catch(() => {
        // Trang chủ không nên vỡ vì 1 widget phụ lỗi — chỉ ẩn phần streak nếu tải thất bại.
      })
      .finally(() => {
        if (active) setLoadingAdherence(false)
      })
    getBristolTrend(14)
      .then((res) => active && setBristolTrend(res))
      .catch(() => active && setBristolTrend([]))
    getFoodDiaryTrend(14)
      .then((res) => active && setFoodTrend(res))
      .catch(() => active && setFoodTrend([]))
    getFoodSensitivityProfile(90)
      .then((res) => active && setSensitivity(res))
      .catch(() => {})
    return () => {
      active = false
    }
  }, [])

  useEffect(() => {
    if (!streakDetailOpen) return
    let active = true
    setLoadingModal(true)
    getAdherence(modalDays)
      .then((res) => active && setModalAdherence(res))
      .catch(() => {})
      .finally(() => active && setLoadingModal(false))
    return () => {
      active = false
    }
  }, [streakDetailOpen, modalDays])

  const bristolByDay = new Map((bristolTrend ?? []).map((p) => [p.loggedAt.slice(0, 10), p.bristolType]))
  const bristolChartData = last14Days().map((date) => ({
    day: formatShortDay(date),
    bristolType: bristolByDay.get(date) ?? null,
  }))
  const foodByDay = new Map((foodTrend ?? []).map((p) => [p.date, p.count]))
  const foodChartData = last14Days().map((date) => ({ day: formatShortDay(date), count: foodByDay.get(date) ?? 0 }))

  return (
    <AppShell>
      <div className="w-full">
        {/* Hero — cùng ngôn ngữ thương hiệu với AuthShell (gradient teal + hoạ tiết lưới) để
            người dùng cảm nhận liền mạch từ lúc đăng nhập tới Trang chủ. */}
        <div
          className="relative overflow-hidden rounded-3xl p-6 text-white shadow-lg shadow-teal-900/10 sm:p-8"
          style={{ background: 'radial-gradient(120% 120% at 15% 0%, #14b8a6 0%, #0d9488 45%, #134e4a 100%)' }}
        >
          <div className="bg-brand-grid absolute inset-0 opacity-60" aria-hidden />
          <div className="grain-overlay absolute inset-0" aria-hidden />

          <div className="relative">
            <Text className="text-xs tracking-[0.3em] text-white/70 uppercase">Chào mừng trở lại</Text>
            <Title level={2} className="mt-2 mb-1! text-white!">
              Chào {displayName} 👋
            </Title>
            <Paragraph className="mb-0! max-w-xl text-white/80">
              Theo dõi tiến trình sức khoẻ tiêu hoá của bạn ngay dưới đây. Dùng menu trên header để trò chuyện cùng
              AI hoặc ghi nhận thêm dữ liệu mới.
            </Paragraph>

            <div className="mt-6">
              {loadingAdherence ? (
                <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
                  {[0, 1, 2].map((i) => (
                    <div key={i} className="rounded-2xl border border-white/15 bg-white/10 p-4">
                      <Skeleton active paragraph={false} title={{ width: '60%' }} className="opacity-70" />
                    </div>
                  ))}
                </div>
              ) : adherence ? (
                <>
                  <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
                    <StatPill
                      icon={<FireOutlined />}
                      label="Streak hiện tại"
                      value={`${adherence.currentStreak} ngày`}
                      hint={!adherence.loggedToday && adherence.currentStreak > 0 ? 'Hôm nay chưa ghi — ghi ngay để giữ streak' : undefined}
                    />
                    <StatPill icon={<TrophyOutlined />} label="Streak dài nhất" value={`${adherence.longestStreak} ngày`} />
                    <div className="rounded-2xl border border-white/15 bg-white/10 px-4 py-3 backdrop-blur-sm">
                      <Text className="text-xs font-medium tracking-wide text-white/70 uppercase">Tỷ lệ tuân thủ (30 ngày)</Text>
                      <div className="mt-1 text-2xl font-bold text-white">{adherence.adherencePercent.toFixed(0)}%</div>
                      <Progress
                        percent={Math.round(adherence.adherencePercent)}
                        showInfo={false}
                        strokeColor={TEAL_LIGHT}
                        railColor="rgba(255,255,255,0.2)"
                        size="small"
                        className="mt-1"
                      />
                    </div>
                  </div>
                  <button
                    type="button"
                    onClick={() => {
                      setModalDays(30)
                      setStreakDetailOpen(true)
                    }}
                    className="mt-3 inline-block cursor-pointer border-0 bg-transparent p-0 text-sm text-white/80 hover:text-white hover:underline"
                  >
                    Xem chi tiết streak & tuân thủ →
                  </button>
                </>
              ) : null}
            </div>
          </div>
        </div>

        <div className="mt-8 grid grid-cols-1 gap-6 lg:grid-cols-2">
          <Card className="rounded-2xl border-black/5 shadow-sm" title="Xu hướng Bristol (14 ngày gần nhất)">
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

          <Card className="rounded-2xl border-black/5 shadow-sm" title="Nhật ký ăn uống (14 ngày gần nhất)">
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
          className="mt-12! rounded-2xl border-black/5 shadow-sm"
          title="Nhạy cảm thực phẩm trong 90 ngày"
          extra={
            <Link to="/food-sensitivity">
              <Button size="small">Xem đầy đủ</Button>
            </Link>
          }
        >
          {sensitivity === null ? (
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

      <Modal
        title="Streak & tỷ lệ tuân thủ"
        open={streakDetailOpen}
        onCancel={() => setStreakDetailOpen(false)}
        footer={null}
      >
        <div className="mt-2">
          <div className="flex items-center justify-between gap-3">
            <Text type="secondary" className="text-sm">
              Một ngày được tính là "đã ghi" khi có ít nhất 1 mục nhật ký ăn uống hoặc Bristol.
            </Text>
            <Segmented
              size="small"
              value={modalDays}
              onChange={(value) => setModalDays(value as number)}
              options={[
                { label: '7 ngày', value: 7 },
                { label: '30 ngày', value: 30 },
                { label: '90 ngày', value: 90 },
              ]}
            />
          </div>

          {loadingModal || !modalAdherence ? (
            <Skeleton active className="mt-4" paragraph={{ rows: 4 }} />
          ) : (
            <>
              <div className="mt-4 grid grid-cols-3 gap-3">
                <Card size="small" className="rounded-xl border-black/5 text-center">
                  <Text type="secondary" className="text-xs"><FireOutlined /> Hiện tại</Text>
                  <div className="text-xl font-semibold">{modalAdherence.currentStreak} ngày</div>
                </Card>
                <Card size="small" className="rounded-xl border-black/5 text-center">
                  <Text type="secondary" className="text-xs"><TrophyOutlined /> Dài nhất</Text>
                  <div className="text-xl font-semibold">{modalAdherence.longestStreak} ngày</div>
                </Card>
                <Card size="small" className="rounded-xl border-black/5 text-center">
                  <Text type="secondary" className="text-xs">Tuân thủ</Text>
                  <div className="text-xl font-semibold">{modalAdherence.adherencePercent.toFixed(0)}%</div>
                </Card>
              </div>

              <div className="mt-4">
                <StreakGrid days={modalAdherence.days} />
              </div>
              <div className="mt-3 flex gap-4 text-xs text-gray-500">
                <span>
                  <span className="mr-1 inline-block h-3 w-3 rounded align-middle" style={{ background: TEAL }} />
                  Đã ghi
                </span>
                <span>
                  <span className="mr-1 inline-block h-3 w-3 rounded align-middle" style={{ background: '#e5e7eb' }} />
                  Chưa ghi
                </span>
              </div>
            </>
          )}
        </div>
      </Modal>
    </AppShell>
  )
}
