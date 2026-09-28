import type { ReactNode } from 'react'
import { AppSidebar } from './AppSidebar'

/** Khung chung cho các trang bệnh nhân sau khi đăng nhập: sidebar cố định + vùng nội dung. */
export function AppShell({ children, fixedViewport = false }: { children: ReactNode; fixedViewport?: boolean }) {
  return (
    <div className={`flex bg-[#f7faf9] ${fixedViewport ? 'h-svh overflow-hidden' : 'min-h-svh'}`}>
      <AppSidebar />
      <main className={`min-w-0 flex-1 p-6 sm:p-8 ${fixedViewport ? 'min-h-0 overflow-hidden' : 'overflow-x-auto'}`}>
        {children}
      </main>
    </div>
  )
}
