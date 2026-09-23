import type { ReactNode } from 'react'
import { AppHeader } from './AppHeader'

/** Khung chung cho các trang bệnh nhân sau khi đăng nhập: thanh nav + vùng nội dung. */
export function AppShell({ children }: { children: ReactNode }) {
  return (
    <div className="min-h-svh bg-[#f7faf9]">
      <AppHeader />
      <main className="mx-auto max-w-5xl px-4 py-8 sm:px-6 sm:py-10">{children}</main>
    </div>
  )
}
