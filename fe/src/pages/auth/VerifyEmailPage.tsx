import { Alert, Button, Spin, Typography } from 'antd'
import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { verifyEmail } from '../../api/auth'
import { AuthShell } from '../../components/auth/AuthShell'

const { Text } = Typography

export default function VerifyEmailPage() {
  const [searchParams] = useSearchParams()
  const token = searchParams.get('token') ?? ''
  const [state, setState] = useState<'loading' | 'success' | 'error'>(token ? 'loading' : 'error')
  const [message, setMessage] = useState(token ? '' : 'Liên kết xác thực không có token.')

  useEffect(() => {
    if (!token) return
    verifyEmail(token)
      .then(() => {
        setState('success')
        setMessage('Email đã được xác thực. Bạn có thể đăng nhập ngay bây giờ.')
      })
      .catch((err: unknown) => {
        setState('error')
        setMessage(
          (err as { response?: { data?: { message?: string } } })?.response?.data?.message ??
            'Liên kết xác thực không hợp lệ hoặc đã hết hạn.',
        )
      })
  }, [token])

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
          <div className="mt-4">
            <Text type="secondary">Bạn có thể </Text>
            <Link to="/register" className="text-teal-700 hover:text-teal-800">đăng ký lại</Link>
          </div>
        )}
      </div>
    </AuthShell>
  )
}
