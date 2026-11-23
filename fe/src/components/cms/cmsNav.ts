import {
  AuditOutlined,
  AlertOutlined,
  BookOutlined,
  DashboardOutlined,
  ExperimentOutlined,
  FileTextOutlined,
  MailOutlined,
  MedicineBoxOutlined,
  QuestionCircleOutlined,
  SafetyCertificateOutlined,
  SafetyOutlined,
  SettingOutlined,
  SoundOutlined,
  TagsOutlined,
  TeamOutlined,
} from '@ant-design/icons'
import type { ComponentType } from 'react'

export interface CmsNavItem {
  icon: ComponentType<{ className?: string }>
  title: string
  desc: string
  to: string
  adminOnly?: boolean
  roles?: string[]
}

export interface CmsNavGroup {
  key: string
  title: string
  items: CmsNavItem[]
}

// Dùng chung cho CmsHeader (dropdown điều hướng) và CmsHomePage (lưới ô Trang chủ) — xem
// 00-General/04-QuanTri-AdminCMS/SCR-AD-00-TrangChu.md. Giữ đúng phân quyền adminOnly/roles đã
// có ở CmsSidebar cũ.
export const CMS_NAV_GROUPS: CmsNavGroup[] = [
  {
    key: 'account',
    title: 'Tài khoản',
    items: [
      { icon: TeamOutlined, title: 'Quản lý người dùng', desc: 'Xem, khoá/mở tài khoản bệnh nhân.', to: '/cms/users', adminOnly: true },
      { icon: SafetyCertificateOutlined, title: 'Phân quyền nhân sự', desc: 'Gán vai trò cho tài khoản nội bộ (RBAC).', to: '/cms/rbac', adminOnly: true },
    ],
  },
  {
    key: 'knowledge',
    title: 'Kho tri thức',
    items: [
      { icon: FileTextOutlined, title: 'Tài liệu y khoa', desc: 'Tải lên, quản lý phiên bản và xác nhận nội dung dùng cho AI.', to: '/cms/documents' },
      { icon: BookOutlined, title: 'Bài viết kiến thức', desc: 'Soạn & duyệt bài viết chuyên sâu cho bệnh nhân.', to: '/cms/articles' },
      { icon: TagsOutlined, title: 'Chủ đề bài viết', desc: 'Quản lý chủ đề dùng chung cho bài viết và FAQ.', to: '/cms/article-topics' },
      { icon: QuestionCircleOutlined, title: 'Câu hỏi thường gặp', desc: 'Soạn & duyệt FAQ hiển thị cho bệnh nhân.', to: '/cms/faq' },
      { icon: MedicineBoxOutlined, title: 'Danh mục chuẩn hoá', desc: 'Danh mục triệu chứng/thuốc dùng làm dropdown.', to: '/cms/catalogs' },
      { icon: AlertOutlined, title: 'Từ khoá Triage', desc: 'Quản lý từ khoá sàng lọc dấu hiệu nguy hiểm.', to: '/cms/triage-keywords', roles: ['ADMIN', 'CONTENT_EDITOR'] },
    ],
  },
  {
    key: 'monitoring',
    title: 'Giám sát',
    items: [
      { icon: SafetyOutlined, title: 'Cảnh báo khẩn cấp', desc: 'Theo dõi và xác nhận escalate các ca Triage cần can thiệp.', to: '/cms/triage-alerts', roles: ['ADMIN', 'CONTENT_EDITOR'] },
      { icon: ExperimentOutlined, title: 'Đánh giá AI & Triage', desc: 'Chất lượng truy xuất RAG và độ nhạy sàng lọc an toàn.', to: '/cms/evaluation', roles: ['ADMIN', 'CONTENT_EDITOR'] },
      { icon: SoundOutlined, title: 'Thông báo broadcast', desc: 'Gửi thông báo chung tới toàn bộ/1 nhóm bệnh nhân.', to: '/cms/broadcast', adminOnly: true },
      { icon: DashboardOutlined, title: 'Thống kê hệ thống', desc: 'Lượt chat, triệu chứng phổ biến, số lần Triage kích hoạt.', to: '/cms/dashboard', adminOnly: true },
      { icon: AuditOutlined, title: 'Nhật ký hoạt động', desc: 'Theo dõi thao tác nhạy cảm để đảm bảo minh bạch.', to: '/cms/audit-logs', adminOnly: true },
    ],
  },
  {
    key: 'config',
    title: 'Cấu hình',
    items: [
      { icon: SettingOutlined, title: 'Cấu hình RAG/Triage', desc: 'Tinh chỉnh tham số truy xuất và kênh cảnh báo.', to: '/cms/runtime-config', adminOnly: true },
      { icon: MailOutlined, title: 'Mẫu email', desc: 'Chỉnh nội dung email xác thực, nhắc thuốc, cảnh báo.', to: '/cms/email-templates', adminOnly: true },
    ],
  },
]

export function filterCmsNavGroups(userType: string | undefined): CmsNavGroup[] {
  return CMS_NAV_GROUPS.map((group) => ({
    ...group,
    items: group.items.filter(
      (item) => (!item.adminOnly || userType === 'ADMIN') && (!item.roles || item.roles.includes(userType ?? '')),
    ),
  })).filter((group) => group.items.length > 0)
}
