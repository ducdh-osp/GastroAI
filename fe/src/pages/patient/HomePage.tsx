import { HeartOutlined } from '@ant-design/icons'
import { Card, Typography } from 'antd'
import { HealthTrackingProgress } from '../../components/home/HealthTrackingProgress'
import { AppShell } from '../../components/layout/AppShell'
import { useAuth } from '../../stores/authStore'

const { Title, Paragraph, Text } = Typography

export default function HomePage() {
  const { email, fullName } = useAuth()

  return (
    <AppShell>
      <div className="mx-auto max-w-5xl space-y-8">
        <Card className="overflow-hidden rounded-2xl border-teal-200/60 bg-gradient-to-br from-white via-teal-50/70 to-emerald-100/60 shadow-sm dark:border-teal-500/20 dark:from-slate-950 dark:via-teal-950/30 dark:to-emerald-950/30">
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
      </div>
    </AppShell>
  )
}
