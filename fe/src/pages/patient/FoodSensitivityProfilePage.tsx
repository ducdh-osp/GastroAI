import { BulbOutlined } from '@ant-design/icons'
import { Alert, Card, Select, Spin, Table, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { useEffect, useRef, useState } from 'react'
import { Bar, BarChart, CartesianGrid, Cell, ReferenceLine, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { getFoodSensitivityProfile } from '../../api/foodSensitivity'
import type { FoodCorrelationItem, FoodSensitivityProfileResponse } from '../../api/foodSensitivity'
import { AppShell } from '../../components/layout/AppShell'

const { Title, Text } = Typography

const DAYS_OPTIONS = [
  { value: 30, label: '30 ngày qua' },
  { value: 90, label: '90 ngày qua' },
  { value: 180, label: '180 ngày qua' },
]

/** Cao hơn tỷ lệ nền bao nhiêu điểm % thì coi là "đáng chú ý" - chỉ để tô màu gợi ý, không
 * phải ngưỡng thống kê chặt chẽ (UC0022 là phân tích đơn giản, không phải chẩn đoán). */
const NOTABLE_GAP_POINTS = 20

function colorFor(item: FoodCorrelationItem, baseline: number): string {
  const gap = item.correlationPercent - baseline
  if (gap >= NOTABLE_GAP_POINTS) return '#cf1322' // đỏ - cao hơn hẳn mức nền
  if (gap > 0) return '#d48806' // vàng cam - hơi cao hơn mức nền
  return '#8c8c8c' // xám - không cao hơn mức nền
}

export default function FoodSensitivityProfilePage() {
  const [days, setDays] = useState(90)
  const [profile, setProfile] = useState<FoodSensitivityProfileResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const requestId = useRef(0)

  useEffect(() => {
    const currentRequestId = ++requestId.current
    setLoading(true)
    setError(null)
    getFoodSensitivityProfile(days)
      .then((data) => {
        if (currentRequestId !== requestId.current) return
        setProfile(data)
      })
      .catch((err: unknown) => {
        if (currentRequestId !== requestId.current) return
        setError(err instanceof Error ? err.message : 'Không thể tải hồ sơ nhạy cảm thực phẩm.')
      })
      .finally(() => {
        if (currentRequestId === requestId.current) setLoading(false)
      })
  }, [days])

  const columns: ColumnsType<FoodCorrelationItem> = [
    { title: 'Từ khoá món ăn', dataIndex: 'foodKeyword', key: 'foodKeyword' },
    { title: 'Số lần ăn', dataIndex: 'timesEaten', key: 'timesEaten', width: 110 },
    { title: 'Số lần kèm triệu chứng', dataIndex: 'timesWithSymptom', key: 'timesWithSymptom', width: 170 },
    {
      title: 'Tỷ lệ kèm triệu chứng', key: 'correlationPercent', width: 160,
      render: (_, record) => (
        <span style={{ color: colorFor(record, profile?.baselineSymptomRatePercent ?? 0), fontWeight: 600 }}>
          {record.correlationPercent.toFixed(0)}%
        </span>
      ),
    },
  ]

  return (
    <AppShell>
      <div className="mx-auto max-w-4xl">
        <div className="mb-6 flex flex-wrap items-end justify-between gap-3">
          <div>
            <Text type="secondary">Theo dõi sức khỏe</Text>
            <Title level={2} className="mb-1! mt-1!">Hồ sơ nhạy cảm thực phẩm</Title>
            <Text type="secondary">
              Đối chiếu nhật ký ăn uống với nhật ký Bristol để gợi ý món ăn có khả năng liên quan
              đến triệu chứng tiêu hóa của bạn.
            </Text>
          </div>
          <Select value={days} onChange={setDays} options={DAYS_OPTIONS} style={{ width: 160 }} />
        </div>

        <div className="mb-6">
          <Alert
            type="info"
            showIcon
            icon={<BulbOutlined />}
            message="Đây là gợi ý sơ bộ, không phải chẩn đoán"
            description="Kết quả tính bằng cách đếm tần suất đơn giản, không phải xét nghiệm y khoa. Một món ăn có tỷ lệ cao có thể chỉ là trùng hợp nếu bạn mới ăn vài lần. Hãy trao đổi với bác sĩ trước khi tự loại bỏ hẳn 1 loại thực phẩm khỏi khẩu phần."
          />
        </div>

        {error && <Alert type="error" message={error} showIcon className="mb-4" />}

        <Spin spinning={loading}>
          {profile && !profile.hasEnoughData && (
            <Card className="rounded-2xl border-black/5 shadow-sm">
              <Text>
                Chưa đủ dữ liệu để phân tích - mới ghi nhận <strong>{profile.totalMealsAnalyzed}</strong> bữa ăn
                trong {profile.daysAnalyzed} ngày gần nhất. Hãy tiếp tục ghi nhật ký ăn uống đều đặn, phân tích
                sẽ đáng tin cậy hơn khi có nhiều dữ liệu hơn.
              </Text>
            </Card>
          )}

          {profile && profile.hasEnoughData && profile.topCorrelations.length === 0 && (
            <Card className="rounded-2xl border-black/5 shadow-sm">
              <Text>
                Đã phân tích {profile.totalMealsAnalyzed} bữa ăn trong {profile.daysAnalyzed} ngày, nhưng chưa có
                món ăn nào lặp lại đủ nhiều lần để đưa ra gợi ý đáng tin cậy. Mô tả món ăn giống nhau giữa các
                lần ghi (vd luôn ghi "cơm gà" thay vì lúc "cơm gà", lúc "gà") sẽ giúp phân tích chính xác hơn.
              </Text>
            </Card>
          )}

          {profile && profile.hasEnoughData && profile.topCorrelations.length > 0 && (
            <>
              <Card className="mb-4 rounded-2xl border-black/5 shadow-sm">
                <Text type="secondary">
                  Đã phân tích <strong>{profile.totalMealsAnalyzed}</strong> bữa ăn trong{' '}
                  <strong>{profile.daysAnalyzed}</strong> ngày gần nhất. Tỷ lệ bữa ăn có kèm triệu chứng tính
                  chung (đường tham chiếu trên biểu đồ): <strong>{profile.baselineSymptomRatePercent.toFixed(0)}%</strong>.
                </Text>
              </Card>

              <Card className="mb-4 rounded-2xl border-black/5 shadow-sm">
                <ResponsiveContainer width="100%" height={Math.max(200, profile.topCorrelations.length * 44)}>
                  <BarChart
                    data={profile.topCorrelations}
                    layout="vertical"
                    margin={{ top: 8, right: 24, left: 0, bottom: 0 }}
                  >
                    <CartesianGrid strokeDasharray="3 3" horizontal={false} />
                    <XAxis type="number" domain={[0, 100]} tickFormatter={(v: number) => `${v}%`} tick={{ fontSize: 11 }} />
                    <YAxis type="category" dataKey="foodKeyword" width={110} tick={{ fontSize: 12 }} />
                    <Tooltip
                      formatter={(value, _name, item) => {
                        const percent = typeof value === 'number' ? value : Number(value)
                        const payload = (item as { payload?: FoodCorrelationItem }).payload
                        const counts = payload ? ` (${payload.timesWithSymptom}/${payload.timesEaten} bữa)` : ''
                        return [`${percent.toFixed(0)}%${counts}`, 'Tỷ lệ kèm triệu chứng']
                      }}
                    />
                    <ReferenceLine
                      x={profile.baselineSymptomRatePercent}
                      stroke="#595959"
                      strokeDasharray="4 4"
                      label={{ value: 'Mức nền', position: 'insideTopRight', fontSize: 11, fill: '#595959' }}
                    />
                    <Bar dataKey="correlationPercent" radius={[0, 4, 4, 0]}>
                      {profile.topCorrelations.map((item) => (
                        <Cell key={item.foodKeyword} fill={colorFor(item, profile.baselineSymptomRatePercent)} />
                      ))}
                    </Bar>
                  </BarChart>
                </ResponsiveContainer>
              </Card>

              <Card className="rounded-2xl border-black/5 shadow-sm">
                <Table<FoodCorrelationItem>
                  columns={columns}
                  dataSource={profile.topCorrelations}
                  rowKey="foodKeyword"
                  size="small"
                  pagination={false}
                />
              </Card>
            </>
          )}
        </Spin>
      </div>
    </AppShell>
  )
}
