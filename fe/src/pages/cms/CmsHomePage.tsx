import { Card, Typography } from 'antd'
import { Link } from 'react-router-dom'
import { CmsAppShell } from '../../components/cms/CmsAppShell'
import { filterCmsNavGroups } from '../../components/cms/cmsNav'
import { useCmsAuth } from '../../stores/cmsAuthStore'

const { Title, Paragraph, Text } = Typography

const ROLE_LABEL: Record<string, string> = {
  ADMIN: 'Admin',
  DOCTOR: 'Bác sĩ',
  CONTENT_EDITOR: 'Biên tập nội dung',
}

// Trang chủ CMS thay thế vai trò điều hướng của CmsSidebar cũ (xem 00-General/
// 04-QuanTri-AdminCMS/SCR-AD-00-TrangChu.md). Dữ liệu nhóm/phân quyền dùng chung với CmsHeader
// qua cmsNav.ts.
export default function CmsHomePage() {
  const { user } = useCmsAuth()
  const visibleGroups = filterCmsNavGroups(user?.userType)

  return (
    <CmsAppShell>
      <div className="w-full">
        <Title level={3} className="mb-1!">
          Chào {user?.fullName ?? user?.email}{user ? ` (${ROLE_LABEL[user.userType]})` : ''} 👋
        </Title>
        <Paragraph type="secondary" className="mb-6!">
          Chọn một chức năng bên dưới để bắt đầu — có thể quay lại Trang chủ bất cứ lúc nào qua menu trên header.
        </Paragraph>

        <div className="space-y-8">
          {visibleGroups.map((group) => (
            <section key={group.key}>
              <Text strong className="mb-3 block text-sm tracking-wide text-gray-500 uppercase dark:text-white/60">
                {group.title}
              </Text>
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
                {group.items.map(({ icon: Icon, title, desc, to }) => (
                  <Link key={to} to={to} className="block h-full no-underline">
                    <Card hoverable className="h-full rounded-2xl border-black/5 shadow-sm transition">
                      <span className="flex h-10 w-10 items-center justify-center rounded-full bg-teal-50 text-teal-600 dark:bg-teal-500/10 dark:text-teal-300">
                        <Icon className="text-lg" />
                      </span>
                      <Title level={5} className="mt-3 mb-1!">{title}</Title>
                      <Paragraph type="secondary" className="mb-0! text-sm">{desc}</Paragraph>
                    </Card>
                  </Link>
                ))}
              </div>
            </section>
          ))}
        </div>
      </div>
    </CmsAppShell>
  )
}
