import {
  AuditOutlined,
  BookOutlined,
  DashboardOutlined,
  DownOutlined,
  FileTextOutlined,
  HistoryOutlined,
  HomeOutlined,
  LogoutOutlined,
  QuestionCircleOutlined,
  SafetyCertificateOutlined,
  TeamOutlined,
} from '@ant-design/icons'
import { Dropdown, Tag } from 'antd'
import type { ReactNode } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { Logo } from '../brand/Logo'
import { useCmsAuth } from '../../stores/cmsAuthStore'

interface NavItem {
  key: string
  label: string
  icon: ReactNode
  to?: string
  comingSoon?: boolean
  adminOnly?: boolean
}

// adminOnly khớp đúng cột Actor trong masterplan (UC0046/47/51/52 chỉ ghi "Admin", không có
// "Bác sĩ") — Bác sĩ không nên thấy các mục này dù chỉ là placeholder "Sắp có", vì sau này
// cũng sẽ không bao giờ có quyền vào. "Kho tri thức" (UC0048/62/65) actor ghi "Admin / Bác sĩ"
// nên cả 2 vai trò đều thấy.
const NAV_SECTIONS: { title: string; items: NavItem[] }[] = [
  {
    title: 'Tổng quan',
    items: [{ key: 'home', label: 'Trang chủ', icon: <HomeOutlined />, to: '/cms' }],
  },
  {
    title: 'Tài khoản',
    items: [{ key: 'login-history', label: 'Lịch sử đăng nhập', icon: <HistoryOutlined />, to: '/cms/login-history' }],
  },
  {
    title: 'Người dùng',
    items: [
      { key: 'users', label: 'Quản lý người dùng', icon: <TeamOutlined />, comingSoon: true, adminOnly: true },
      { key: 'roles', label: 'Phân quyền RBAC', icon: <SafetyCertificateOutlined />, comingSoon: true, adminOnly: true },
    ],
  },
  {
    title: 'Kho tri thức',
    items: [
      { key: 'documents', label: 'Tài liệu y khoa', icon: <FileTextOutlined />, comingSoon: true },
      { key: 'articles', label: 'Bài viết kiến thức', icon: <BookOutlined />, comingSoon: true },
      { key: 'faq', label: 'Câu hỏi thường gặp', icon: <QuestionCircleOutlined />, comingSoon: true },
    ],
  },
  {
    title: 'Giám sát',
    items: [
      { key: 'dashboard', label: 'Thống kê hệ thống', icon: <DashboardOutlined />, comingSoon: true, adminOnly: true },
      { key: 'audit', label: 'Nhật ký hoạt động', icon: <AuditOutlined />, comingSoon: true, adminOnly: true },
    ],
  },
]

const ROLE_LABEL: Record<string, string> = {
  ADMIN: 'Admin',
  DOCTOR: 'Bác sĩ',
}

/**
 * Sidebar cố định cho cổng CMS — nền tối, tách biệt trực quan với cổng bệnh nhân.
 * Các mục chưa có trang thật (comingSoon) hiển thị mờ + tag "Sắp có", không điều hướng đi
 * đâu cả — tránh tạo link chết trong lúc các UC0046+ chưa được code (xem masterplan GĐ5).
 */
export function CmsSidebar() {
  const { user, logout } = useCmsAuth()
  const location = useLocation()

  const visibleSections = NAV_SECTIONS.map((section) => ({
    ...section,
    items: section.items.filter((item) => !item.adminOnly || user?.userType === 'ADMIN'),
  })).filter((section) => section.items.length > 0)

  return (
    <aside className="flex h-svh w-64 shrink-0 flex-col justify-between border-r border-white/10 bg-[#0f172a] text-white">
      <div>
        <div className="p-5">
          <Logo variant="light" />
          <p className="mt-1 text-[0.65rem] tracking-[0.3em] text-white/50 uppercase">Cổng quản trị</p>
        </div>

        <nav className="mt-2 space-y-5 px-3">
          {visibleSections.map((section) => (
            <div key={section.title}>
              <p className="px-2 text-[0.65rem] font-semibold tracking-[0.2em] text-white/40 uppercase">
                {section.title}
              </p>
              <div className="mt-1.5 space-y-0.5">
                {section.items.map((item) => {
                  const active = item.to === location.pathname
                  if (item.comingSoon || !item.to) {
                    return (
                      <div
                        key={item.key}
                        className="flex cursor-not-allowed items-center justify-between gap-2 rounded-lg px-3 py-2 text-sm text-white/35"
                      >
                        <span className="flex items-center gap-2.5">
                          {item.icon}
                          {item.label}
                        </span>
                        <Tag className="m-0! border-0! bg-white/10! text-[0.6rem]! text-white/50!">Sắp có</Tag>
                      </div>
                    )
                  }
                  return (
                    <Link
                      key={item.key}
                      to={item.to}
                      className={`flex items-center gap-2.5 rounded-lg px-3 py-2 text-sm transition-colors ${
                        active ? 'bg-teal-500/15 text-teal-300' : 'text-white/80 hover:bg-white/5'
                      }`}
                    >
                      {item.icon}
                      {item.label}
                    </Link>
                  )
                })}
              </div>
            </div>
          ))}
        </nav>
      </div>

      <div className="border-t border-white/10 p-3">
        <Dropdown
          menu={{
            items: [{ key: 'logout', icon: <LogoutOutlined />, label: 'Đăng xuất', danger: true, onClick: logout }],
          }}
          trigger={['click']}
        >
          <button className="flex w-full items-center gap-2.5 rounded-lg px-2 py-2 text-left hover:bg-white/5">
            <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-teal-500/20 text-sm font-semibold text-teal-300">
              {(user?.fullName ?? user?.email ?? '?').charAt(0).toUpperCase()}
            </span>
            <span className="min-w-0 flex-1">
              <span className="block truncate text-sm font-medium text-white">{user?.fullName ?? user?.email}</span>
              <span className="block text-xs text-white/50">{user ? ROLE_LABEL[user.userType] : ''}</span>
            </span>
            <DownOutlined className="text-xs text-white/40" />
          </button>
        </Dropdown>
      </div>
    </aside>
  )
}
