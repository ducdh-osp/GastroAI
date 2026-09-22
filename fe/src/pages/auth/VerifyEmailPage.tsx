import { Alert, Button, Card, Spin, Typography } from 'antd'
import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { verifyEmail } from '../../api/auth'

const { Title, Text } = Typography

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
    <div className="min-h-svh flex items-center justify-center bg-gray-50 px-4">
      <Card className="w-full max-w-md text-center">
        <Title level={3}>Xác thực email</Title>
        {state === 'loading' && <Spin />}
        {state !== 'loading' && (
          <Alert type={state === 'success' ? 'success' : 'error'} message={message} showIcon />
        )}
        {state === 'success' && (
          <Button type="primary" className="mt-4" href="/login">Đến trang đăng nhập</Button>
        )}
        {state === 'error' && (
          <div className="mt-4"><Text type="secondary">Bạn có thể </Text><Link to="/register">đăng ký lại</Link></div>
        )}
      </Card>
    </div>
  )
}