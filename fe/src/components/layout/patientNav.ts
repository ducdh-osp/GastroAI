import {
  BarChartOutlined,
  BulbOutlined,
  CoffeeOutlined,
  CommentOutlined,
  ExperimentOutlined,
  FileSearchOutlined,
  FileTextOutlined,
  LineChartOutlined,
  MedicineBoxOutlined,
  QuestionCircleOutlined,
  SolutionOutlined,
} from '@ant-design/icons'
import type { ComponentType } from 'react'

export interface PatientNavItem {
  icon: ComponentType<{ className?: string }>
  title: string
  desc: string
  to: string
}

export interface PatientNavGroup {
  key: string
  title: string
  accent: string
  items: PatientNavItem[]
}

// Dùng chung cho AppHeader (dropdown điều hướng) và HomePage (lưới ô Trang chủ) — xem
// 00-General/02-NguoiDung-BenhNhan/SCR-PT-00-TrangChu.md. Không có mục "Nhận diện món ăn" vì
// UC0044 đã cắt khỏi phạm vi đồ án (2026-11-19).
export const PATIENT_NAV_GROUPS: PatientNavGroup[] = [
  {
    key: 'chat',
    title: 'Trò chuyện cùng AI',
    accent: 'bg-teal-50 text-teal-600 dark:bg-teal-500/10 dark:text-teal-300',
    items: [
      { icon: CommentOutlined, title: 'Trò chuyện với AI', desc: 'Hỏi đáp trực tiếp, có trích dẫn nguồn y khoa và xem lại lịch sử ngay trong màn chat.', to: '/chat' },
      { icon: SolutionOutlined, title: 'Kiểm tra triệu chứng', desc: 'Trả lời từng bước để AI thu thập đủ thông tin trước khi gợi ý mức độ chăm sóc.', to: '/symptom-assessment' },
    ],
  },
  {
    key: 'daily',
    title: 'Ghi nhận hằng ngày',
    accent: 'bg-amber-50 text-amber-600 dark:bg-amber-500/10 dark:text-amber-300',
    items: [
      { icon: CoffeeOutlined, title: 'Nhật ký ăn uống', desc: 'Ghi lại bữa ăn và triệu chứng đi kèm (nếu có).', to: '/food-diary' },
      { icon: ExperimentOutlined, title: 'Nhật ký Bristol', desc: 'Ghi nhận tình trạng tiêu hoá theo thang Bristol chuẩn.', to: '/bristol' },
      { icon: MedicineBoxOutlined, title: 'Nhắc uống thuốc', desc: 'Thiết lập lịch nhắc và xác nhận đã uống.', to: '/medications' },
    ],
  },
  {
    key: 'review',
    title: 'Xem lại & phân tích',
    accent: 'bg-sky-50 text-sky-600 dark:bg-sky-500/10 dark:text-sky-300',
    items: [
      { icon: BarChartOutlined, title: 'Lịch sử ăn uống', desc: 'Biểu đồ và danh sách nhật ký ăn uống theo thời gian.', to: '/food-diary-history' },
      { icon: LineChartOutlined, title: 'Xu hướng Bristol', desc: 'Theo dõi xu hướng tiêu hoá, phát hiện bất thường kéo dài.', to: '/bristol-history' },
      { icon: BulbOutlined, title: 'Nhạy cảm thực phẩm', desc: 'AI tự phát hiện món ăn hay đi kèm triệu chứng của riêng bạn.', to: '/food-sensitivity' },
    ],
  },
  {
    key: 'profile',
    title: 'Hồ sơ & kiến thức',
    accent: 'bg-violet-50 text-violet-600 dark:bg-violet-500/10 dark:text-violet-300',
    items: [
      { icon: FileTextOutlined, title: 'Hồ sơ bệnh lý', desc: 'Tiền sử bệnh, dị ứng, thuốc đang dùng và liên hệ khẩn cấp.', to: '/medical-profile' },
      { icon: QuestionCircleOutlined, title: 'Hỏi đáp thường gặp', desc: 'Tra nhanh câu trả lời cho các câu hỏi phổ biến.', to: '/faq' },
      { icon: FileSearchOutlined, title: 'Bài viết tiêu hoá', desc: 'Thư viện kiến thức chuyên sâu về bệnh lý tiêu hoá.', to: '/articles' },
    ],
  },
]
