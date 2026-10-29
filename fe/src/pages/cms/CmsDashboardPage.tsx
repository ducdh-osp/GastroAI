import {
  AlertOutlined,
  ClockCircleOutlined,
  DownloadOutlined,
  FileExcelOutlined,
  FilePdfOutlined,
  LikeOutlined,
  MessageOutlined,
  ReloadOutlined,
  SmileOutlined,
  TeamOutlined,
} from '@ant-design/icons'
import {
  Alert,
  Button,
  Card,
  Col,
  Dropdown,
  Empty,
  message,
  Progress,
  Row,
  Select,
  Space,
  Spin,
  Statistic,
  Table,
  Tabs,
  Tag,
  Typography,
} from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { useEffect, useState } from 'react'
import {
  Area,
  AreaChart,
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Legend,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import {
  downloadDashboardReport,
  getDashboardStats,
  type DailyChatMetric,
  type DashboardStatsResponse,
  type SymptomMetric,
  type TriageGroupMetric,
} from '../../api/cmsDashboard'
import { CmsAppShell } from '../../components/cms/CmsAppShell'

const { Title, Text, Paragraph } = Typography

const PIE_COLORS = ['#ef4444', '#f59e0b', '#3b82f6', '#10b981', '#8b5cf6', '#ec4899', '#6366f1']

export default function CmsDashboardPage() {
  const [days, setDays] = useState(30)
  const [data, setData] = useState<DashboardStatsResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [exporting, setExporting] = useState<'EXCEL' | 'PDF' | null>(null)
  const [error, setError] = useState<string | null>(null)

  async function loadData(selectedDays = days) {
    setLoading(true)
    setError(null)
    try {
      const res = await getDashboardStats(selectedDays)
      setData(res)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không thể tải dữ liệu thống kê.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void loadData(days)
  }, [days])

  async function handleExport(format: 'EXCEL' | 'PDF') {
    setExporting(format)
    try {
      await downloadDashboardReport(format, days)
      message.success(`Đã xuất báo cáo ${format === 'PDF' ? 'PDF' : 'Excel'} thành công!`)
    } catch (err) {
      message.error(err instanceof Error ? err.message : 'Xuất báo cáo thất bại.')
    } finally {
      setExporting(null)
    }
  }

  const exportMenuItems = [
    {
      key: 'excel',
      label: 'Xuất file Excel (.xlsx)',
      icon: <FileExcelOutlined className="text-emerald-600" />,
      onClick: () => void handleExport('EXCEL'),
    },
    {
      key: 'pdf',
      label: 'Xuất file PDF (.pdf)',
      icon: <FilePdfOutlined className="text-rose-600" />,
      onClick: () => void handleExport('PDF'),
    },
  ]

  // Table columns for Symptoms tab
  const symptomColumns: ColumnsType<SymptomMetric> = [
    {
      title: 'Triệu chứng tiêu hóa',
      dataIndex: 'symptomName',
      key: 'symptomName',
      render: (name: string, row) => (
        <div>
          <div className="font-medium text-slate-800">{name}</div>
          <Text type="secondary" className="text-xs font-mono">{row.symptomCode}</Text>
        </div>
      ),
    },
    {
      title: 'Số ca ghi nhận',
      dataIndex: 'totalCount',
      key: 'totalCount',
      sorter: (a, b) => a.totalCount - b.totalCount,
      render: (val: number) => <span className="font-semibold text-slate-700">{val}</span>,
    },
    {
      title: 'Ca khẩn cấp (Emergency)',
      dataIndex: 'emergencyCount',
      key: 'emergencyCount',
      render: (val: number) =>
        val > 0 ? (
          <Tag color="red" className="font-semibold">{val} ca</Tag>
        ) : (
          <span className="text-slate-400">0</span>
        ),
    },
    {
      title: 'Tỷ lệ',
      dataIndex: 'percentage',
      key: 'percentage',
      render: (val: number) => (
        <div className="flex items-center gap-2">
          <Progress percent={val} size="small" strokeColor="#3b82f6" className="mb-0 max-w-[120px]" />
          <span className="text-xs text-slate-500">{val}%</span>
        </div>
      ),
    },
  ]

  // Table columns for Triage Groups tab
  const triageColumns: ColumnsType<TriageGroupMetric> = [
    {
      title: 'Nhóm dấu hiệu cảnh báo',
      dataIndex: 'groupName',
      key: 'groupName',
      render: (name: string, row) => (
        <div>
          <div className="font-medium text-slate-800">{name}</div>
          <Text type="secondary" className="text-xs font-mono">{row.groupCode}</Text>
        </div>
      ),
    },
    {
      title: 'Số ca kích hoạt',
      dataIndex: 'count',
      key: 'count',
      sorter: (a, b) => a.count - b.count,
      render: (val: number) => <Tag color="volcano" className="font-bold text-sm px-2 py-0.5">{val}</Tag>,
    },
    {
      title: 'Tỷ lệ',
      dataIndex: 'percentage',
      key: 'percentage',
      render: (val: number) => (
        <div className="flex items-center gap-2">
          <Progress percent={val} size="small" strokeColor="#f97316" className="mb-0 max-w-[120px]" />
          <span className="text-xs text-slate-500">{val}%</span>
        </div>
      ),
    },
  ]

  // Table columns for Daily Chats tab
  const chatColumns: ColumnsType<DailyChatMetric> = [
    { title: 'Ngày', dataIndex: 'day', key: 'day', render: (d: string) => <span className="font-medium">{d}</span> },
    { title: 'Tổng tin nhắn', dataIndex: 'totalMessages', key: 'totalMessages' },
    { title: 'Người bệnh gửi', dataIndex: 'patientMessages', key: 'patientMessages' },
    { title: 'AI phản hồi', dataIndex: 'botMessages', key: 'botMessages' },
    {
      title: 'Cảnh báo khẩn cấp',
      dataIndex: 'emergencyMessages',
      key: 'emergencyMessages',
      render: (val: number) =>
        val > 0 ? <Tag color="error">{val}</Tag> : <span className="text-slate-400">0</span>,
    },
  ]

  // Format short day label for line chart: "YYYY-MM-DD" -> "DD/MM"
  const chartData = (data?.dailyChats || []).map((item) => ({
    ...item,
    shortDay: item.day.length >= 10 ? `${item.day.slice(8, 10)}/${item.day.slice(5, 7)}` : item.day,
  }))

  return (
    <CmsAppShell>
      <div className="space-y-6">
        {/* Header Bar */}
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <Title level={3} className="mb-1! tracking-tight">
              Thống kê & Báo cáo hệ thống
            </Title>
            <Paragraph type="secondary" className="mb-0! text-sm">
              Theo dõi lượt tương tác hỏi đáp AI, phân bố triệu chứng và chỉ số an toàn Triage theo thời gian thực.
            </Paragraph>
          </div>

          <Space wrap size="middle">
            <Select
              value={days}
              onChange={(val) => setDays(val)}
              className="w-36"
              options={[
                { value: 7, label: '7 ngày qua' },
                { value: 14, label: '14 ngày qua' },
                { value: 30, label: '30 ngày qua' },
              ]}
            />

            <Button
              icon={<ReloadOutlined />}
              onClick={() => void loadData(days)}
              loading={loading}
            >
              Làm mới
            </Button>

            <Dropdown menu={{ items: exportMenuItems }} placement="bottomRight">
              <Button
                type="primary"
                icon={<DownloadOutlined />}
                loading={exporting !== null}
                className="bg-blue-600 hover:bg-blue-500 shadow-sm"
              >
                Xuất báo cáo
              </Button>
            </Dropdown>
          </Space>
        </div>

        {error && <Alert type="error" message={error} showIcon closable onClose={() => setError(null)} />}

        {loading && !data ? (
          <div className="flex h-64 items-center justify-center">
            <Spin size="large" tip="Đang tải dữ liệu thống kê..." />
          </div>
        ) : !data ? (
          <Empty description="Không có dữ liệu thống kê." />
        ) : (
          <>
            {/* KPI Cards Row */}
            <Row gutter={[16, 16]}>
              <Col xs={24} sm={12} lg={6}>
                <Card className="rounded-2xl border-slate-100 shadow-sm transition hover:shadow-md">
                  <div className="flex items-start justify-between">
                    <div>
                      <Text type="secondary" className="text-xs uppercase font-medium tracking-wider">
                        Người dùng
                      </Text>
                      <Statistic
                        value={data.summary.totalPatients}
                        className="mt-1"
                        valueStyle={{ fontSize: '1.75rem', fontWeight: 700, color: '#1e293b' }}
                      />
                      <div className="mt-2 text-xs text-slate-500 flex items-center gap-1">
                        <ClockCircleOutlined />
                        <span>{data.summary.totalSessions} phiên chat đã mở</span>
                      </div>
                    </div>
                    <span className="flex h-12 w-12 items-center justify-center rounded-xl bg-blue-50 text-blue-600 text-xl">
                      <TeamOutlined />
                    </span>
                  </div>
                </Card>
              </Col>

              <Col xs={24} sm={12} lg={6}>
                <Card className="rounded-2xl border-slate-100 shadow-sm transition hover:shadow-md">
                  <div className="flex items-start justify-between">
                    <div>
                      <Text type="secondary" className="text-xs uppercase font-medium tracking-wider">
                        Lượt Chat AI
                      </Text>
                      <Statistic
                        value={data.summary.totalMessages}
                        className="mt-1"
                        valueStyle={{ fontSize: '1.75rem', fontWeight: 700, color: '#0f766e' }}
                      />
                      <div className="mt-2 text-xs text-emerald-600 font-medium flex items-center gap-1">
                        <MessageOutlined />
                        <span>+{data.summary.todayMessages} tin nhắn hôm nay</span>
                      </div>
                    </div>
                    <span className="flex h-12 w-12 items-center justify-center rounded-xl bg-teal-50 text-teal-600 text-xl">
                      <MessageOutlined />
                    </span>
                  </div>
                </Card>
              </Col>

              <Col xs={24} sm={12} lg={6}>
                <Card className="rounded-2xl border-slate-100 shadow-sm transition hover:shadow-md">
                  <div className="flex items-start justify-between">
                    <div>
                      <Text type="secondary" className="text-xs uppercase font-medium tracking-wider">
                        Cảnh báo Triage
                      </Text>
                      <Statistic
                        value={data.summary.totalAlerts}
                        className="mt-1"
                        valueStyle={{ fontSize: '1.75rem', fontWeight: 700, color: '#e11d48' }}
                      />
                      <div className="mt-2 flex items-center gap-2">
                        {data.summary.newAlerts > 0 ? (
                          <Tag color="red" className="m-0 text-xs font-semibold animate-pulse">
                            {data.summary.newAlerts} mới
                          </Tag>
                        ) : (
                          <Tag color="green" className="m-0 text-xs">Ổn định</Tag>
                        )}
                        <span className="text-xs text-slate-500">
                          {data.summary.resolvedAlerts} đã xử lý
                        </span>
                      </div>
                    </div>
                    <span className="flex h-12 w-12 items-center justify-center rounded-xl bg-rose-50 text-rose-600 text-xl">
                      <AlertOutlined />
                    </span>
                  </div>
                </Card>
              </Col>

              <Col xs={24} sm={12} lg={6}>
                <Card className="rounded-2xl border-slate-100 shadow-sm transition hover:shadow-md">
                  <div className="flex items-start justify-between">
                    <div>
                      <Text type="secondary" className="text-xs uppercase font-medium tracking-wider">
                        Đánh giá chất lượng AI
                      </Text>
                      <Statistic
                        value={data.summary.satisfactionRate}
                        suffix="%"
                        className="mt-1"
                        valueStyle={{ fontSize: '1.75rem', fontWeight: 700, color: '#7c3aed' }}
                      />
                      <div className="mt-2 text-xs text-slate-500 flex items-center gap-1">
                        <LikeOutlined className="text-blue-500" />
                        <span>{data.summary.helpfulRatings} thích</span>
                        <span className="text-slate-300">|</span>
                        <span>{data.summary.unhelpfulRatings} chưa hài lòng</span>
                      </div>
                    </div>
                    <span className="flex h-12 w-12 items-center justify-center rounded-xl bg-purple-50 text-purple-600 text-xl">
                      <SmileOutlined />
                    </span>
                  </div>
                </Card>
              </Col>
            </Row>

            {/* Charts Row */}
            <Row gutter={[16, 16]}>
              {/* Daily Chat Activity Chart */}
              <Col xs={24} lg={16}>
                <Card
                  title={<span className="font-semibold text-slate-800">Xu hướng tương tác Chat theo ngày</span>}
                  extra={<Text type="secondary" className="text-xs">{data.fromDate} → {data.toDate}</Text>}
                  className="rounded-2xl border-slate-100 shadow-sm"
                >
                  <div className="h-72 w-full">
                    <ResponsiveContainer width="100%" height="100%">
                      <AreaChart data={chartData} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                        <defs>
                          <linearGradient id="colorTotal" x1="0" y1="0" x2="0" y2="1">
                            <stop offset="5%" stopColor="#3b82f6" stopOpacity={0.4} />
                            <stop offset="95%" stopColor="#3b82f6" stopOpacity={0.0} />
                          </linearGradient>
                          <linearGradient id="colorEmergency" x1="0" y1="0" x2="0" y2="1">
                            <stop offset="5%" stopColor="#ef4444" stopOpacity={0.4} />
                            <stop offset="95%" stopColor="#ef4444" stopOpacity={0.0} />
                          </linearGradient>
                        </defs>
                        <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f1f5f9" />
                        <XAxis dataKey="shortDay" tick={{ fontSize: 12, fill: '#64748b' }} axisLine={false} tickLine={false} />
                        <YAxis tick={{ fontSize: 12, fill: '#64748b' }} axisLine={false} tickLine={false} />
                        <Tooltip
                          contentStyle={{ borderRadius: 12, border: 'none', boxShadow: '0 4px 12px rgba(0,0,0,0.1)' }}
                          formatter={(value, name) => [
                            value,
                            name === 'totalMessages'
                              ? 'Tổng tin nhắn'
                              : name === 'emergencyMessages'
                              ? 'Tin nhắn khẩn cấp'
                              : name,
                          ]}
                          labelFormatter={(label) => `Ngày: ${label}`}
                        />
                        <Legend
                          verticalAlign="top"
                          align="right"
                          formatter={(value) => (value === 'totalMessages' ? 'Tổng tin nhắn' : 'Khẩn cấp')}
                        />
                        <Area
                          type="monotone"
                          dataKey="totalMessages"
                          stroke="#3b82f6"
                          strokeWidth={2}
                          fillOpacity={1}
                          fill="url(#colorTotal)"
                        />
                        <Area
                          type="monotone"
                          dataKey="emergencyMessages"
                          stroke="#ef4444"
                          strokeWidth={2}
                          fillOpacity={1}
                          fill="url(#colorEmergency)"
                        />
                      </AreaChart>
                    </ResponsiveContainer>
                  </div>
                </Card>
              </Col>

              {/* Triage Alert Distribution Pie */}
              <Col xs={24} lg={8}>
                <Card
                  title={<span className="font-semibold text-slate-800">Phân bố cảnh báo Triage</span>}
                  extra={<Tag color="volcano">{data.triageGroups.length} nhóm</Tag>}
                  className="rounded-2xl border-slate-100 shadow-sm"
                >
                  <div className="h-72 w-full flex flex-col items-center justify-center">
                    {data.triageGroups.length === 0 ? (
                      <Empty description="Chưa có cảnh báo Triage nào." image={Empty.PRESENTED_IMAGE_SIMPLE} />
                    ) : (
                      <ResponsiveContainer width="100%" height="100%">
                        <PieChart>
                          <Pie
                            data={data.triageGroups}
                            dataKey="count"
                            nameKey="groupName"
                            cx="50%"
                            cy="50%"
                            innerRadius={50}
                            outerRadius={80}
                            paddingAngle={4}
                          >
                            {data.triageGroups.map((_, index) => (
                              <Cell key={`cell-${index}`} fill={PIE_COLORS[index % PIE_COLORS.length]} />
                            ))}
                          </Pie>
                          <Tooltip
                            contentStyle={{ borderRadius: 12, border: 'none', boxShadow: '0 4px 12px rgba(0,0,0,0.1)' }}
                            formatter={(value, name) => [`${value} ca`, name]}
                          />
                        </PieChart>
                      </ResponsiveContainer>
                    )}
                    {data.triageGroups.length > 0 && (
                      <div className="mt-2 flex flex-wrap justify-center gap-2 text-xs">
                        {data.triageGroups.slice(0, 4).map((g, idx) => (
                          <span key={g.groupCode} className="inline-flex items-center gap-1">
                            <span
                              className="h-2.5 w-2.5 rounded-full"
                              style={{ backgroundColor: PIE_COLORS[idx % PIE_COLORS.length] }}
                            />
                            <span className="text-slate-600 truncate max-w-[120px]">{g.groupName}</span>
                          </span>
                        ))}
                      </div>
                    )}
                  </div>
                </Card>
              </Col>
            </Row>

            {/* Top Symptoms Bar Chart */}
            <Card
              title={<span className="font-semibold text-slate-800">Top Triệu chứng tiêu hóa được báo cáo</span>}
              className="rounded-2xl border-slate-100 shadow-sm"
            >
              <div className="h-64 w-full">
                {data.topSymptoms.length === 0 ? (
                  <Empty description="Chưa có ghi nhận triệu chứng trong khoảng thời gian này." image={Empty.PRESENTED_IMAGE_SIMPLE} />
                ) : (
                  <ResponsiveContainer width="100%" height="100%">
                    <BarChart data={data.topSymptoms} margin={{ top: 10, right: 20, left: 0, bottom: 20 }}>
                      <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f1f5f9" />
                      <XAxis
                        dataKey="symptomName"
                        tick={{ fontSize: 12, fill: '#475569' }}
                        interval={0}
                        angle={-10}
                        textAnchor="end"
                        axisLine={false}
                        tickLine={false}
                      />
                      <YAxis tick={{ fontSize: 12, fill: '#64748b' }} axisLine={false} tickLine={false} />
                      <Tooltip
                        contentStyle={{ borderRadius: 12, border: 'none', boxShadow: '0 4px 12px rgba(0,0,0,0.1)' }}
                        formatter={(val, name) => [
                          `${val} ca`,
                          name === 'totalCount' ? 'Tổng số ca' : 'Ca khẩn cấp',
                        ]}
                      />
                      <Legend
                        verticalAlign="top"
                        align="right"
                        formatter={(val) => (val === 'totalCount' ? 'Tổng số ca' : 'Khẩn cấp')}
                      />
                      <Bar dataKey="totalCount" fill="#3b82f6" radius={[6, 6, 0, 0]} maxBarSize={48} />
                      <Bar dataKey="emergencyCount" fill="#ef4444" radius={[6, 6, 0, 0]} maxBarSize={48} />
                    </BarChart>
                  </ResponsiveContainer>
                )}
              </div>
            </Card>

            {/* Detailed Data Tables (Tabs) */}
            <Card className="rounded-2xl border-slate-100 shadow-sm">
              <Tabs
                defaultActiveKey="symptoms"
                items={[
                  {
                    key: 'symptoms',
                    label: `Chi tiết triệu chứng (${data.topSymptoms.length})`,
                    children: (
                      <Table
                        columns={symptomColumns}
                        dataSource={data.topSymptoms}
                        rowKey="symptomCode"
                        pagination={false}
                        size="middle"
                      />
                    ),
                  },
                  {
                    key: 'triage',
                    label: `Chi tiết cảnh báo Triage (${data.triageGroups.length})`,
                    children: (
                      <Table
                        columns={triageColumns}
                        dataSource={data.triageGroups}
                        rowKey="groupCode"
                        pagination={false}
                        size="middle"
                      />
                    ),
                  },
                  {
                    key: 'chats',
                    label: `Lịch sử chat theo ngày (${data.dailyChats.length})`,
                    children: (
                      <Table
                        columns={chatColumns}
                        dataSource={data.dailyChats}
                        rowKey="day"
                        pagination={{ pageSize: 10 }}
                        size="middle"
                      />
                    ),
                  },
                ]}
              />
            </Card>
          </>
        )}
      </div>
    </CmsAppShell>
  )
}
