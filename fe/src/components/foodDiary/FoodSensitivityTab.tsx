import { BulbOutlined, EyeOutlined } from '@ant-design/icons'
import { Alert, Button, Card, Col, Progress, Row, Segmented, Spin, Statistic, Table, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { Bar, BarChart, CartesianGrid, Cell, ReferenceLine, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { getFoodSensitivityProfile } from '../../api/foodSensitivity'
import type { FoodCorrelationItem, FoodSensitivityProfileResponse } from '../../api/foodSensitivity'

const { Text, Title } = Typography
const MIN_MEALS_FOR_ANALYSIS = 15
const NOTABLE_GAP_POINTS = 20
const METRIC_CARD_CLASS =
  'h-full overflow-hidden rounded-2xl border border-slate-200/80 bg-white shadow-[0_8px_24px_rgba(15,23,42,0.06)] transition-all duration-200 hover:-translate-y-0.5 hover:shadow-[0_14px_32px_rgba(15,23,42,0.1)] dark:border-white/10 dark:bg-slate-900/80'
const CONTENT_CARD_CLASS =
  'overflow-hidden rounded-2xl border border-slate-200/80 bg-white shadow-[0_10px_30px_rgba(15,23,42,0.07)] [&_.ant-card-head]:border-slate-100 [&_.ant-card-head]:bg-slate-50/80 [&_.ant-card-head-title]:py-4 dark:border-white/10 dark:bg-slate-900/80 dark:[&_.ant-card-head]:border-white/10 dark:[&_.ant-card-head]:bg-white/[0.03]'

function colorFor(item: FoodCorrelationItem, baseline: number): string {
  const gap = item.correlationPercent - baseline
  if (gap >= NOTABLE_GAP_POINTS) return '#cf1322'
  if (gap > 0) return '#d48806'
  return '#8c8c8c'
}

interface FoodSensitivityTabProps {
  days: 30 | 90 | 180
  onDaysChange: (days: 30 | 90 | 180) => void
  onViewEvidence: (keyword: string, days: number) => void
}

export function FoodSensitivityTab({ days, onDaysChange, onViewEvidence }: FoodSensitivityTabProps) {
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
        if (requestId.current === currentRequestId) setProfile(data)
      })
      .catch((err: unknown) => {
        if (requestId.current === currentRequestId) {
          setError(err instanceof Error ? err.message : 'Không thể tải hồ sơ nhạy cảm thực phẩm.')
        }
      })
      .finally(() => {
        if (requestId.current === currentRequestId) setLoading(false)
      })
  }, [days])

  const columns: ColumnsType<FoodCorrelationItem> = [
    { title: 'Từ khoá món ăn', dataIndex: 'foodKeyword', key: 'foodKeyword' },
    { title: 'Số lần ăn', dataIndex: 'timesEaten', key: 'timesEaten', width: 100 },
    { title: 'Kèm triệu chứng', dataIndex: 'timesWithSymptom', key: 'timesWithSymptom', width: 140 },
    {
      title: 'Tỷ lệ', key: 'correlationPercent', width: 90,
      render: (_, record) => (
        <span style={{ color: colorFor(record, profile?.baselineSymptomRatePercent ?? 0), fontWeight: 600 }}>
          {record.correlationPercent.toFixed(0)}%
        </span>
      ),
    },
    {
      title: '', key: 'actions', width: 155,
      render: (_, record) => (
        <Button
          size="small"
          type="link"
          icon={<EyeOutlined />}
          onClick={() => onViewEvidence(record.foodKeyword, days)}
        >
          Xem các lần liên quan
        </Button>
      ),
    },
  ]

  return (
    <div className="grid gap-6 pb-2">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <Title level={4} className="mb-1!">Nhạy cảm thực phẩm</Title>
          <Text type="secondary">Đối chiếu bữa ăn, triệu chứng tự ghi và nhật ký Bristol.</Text>
        </div>
        <Segmented
          value={days}
          options={[
            { value: 30, label: '30 ngày' },
            { value: 90, label: '90 ngày' },
            { value: 180, label: '180 ngày' },
          ]}
          onChange={(value) => onDaysChange(value as 30 | 90 | 180)}
        />
      </div>

      <Alert
        type="info"
        showIcon
        icon={<BulbOutlined />}
        message="Đây là gợi ý sơ bộ, không phải chẩn đoán"
        description="Kết quả dựa trên tần suất xuất hiện cùng triệu chứng và có thể chỉ là trùng hợp. Hãy trao đổi với bác sĩ trước khi loại bỏ hẳn một thực phẩm khỏi khẩu phần."
      />
      {error && <Alert type="error" message={error} showIcon />}

      <Spin spinning={loading}>
        <div className="grid gap-6">
          {profile && !profile.hasEnoughData && (
            <Card className={CONTENT_CARD_CLASS}>
              <Title level={5}>Chưa đủ dữ liệu để phân tích</Title>
              <Text type="secondary">
                Đã ghi {profile.totalMealsAnalyzed}/{MIN_MEALS_FOR_ANALYSIS} bữa trong {profile.daysAnalyzed} ngày gần nhất.
                Hãy tiếp tục ghi đều đặn để kết quả có ý nghĩa hơn.
              </Text>
              <Progress
                className="mt-5"
                percent={Math.min(100, Math.round(profile.totalMealsAnalyzed / MIN_MEALS_FOR_ANALYSIS * 100))}
                format={() => `${profile.totalMealsAnalyzed}/${MIN_MEALS_FOR_ANALYSIS} bữa`}
              />
            </Card>
          )}

          {profile?.hasEnoughData && (
            <div className="grid gap-6">
              <Row gutter={[24, 24]}>
                <Col xs={24} sm={12}>
                  <Card className={METRIC_CARD_CLASS}>
                    <Statistic title="Tổng bữa đã phân tích" value={profile.totalMealsAnalyzed} suffix="bữa" />
                  </Card>
                </Col>
                <Col xs={24} sm={12}>
                  <Card className={METRIC_CARD_CLASS}>
                    <Statistic
                      title="Tỷ lệ bữa có kèm triệu chứng"
                      value={profile.baselineSymptomRatePercent}
                      precision={0}
                      suffix="%"
                    />
                  </Card>
                </Col>
              </Row>

              {profile.topCorrelations.length === 0 ? (
                <Card className={CONTENT_CARD_CLASS}>
                  <Text>
                    Chưa có món ăn nào lặp lại đủ nhiều để đưa ra gợi ý. Hãy mô tả món ăn nhất quán giữa các lần ghi.
                  </Text>
                </Card>
              ) : (
                <div className="grid gap-6">
                  <Card className={CONTENT_CARD_CLASS} title="Thực phẩm thường đi kèm triệu chứng">
                    <ResponsiveContainer width="100%" height={Math.max(220, profile.topCorrelations.length * 44)}>
                      <BarChart
                        data={profile.topCorrelations}
                        layout="vertical"
                        margin={{ top: 8, right: 24, left: 0, bottom: 0 }}
                      >
                        <CartesianGrid strokeDasharray="3 3" horizontal={false} />
                        <XAxis type="number" domain={[0, 100]} tickFormatter={(value: number) => `${value}%`} tick={{ fontSize: 11 }} />
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

                  <Card className={CONTENT_CARD_CLASS}>
                    <Table<FoodCorrelationItem>
                      columns={columns}
                      dataSource={profile.topCorrelations}
                      rowKey="foodKeyword"
                      size="small"
                      pagination={false}
                      scroll={{ x: 700 }}
                    />
                  </Card>
                </div>
              )}

              <div className="pt-1 text-right">
                <Link
                  to="/chat"
                  state={{
                    initialQuestion: `Tôi muốn hiểu hồ sơ nhạy cảm thực phẩm của mình. Trong ${profile.daysAnalyzed} ngày qua, các món hay xuất hiện cùng triệu chứng gồm: ${profile.topCorrelations.slice(0, 3).map((item) => `${item.foodKeyword} (${item.timesWithSymptom}/${item.timesEaten} lần)`).join(', ') || 'chưa có món đủ dữ liệu'}. Đây chỉ là tương quan; tôi nên xem xét điều gì?`,
                  }}
                >
                  <Button type="primary">Hỏi AI về kết quả này</Button>
                </Link>
              </div>
            </div>
          )}
        </div>
      </Spin>
    </div>
  )
}
