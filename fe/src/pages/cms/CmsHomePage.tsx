import { AlertOutlined, DashboardOutlined, MessageOutlined, SmileOutlined, TeamOutlined } from '@ant-design/icons'
import { Button, Card, Empty, Skeleton, Tag, Typography } from 'antd'
import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  Area,
  AreaChart,
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip as RechartsTooltip,
  XAxis,
  YAxis,
} from 'recharts'
import { getDashboardStats, type DashboardStatsResponse } from '../../api/cmsDashboard'
import { CmsAppShell } from '../../components/cms/CmsAppShell'
import { useCmsAuth } from '../../stores/cmsAuthStore'

const { Title, Paragraph, Text } = Typography

const ROLE_LABEL: Record<string, string> = {
  ADMIN: 'Admin',
  DOCTOR: 'Bác sĩ',
  CONTENT_EDITOR: 'Biên tập chuyên môn y khoa',
  CONTENT_AUTHOR: 'Cộng tác viên nội dung',
  OPERATIONS: 'Giám sát vận hành',
}

const PIE_COLORS = ['#ef4444', '#f59e0b', '#3b82f6', '#10b981', '#8b5cf6', '#ec4899', '#6366f1']
const PREVIEW_DAYS = 14

// Trang chủ CMS — kiểu dashboard (xem HomePage.tsx phía bệnh nhân), KHÔNG phải lưới ô bấm điều
// hướng nữa: điều hướng đã có đầy đủ qua dropdown trên header (CmsHeader/cmsNav.ts), lưới ô ở
// đây chỉ trùng lặp. Biểu đồ lấy lại từ CmsDashboardPage.tsx (cùng API getDashboardStats), rút
// gọn còn 14 ngày cố định làm bản xem nhanh — xem đầy đủ + đổi khoảng ngày thì qua /cms/dashboard.
export default function CmsHomePage() {
  const { user } = useCmsAuth()
  const [data, setData] = useState<DashboardStatsResponse | null>(null)

  useEffect(() => {
    let active = true
    getDashboardStats(PREVIEW_DAYS)
      .then((res) => active && setData(res))
      .catch(() => active && setData(null))
    return () => {
      active = false
    }
  }, [])

  const chartData = (data?.dailyChats ?? []).map((item) => ({
    ...item,
    shortDay: item.day.length >= 10 ? `${item.day.slice(8, 10)}/${item.day.slice(5, 7)}` : item.day,
  }))

  return (
    <CmsAppShell>
      <div className="w-full grid gap-8 pb-2">
        <Card className="overflow-hidden rounded-2xl border border-teal-200/70 bg-gradient-to-br from-white via-teal-50/70 to-emerald-100/60 shadow-[0_10px_30px_rgba(15,118,110,0.08)] dark:border-teal-500/20 dark:from-slate-950 dark:via-teal-950/30 dark:to-emerald-950/30">
          <div className="flex items-start gap-4 py-2">
            <div className="hidden h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-teal-600 text-xl text-white shadow-lg shadow-teal-600/20 sm:flex">
              <DashboardOutlined />
            </div>
            <div>
              <Text type="secondary">Tổng quan hệ thống</Text>
              <Title level={2} className="mb-2! mt-1!">
                Chào {user?.fullName ?? user?.email}{user ? ` - ${ROLE_LABEL[user.userType] ?? user.userType}` : ''} 👋
              </Title>
              <Paragraph type="secondary" className="mb-0! max-w-3xl">
                Số liệu vận hành {PREVIEW_DAYS} ngày gần nhất. Mọi chức năng quản trị nằm trên menu header phía trên.
              </Paragraph>
            </div>
          </div>
        </Card>

        {data === null ? (
          <Skeleton active paragraph={{ rows: 4 }} />
        ) : (
          <>
            <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
              <Card className="rounded-2xl border-black/5 shadow-sm">
                <div className="flex items-center gap-3">
                  <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-blue-50 text-blue-600 text-lg dark:bg-blue-500/10">
                    <TeamOutlined />
                  </span>
                  <div>
                    <Text type="secondary" className="text-xs">Người dùng</Text>
                    <div className="text-xl font-bold">{data.summary.totalPatients}</div>
                  </div>
                </div>
              </Card>
              <Card className="rounded-2xl border-black/5 shadow-sm">
                <div className="flex items-center gap-3">
                  <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-teal-50 text-teal-600 text-lg dark:bg-teal-500/10">
                    <MessageOutlined />
                  </span>
                  <div>
                    <Text type="secondary" className="text-xs">Lượt Chat AI</Text>
                    <div className="text-xl font-bold">{data.summary.totalMessages}</div>
                  </div>
                </div>
              </Card>
              <Card className="rounded-2xl border-black/5 shadow-sm">
                <div className="flex items-center gap-3">
                  <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-rose-50 text-rose-600 text-lg dark:bg-rose-500/10">
                    <AlertOutlined />
                  </span>
                  <div>
                    <Text type="secondary" className="text-xs">Cảnh báo Triage</Text>
                    <div className="text-xl font-bold">
                      {data.summary.totalAlerts}
                      {data.summary.newAlerts > 0 && (
                        <Tag color="red" className="ml-2 align-middle text-xs">{data.summary.newAlerts} mới</Tag>
                      )}
                    </div>
                  </div>
                </div>
              </Card>
              <Card className="rounded-2xl border-black/5 shadow-sm">
                <div className="flex items-center gap-3">
                  <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-purple-50 text-purple-600 text-lg dark:bg-purple-500/10">
                    <SmileOutlined />
                  </span>
                  <div>
                    <Text type="secondary" className="text-xs">Hài lòng AI</Text>
                    <div className="text-xl font-bold">{data.summary.satisfactionRate}%</div>
                  </div>
                </div>
              </Card>
            </div>

            <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
              <Card className="rounded-2xl border-black/5 shadow-sm" title={`Lượt chat theo ngày (${PREVIEW_DAYS} ngày gần nhất)`}>
                <ResponsiveContainer width="100%" height={220}>
                  <AreaChart data={chartData} margin={{ left: -20, right: 8, top: 8 }}>
                    <defs>
                      <linearGradient id="homeColorTotal" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="5%" stopColor="#3b82f6" stopOpacity={0.4} />
                        <stop offset="95%" stopColor="#3b82f6" stopOpacity={0} />
                      </linearGradient>
                    </defs>
                    <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" vertical={false} />
                    <XAxis dataKey="shortDay" tick={{ fontSize: 11 }} stroke="#94a3b8" />
                    <YAxis allowDecimals={false} tick={{ fontSize: 11 }} stroke="#94a3b8" width={24} />
                    <RechartsTooltip formatter={(value) => [`${value}`, 'Tổng tin nhắn']} labelFormatter={(label) => `Ngày ${label}`} />
                    <Area type="monotone" dataKey="totalMessages" stroke="#3b82f6" strokeWidth={2} fillOpacity={1} fill="url(#homeColorTotal)" />
                  </AreaChart>
                </ResponsiveContainer>
              </Card>

              <Card className="rounded-2xl border-black/5 shadow-sm" title="Phân bố cảnh báo Triage">
                {data.triageGroups.length === 0 ? (
                  <div className="flex h-[220px] items-center justify-center">
                    <Empty description="Chưa có cảnh báo Triage nào." image={Empty.PRESENTED_IMAGE_SIMPLE} />
                  </div>
                ) : (
                  <ResponsiveContainer width="100%" height={220}>
                    <PieChart>
                      <Pie data={data.triageGroups} dataKey="count" nameKey="groupName" cx="50%" cy="50%" innerRadius={45} outerRadius={75} paddingAngle={4}>
                        {data.triageGroups.map((_, index) => (
                          <Cell key={`cell-${index}`} fill={PIE_COLORS[index % PIE_COLORS.length]} />
                        ))}
                      </Pie>
                      <RechartsTooltip formatter={(value, name) => [`${value} ca`, name]} />
                    </PieChart>
                  </ResponsiveContainer>
                )}
              </Card>
            </div>

            <Card
              className="rounded-2xl border-black/5 shadow-sm"
              title="Top triệu chứng tiêu hoá được báo cáo"
              extra={
                <Link to="/cms/dashboard">
                  <Button size="small">Xem đầy đủ</Button>
                </Link>
              }
            >
              {data.topSymptoms.length === 0 ? (
                <Empty description="Chưa có ghi nhận triệu chứng trong khoảng thời gian này." image={Empty.PRESENTED_IMAGE_SIMPLE} />
              ) : (
                <ResponsiveContainer width="100%" height={220}>
                  <BarChart data={data.topSymptoms} margin={{ top: 8, right: 20, left: 0, bottom: 20 }}>
                    <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" vertical={false} />
                    <XAxis dataKey="symptomName" tick={{ fontSize: 11 }} interval={0} angle={-10} textAnchor="end" stroke="#94a3b8" />
                    <YAxis allowDecimals={false} tick={{ fontSize: 11 }} stroke="#94a3b8" width={24} />
                    <RechartsTooltip formatter={(val) => [`${val} ca`, 'Tổng số ca']} />
                    <Bar dataKey="totalCount" fill="#0d9488" radius={[6, 6, 0, 0]} maxBarSize={44} />
                  </BarChart>
                </ResponsiveContainer>
              )}
            </Card>
          </>
        )}
      </div>
    </CmsAppShell>
  )
}
