import type { ReactNode } from 'react'
import { CmsHeader } from './CmsHeader'

/** Khung chung cho các trang CMS sau khi đăng nhập: header cố định + vùng nội dung. Không còn
 * sidebar (xem 00-General/04-QuanTri-AdminCMS/SCR-AD-00-TrangChu.md) — điều hướng đi qua Trang
 * chủ CMS (CmsHomePage). */
export function CmsAppShell({ children }: { children: ReactNode }) {
  return (
    <div className="flex h-svh flex-col overflow-hidden bg-[#f1f5f9] dark:bg-[#0a0a0a]">
      <CmsHeader />
      <main className="min-h-0 min-w-0 flex-1 overflow-y-auto overflow-x-auto p-6 sm:p-8">{children}</main>
    </div>
  )
}
