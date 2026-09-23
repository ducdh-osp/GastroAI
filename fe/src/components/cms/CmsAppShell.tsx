import type { ReactNode } from 'react'
import { CmsSidebar } from './CmsSidebar'

/** Khung chung cho các trang CMS sau khi đăng nhập: sidebar cố định + vùng nội dung. */
export function CmsAppShell({ children }: { children: ReactNode }) {
  return (
    <div className="flex min-h-svh bg-[#f1f5f9]">
      <CmsSidebar />
      <main className="flex-1 overflow-x-auto p-6 sm:p-8">{children}</main>
    </div>
  )
}
