import type { ReactNode } from 'react'
import { AppSidebar } from './AppSidebar'

/** Khung chung cho các trang bệnh nhân sau khi đăng nhập: sidebar cố định + vùng nội dung. */
export function AppShell({ children, fixedViewport = false }: { children: ReactNode; fixedViewport?: boolean }) {
  return (
    <div className="flex h-svh overflow-hidden bg-[#f7faf9] dark:bg-[#0b1120]">
      <AppSidebar />
      <main className={`min-h-0 min-w-0 flex-1 p-6 sm:p-8 ${fixedViewport ? 'overflow-hidden' : 'overflow-y-auto overflow-x-auto overscroll-contain'}`}>
        {children}
      </main>
    </div>
  )
}
