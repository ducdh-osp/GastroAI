import type { ReactNode } from 'react'
import { CmsSidebar } from './CmsSidebar'

/** Khung chung cho các trang CMS sau khi đăng nhập: sidebar cố định + vùng nội dung. */
export function CmsAppShell({ children }: { children: ReactNode }) {
  return (
    <div className="flex h-svh overflow-hidden bg-[#f1f5f9]">
      <CmsSidebar />
      <main className="min-w-0 flex-1 overflow-y-auto overflow-x-auto p-6 sm:p-8">{children}</main>
    </div>
  )
}
