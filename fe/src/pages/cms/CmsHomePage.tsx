import { AuditOutlined, DashboardOutlined, FileTextOutlined, TeamOutlined } from '@ant-design/icons'
import { Card, Typography } from 'antd'
import { Link } from 'react-router-dom'
import { CmsAppShell } from '../../components/cms/CmsAppShell'
import { useCmsAuth } from '../../stores/cmsAuthStore'

const { Title, Paragraph } = Typography

const ROLE_LABEL: Record<string, string> = {
  ADMIN: 'Admin',
  DOCTOR: 'Bác sĩ',
  CONTENT_EDITOR: 'Biên tập nội dung',
}

// Cùng nguyên tắc adminOnly với CmsSidebar — "Quản lý người dùng"/"Nhật ký hoạt động" chỉ
// dành cho Admin theo masterplan, Bác sĩ không nên thấy dù chỉ là thẻ giới thiệu tính năng.
const UPCOMING = [
  { icon: DashboardOutlined, title: 'Thống kê hệ thống', desc: 'Theo dõi lượt chat, triệu chứng phổ biến và cảnh báo Triage (UC51 & UC56).', to: '/cms/dashboard', adminOnly: true },
  { icon: TeamOutlined, title: 'Quản lý người dùng', desc: 'Xem, khóa/mở tài khoản bệnh nhân và gán vai trò RBAC.', to: '/cms/users', adminOnly: true },
  { icon: FileTextOutlined, title: 'Kho tài liệu y khoa', desc: 'Tải lên, cập nhật và gỡ tài liệu dùng cho trợ lý AI.', to: '/cms/documents' },
  { icon: AuditOutlined, title: 'Nhật ký hoạt động', desc: 'Theo dõi thao tác nhạy cảm để đảm bảo minh bạch.', adminOnly: true },
]

export default function CmsHomePage() {
  const { user } = useCmsAuth()
  const visibleUpcoming = UPCOMING.filter((item) => !item.adminOnly || user?.userType === 'ADMIN')

  return (
    <CmsAppShell>
      <Card className="rounded-2xl border-black/5 shadow-sm">
        <Title level={3} className="mb-1!">
          Chào {user?.fullName ?? user?.email}{user ? ` (${ROLE_LABEL[user.userType]})` : ''} 👋
        </Title>
        <Paragraph type="secondary" className="mb-0!">
          Đây là không gian quản trị GastroAI. Các phân hệ quản lý người dùng, kho tri thức và giám sát hệ thống
          đang được xây dựng theo kế hoạch giai đoạn tiếp theo.
        </Paragraph>
      </Card>

      <div className="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {visibleUpcoming.map(({ icon: Icon, title, desc, to }) => {
          const cardContent = (
            <Card
              key={title}
              hoverable={Boolean(to)}
              className="rounded-2xl border-black/5 shadow-sm h-full transition"
            >
              <span className="flex h-10 w-10 items-center justify-center rounded-full bg-slate-100 text-slate-600 dark:text-white">
                <Icon />
              </span>
              <Title level={5} className="mt-3 mb-1!">{title}</Title>
              <Paragraph type="secondary" className="mb-0! text-sm">{desc}</Paragraph>
            </Card>
          )

          return to ? (
            <Link key={title} to={to} className="no-underline block h-full">
              {cardContent}
            </Link>
          ) : (
            <div key={title} className="block h-full">
              {cardContent}
            </div>
          )
        })}
      </div>
    </CmsAppShell>
  )
}
