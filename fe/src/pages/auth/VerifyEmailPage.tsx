import { Alert, Button, Input, Spin, Typography } from 'antd'
import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { resendVerification, verifyEmailOnce } from '../../api/auth'
import { AuthShell } from '../../components/auth/AuthShell'

const { Text } = Typography

export default function VerifyEmailPage() {
  const [searchParams] = useSearchParams()
  const token = searchParams.get('token') ?? ''
  const [state, setState] = useState<'loading' | 'success' | 'error'>(token ? 'loading' : 'error')
  const [message, setMessage] = useState(token ? '' : 'Liên kết xác thực không có token.')
  const [email, setEmail] = useState('')
  const [resending, setResending] = useState(false)
  const [resendNotice, setResendNotice] = useState<string | null>(null)
  const [resendError, setResendError] = useState<string | null>(null)

  useEffect(() => {
    if (!token) return
    let active = true
    verifyEmailOnce(token)
      .then(() => {
        if (active) {
          setState('success')
          setMessage('Email đã được xác thực. Bạn có thể đăng nhập ngay bây giờ.')
        }
      })
      .catch((err: unknown) => {
        if (active) {
          setState('error')
          setMessage(
            (err as { response?: { data?: { message?: string } } })?.response?.data?.message ??
              'Liên kết xác thực không hợp lệ hoặc đã hết hạn.',
          )
        }
      })
    return () => { active = false }
  }, [token])

  async function onResendVerification() {
    const normalizedEmail = email.trim()
    if (!normalizedEmail) {
      setResendError('Vui lòng nhập email đã đăng ký.')
      return
    }
    setResending(true)
    setResendNotice(null)
    setResendError(null)
    try {
      await resendVerification(normalizedEmail)
      setResendNotice('Nếu email tồn tại và chưa được xác thực, hệ thống đã gửi lại link xác thực.')
    } catch {
      setResendError('Không thể gửi lại email lúc này. Vui lòng kiểm tra email và thử lại.')
    } finally {
      setResending(false)
    }
  }

  return (
    <AuthShell
      title="Xác thực email"
      eyebrow="Bước cuối cùng"
      heroTitle="Gần hoàn tất rồi"
      heroDescription="Xác thực email để kích hoạt tài khoản và bắt đầu sử dụng đầy đủ tính năng của GastroAI."
    >
      <div className="text-center">
        {state === 'loading' && <Spin />}
        {state !== 'loading' && (
          <Alert type={state === 'success' ? 'success' : 'error'} message={message} showIcon />
        )}
        {state === 'success' && (
          <Button type="primary" className="mt-4" href="/login">Đến trang đăng nhập</Button>
        )}
        {state === 'error' && (
          <div className="mt-4 space-y-3 text-left">
            <Text type="secondary">Nhập email đã đăng ký để yêu cầu gửi lại link xác thực.</Text>
            <Input
              type="email"
              autoComplete="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              placeholder="ban@example.com"
              aria-label="Email đã đăng ký"
            />
            <Button type="primary" block loading={resending} onClick={() => void onResendVerification()}>
              Gửi lại email xác thực
            </Button>
            {resendNotice && <Alert type="info" showIcon message={resendNotice} />}
            {resendError && <Alert type="error" showIcon message={resendError} />}
            <div className="text-center">
              <Text type="secondary">Hoặc </Text>
              <Link to="/login" className="text-teal-700 hover:text-teal-800">quay lại đăng nhập</Link>
            </div>
          </div>
        )}
      </div>
    </AuthShell>
  )
}
