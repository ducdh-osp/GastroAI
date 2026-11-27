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
import type { CmsRole } from '../../api/cmsAuth'

/** Tên hiển thị vai trò CMS — đúng tên trong SCR-AD-03 §3. */
export const CMS_ROLE_LABEL: Record<CmsRole, string> = {
  ADMIN: 'Admin',
  CONTENT_EDITOR: 'Biên tập chuyên môn y khoa',
  CONTENT_AUTHOR: 'Cộng tác viên nội dung',
  OPERATIONS: 'Giám sát vận hành',
}

export interface CmsNavItem {
  icon: ComponentType<{ className?: string }>
  title: string
  desc: string
  to: string
  /** Vai trò được thấy mục này — đúng ma trận SCR-AD-03 §4. Backend (SecurityConfig) vẫn là chốt chặn thật. */
  roles: CmsRole[]
}

export interface CmsNavGroup {
  key: string
  title: string
  items: CmsNavItem[]
}

const ADMIN: CmsRole[] = ['ADMIN']
const ADMIN_EDITOR: CmsRole[] = ['ADMIN', 'CONTENT_EDITOR']
const CONTENT_TEAM: CmsRole[] = ['ADMIN', 'CONTENT_EDITOR', 'CONTENT_AUTHOR']
const ADMIN_OPERATIONS: CmsRole[] = ['ADMIN', 'OPERATIONS']

// Dùng chung cho CmsHeader (dropdown điều hướng) và CmsHomePage (lối tắt cho vai trò không xem
// dashboard) — xem 00-General/04-QuanTri-AdminCMS/SCR-AD-00-TrangChu.md và ma trận SCR-AD-03 §4.
// "Đánh giá AI & Triage" chưa có trong ma trận; giữ Admin-only cho khớp SecurityConfig.
export const CMS_NAV_GROUPS: CmsNavGroup[] = [
  {
    key: 'account',
    title: 'Tài khoản',
    items: [
      { icon: TeamOutlined, title: 'Quản lý người dùng', desc: 'Xem, khoá/mở tài khoản bệnh nhân.', to: '/cms/users', roles: ADMIN },
      { icon: SafetyCertificateOutlined, title: 'Phân quyền nhân sự', desc: 'Gán vai trò cho tài khoản nội bộ (RBAC).', to: '/cms/rbac', roles: ADMIN },
    ],
  },
  {
    key: 'knowledge',
    title: 'Kho tri thức',
    items: [
      { icon: FileTextOutlined, title: 'Tài liệu y khoa', desc: 'Tải lên, quản lý phiên bản và xác nhận nội dung dùng cho AI.', to: '/cms/documents', roles: ADMIN_EDITOR },
      { icon: BookOutlined, title: 'Bài viết kiến thức', desc: 'Soạn & duyệt bài viết chuyên sâu cho bệnh nhân.', to: '/cms/articles', roles: CONTENT_TEAM },
      { icon: TagsOutlined, title: 'Chủ đề bài viết', desc: 'Quản lý chủ đề dùng chung cho bài viết và FAQ.', to: '/cms/article-topics', roles: CONTENT_TEAM },
      { icon: QuestionCircleOutlined, title: 'Câu hỏi thường gặp', desc: 'Soạn & duyệt FAQ hiển thị cho bệnh nhân.', to: '/cms/faq', roles: CONTENT_TEAM },
      { icon: MedicineBoxOutlined, title: 'Danh mục chuẩn hoá', desc: 'Danh mục triệu chứng/thuốc dùng làm dropdown.', to: '/cms/catalogs', roles: ADMIN_EDITOR },
      { icon: AlertOutlined, title: 'Từ khoá Triage', desc: 'Quản lý từ khoá sàng lọc dấu hiệu nguy hiểm.', to: '/cms/triage-keywords', roles: ADMIN_EDITOR },
    ],
  },
  {
    key: 'monitoring',
    title: 'Giám sát',
    items: [
      { icon: SafetyOutlined, title: 'Cảnh báo khẩn cấp', desc: 'Theo dõi và xác nhận escalate các ca Triage cần can thiệp.', to: '/cms/triage-alerts', roles: ADMIN_OPERATIONS },
      { icon: ExperimentOutlined, title: 'Đánh giá AI & Triage', desc: 'Chất lượng truy xuất RAG và độ nhạy sàng lọc an toàn.', to: '/cms/evaluation', roles: ADMIN },
      { icon: SoundOutlined, title: 'Thông báo broadcast', desc: 'Gửi thông báo chung tới toàn bộ/1 nhóm bệnh nhân.', to: '/cms/broadcast', roles: ADMIN },
      { icon: DashboardOutlined, title: 'Thống kê hệ thống', desc: 'Lượt chat, triệu chứng phổ biến, số lần Triage kích hoạt.', to: '/cms/dashboard', roles: ADMIN_OPERATIONS },
      { icon: AuditOutlined, title: 'Nhật ký hoạt động', desc: 'Theo dõi thao tác nhạy cảm để đảm bảo minh bạch.', to: '/cms/audit-logs', roles: ADMIN },
    ],
  },
  {
    key: 'config',
    title: 'Cấu hình',
    items: [
      { icon: SettingOutlined, title: 'Cấu hình RAG/Triage', desc: 'Tinh chỉnh tham số truy xuất và kênh cảnh báo.', to: '/cms/runtime-config', roles: ADMIN },
      { icon: MailOutlined, title: 'Mẫu email', desc: 'Chỉnh nội dung email xác thực, nhắc thuốc, cảnh báo.', to: '/cms/email-templates', roles: ADMIN },
    ],
  },
]

function canAccessItem(userType: CmsRole | undefined, item: CmsNavItem): boolean {
  return userType !== undefined && item.roles.includes(userType)
}

// Route con dùng quyền của mục cha: "/cms/articles/12/edit" thuộc mục "/cms/articles". So bằng hoặc
// có "/" ngay sau để "/cms/faq-x" không bị nhận nhầm là mục "/cms/faq".
function findCmsNavItem(path: string): CmsNavItem | undefined {
  return CMS_NAV_GROUPS.flatMap((group) => group.items)
    .find((item) => path === item.to || path.startsWith(`${item.to}/`))
}

/**
 * Nguồn sự thật DUY NHẤT cho quyền vào 1 màn CMS — dùng chung cho menu (filterCmsNavGroups),
 * route guard (CmsRoleRoute) và các chỗ ẩn/hiện theo quyền (Trang chủ, header, drawer cảnh báo),
 * để menu và route không bao giờ lệch nhau. Path không có trong CMS_NAV_GROUPS -> false: route
 * ngoài menu (/cms, /cms/notifications...) phải dùng CmsProtectedRoute, không dùng hàm này.
 */
export function canAccessCmsPath(userType: CmsRole | undefined, path: string): boolean {
  const item = findCmsNavItem(path)
  return item !== undefined && canAccessItem(userType, item)
}

export function isRoleRestrictedCmsPath(path: string): boolean {
  return findCmsNavItem(path) !== undefined
}

export function filterCmsNavGroups(userType: CmsRole | undefined): CmsNavGroup[] {
  return CMS_NAV_GROUPS.map((group) => ({
    ...group,
    items: group.items.filter((item) => canAccessItem(userType, item)),
  })).filter((group) => group.items.length > 0)
}
