import type { ReactNode } from 'react'
import { AppSidebar } from './AppSidebar'

/** Khung chung cho các trang bệnh nhân sau khi đăng nhập: sidebar cố định + vùng nội dung. */
export function AppShell({ children }: { children: ReactNode }) {
  return (
    <div className="flex min-h-svh bg-[#f7faf9]">
      <AppSidebar />
      <main className="flex-1 overflow-x-auto p-6 sm:p-8">{children}</main>
    </div>
  )
}
