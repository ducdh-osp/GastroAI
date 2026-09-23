interface LogoProps {
  variant?: 'light' | 'dark'
  className?: string
}

/** Logo chữ GastroAI — không cần file ảnh riêng, tự vẽ bằng SVG nên luôn nét ở mọi kích thước. */
export function Logo({ variant = 'dark', className }: LogoProps) {
  const ink = variant === 'light' ? '#fcfefe' : '#0f2926'

  return (
    <span className={`inline-flex items-center gap-2 ${className ?? ''}`}>
      <svg width="30" height="30" viewBox="0 0 30 30" fill="none" aria-hidden>
        <rect width="30" height="30" rx="9" fill={variant === 'light' ? 'rgba(255,255,255,0.14)' : '#0d9488'} />
        <path
          d="M9 20c0-4.5 2.8-6.5 2.8-10.2 0-1.4-.5-2.4-.5-2.4s3.9.6 4.7 4.4c.5-1 .6-2.6.3-3.8 0 0 4.7 2.3 4.7 8 0 4.6-3.4 7.4-6.9 7.4-2.9 0-5.1-1.5-5.1-3.4Z"
          fill={variant === 'light' ? '#fcfefe' : '#ecfdf9'}
        />
      </svg>
      <span className="font-display text-lg font-bold tracking-tight" style={{ color: ink }}>
        Gastro<span style={{ color: variant === 'light' ? '#f0b429' : '#0d9488' }}>AI</span>
      </span>
    </span>
  )
}
