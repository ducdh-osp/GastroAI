import type { ComponentType, CSSProperties, ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { Logo } from '../brand/Logo'

interface TrustItem {
  icon: ComponentType<{ className?: string; style?: CSSProperties }>
  text: string
}

interface AuthShellTone {
  gradient: string
  accent: string
  logoTo: string
}

const TONES: Record<'brand' | 'admin', AuthShellTone> = {
  // Cổng bệnh nhân — xanh ngọc, gần gũi.
  brand: {
    gradient: 'radial-gradient(120% 120% at 15% 0%, #14b8a6 0%, #0d9488 45%, #134e4a 100%)',
    accent: '#f0b429',
    logoTo: '/',
  },
  // Cổng quản trị (Admin/Bác sĩ) — xanh mực đậm hơn, tách biệt rõ với cổng bệnh nhân
  // để không ai nhầm 2 luồng đăng nhập với nhau.
  admin: {
    gradient: 'radial-gradient(120% 120% at 15% 0%, #334155 0%, #1e293b 45%, #0f172a 100%)',
    accent: '#2dd4bf',
    logoTo: '/cms/login',
  },
}

interface AuthShellProps {
  title: ReactNode
  subtitle?: ReactNode
  children: ReactNode
  footer?: ReactNode
  tone?: 'brand' | 'admin'
  eyebrow?: string
  heroTitle?: string
  heroDescription?: string
  trustItems?: TrustItem[]
}

/**
 * Khung dùng chung cho các trang xác thực công khai (đăng nhập/đăng ký/quên mật khẩu...):
 * panel trái giới thiệu thương hiệu trên nền gradient, panel phải là thẻ chứa form.
 * `tone="admin"` đổi sang bảng màu tối hơn cho cổng CMS — tách biệt trực quan khỏi cổng
 * bệnh nhân dù dùng chung 1 component, tránh trùng lặp code layout.
 * Ẩn panel trái trên màn hình nhỏ để form luôn full-width, dễ thao tác trên di động.
 */
export function AuthShell({
  title,
  subtitle,
  children,
  footer,
  tone = 'brand',
  eyebrow = 'Cơ sở dữ liệu công chứng y tế cá nhân',
  heroTitle = 'Nền tảng chăm sóc tiêu hóa cùng AI',
  heroDescription = 'Theo dõi triệu chứng, nhật ký ăn uống và trò chuyện với trợ lý AI được huấn luyện từ tài liệu y khoa chuyên ngành tiêu hóa.',
  trustItems,
}: AuthShellProps) {
  const { gradient, accent, logoTo } = TONES[tone]

  return (
    <div className="grid min-h-svh bg-[#f7faf9] lg:grid-cols-[minmax(0,1.05fr)_minmax(440px,0.9fr)]">
      {/* Panel thương hiệu */}
      <section
        className="relative hidden overflow-hidden text-white lg:flex lg:flex-col lg:justify-between"
        style={{ background: gradient }}
      >
        <div className="bg-brand-grid absolute inset-0 opacity-60" aria-hidden />
        <div className="grain-overlay absolute inset-0" aria-hidden />

        <div className="relative p-10">
          <Logo variant="light" />
        </div>

        <div className="relative p-10 pb-16">
          <p className="text-xs tracking-[0.35em] text-white/70 uppercase">{eyebrow}</p>
          <h1 className="mt-4 max-w-md font-display text-5xl leading-tight font-extrabold">{heroTitle}</h1>
          <p className="mt-4 max-w-sm text-base leading-relaxed text-white/80">{heroDescription}</p>

          {trustItems ? (
            <ul className="mt-9 space-y-3">
              {trustItems.map(({ icon: Icon, text }) => (
                <li key={text} className="flex items-center gap-3 text-sm text-white/90">
                  <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-white/10 ring-1 ring-white/20">
                    <Icon style={{ color: accent }} aria-hidden />
                  </span>
                  {text}
                </li>
              ))}
            </ul>
          ) : null}
        </div>
      </section>

      {/* Panel biểu mẫu */}
      <section className="relative flex min-h-svh items-center justify-center px-4 py-6 sm:px-8 sm:py-10 lg:px-10">
        <div className="w-full max-w-md space-y-6">
          <div className="flex items-center gap-3 lg:hidden">
            <Link to={logoTo}>
              <Logo />
            </Link>
          </div>

          <div className="space-y-2">
            <div className="rule-accent w-16" style={{ background: `linear-gradient(90deg, transparent, ${accent}, transparent)` }} />
            <h2 className="font-display text-2xl leading-tight font-bold text-[#0f2926] sm:text-3xl">{title}</h2>
            {subtitle ? <p className="text-sm leading-6 text-gray-500 sm:text-base">{subtitle}</p> : null}
          </div>

          <div className="rounded-2xl border border-black/5 bg-white/95 p-5 shadow-xl shadow-teal-900/5 backdrop-blur-sm sm:p-8">
            {children}
          </div>

          {footer ? <div className="text-center text-sm text-gray-500">{footer}</div> : null}
        </div>
      </section>
    </div>
  )
}
