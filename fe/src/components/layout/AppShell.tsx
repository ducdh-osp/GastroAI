import type { ReactNode } from 'react'
import { AppHeader } from './AppHeader'

/** Khung chung cho các trang bệnh nhân sau khi đăng nhập: header cố định + vùng nội dung.
 * Không còn sidebar liệt kê sẵn mọi chức năng (xem 00-General/02-NguoiDung-BenhNhan/
 * SCR-PT-00-TrangChu.md) — điều hướng giữa các chức năng đi qua Trang chủ (HomePage). */
export function AppShell({
  children,
  fixedViewport = false,
  noPadding = false,
}: {
  children: ReactNode
  fixedViewport?: boolean
  noPadding?: boolean
}) {
  return (
    <div className="flex h-svh flex-col overflow-hidden bg-[#f7faf9] dark:bg-[#0a0a0a]">
      <AppHeader />
      <main
        className={`min-h-0 min-w-0 flex-1 ${noPadding ? '' : 'p-6 sm:p-8'} ${
          fixedViewport ? 'overflow-hidden' : 'overflow-y-auto overflow-x-auto overscroll-contain'
        }`}
      >
        {children}
      </main>
    </div>
  )
}
