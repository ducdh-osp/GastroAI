import { HeartOutlined, ReadOutlined, SafetyCertificateOutlined } from '@ant-design/icons'
import { Alert, Button, Form, Input } from 'antd'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { resendVerification } from '../../api/auth'
import { AuthShell } from '../../components/auth/AuthShell'
import { useAuth } from '../../stores/authStore'
import { isAtMostUtf8Bytes } from '../../lib/validation'

interface LoginFormValues {
  email: string
  password: string
}

const TRUST_ITEMS = [
  { icon: SafetyCertificateOutlined, text: 'Dữ liệu sức khỏe được mã hóa và bảo mật' },
  { icon: ReadOutlined, text: 'Trả lời dựa trên tài liệu y khoa có nguồn gốc rõ ràng' },
  { icon: HeartOutlined, text: 'Luôn nhắc bạn gặp bác sĩ khi có dấu hiệu bất thường' },
]

// Message ASCII thật do BE trả (xem AuthService.java) → tiếng Việt có dấu đầy đủ. Riêng
// tài khoản bị khóa không nằm ở đây — message đó đã có dấu sẵn từ AccountLockedException,
// hiển thị kèm lockedUntil riêng bên dưới thay vì map cứng.
const BE_MESSAGE_MAP: Record<string, string> = {
  'Email hoac mat khau khong dung': 'Email hoặc mật khẩu không đúng.',
  'Email chua duoc xac thuc': 'Email chưa được xác thực. Vui lòng kiểm tra hộp thư.',
}

function formatError(msg: string): string {
  const mapped = BE_MESSAGE_MAP[msg.trim()]
  if (mapped) return mapped
  // Nếu không có trong map, chỉ đảm bảo có dấu chấm cuối
  return msg.trim().endsWith('.') || msg.trim().endsWith('!') || msg.trim().endsWith('?')
    ? msg.trim()
    : msg.trim() + '.'
}

function formatLockedUntil(value: string): string {
  return new Date(value).toLocaleString('vi-VN', { dateStyle: 'medium', timeStyle: 'short' })
}

export default function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [error, setError] = useState<string | null>(null)
  const [lockedUntil, setLockedUntil] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)
  const [unverifiedEmail, setUnverifiedEmail] = useState<string | null>(null)
  const [resending, setResending] = useState(false)
  const [resendNotice, setResendNotice] = useState<string | null>(null)
  const [resendError, setResendError] = useState<string | null>(null)

  async function onFinish(values: LoginFormValues) {
    setError(null)
    setLockedUntil(null)
    setUnverifiedEmail(null)
    setResendNotice(null)
    setResendError(null)
    setLoading(true)
    try {
      await login(values)
      navigate('/')
    } catch (err: unknown) {
      const data = (err as { response?: { data?: { message?: string; lockedUntil?: string } } })?.response?.data
      setError(formatError(data?.message ?? 'Đăng nhập thất bại, thử lại sau.'))
      setLockedUntil(data?.lockedUntil ?? null)
      if (data?.message?.trim() === 'Email chua duoc xac thuc') setUnverifiedEmail(values.email.trim())
    } finally {
      setLoading(false)
    }
  }

  async function onResendVerification() {
    if (!unverifiedEmail) return
    setResending(true)
    setResendNotice(null)
    setResendError(null)
    try {
      await resendVerification(unverifiedEmail)
      setResendNotice('Nếu email tồn tại và chưa được xác thực, hệ thống đã gửi lại link xác thực.')
    } catch {
      setResendError('Không thể gửi lại email lúc này. Vui lòng thử lại sau.')
    } finally {
      setResending(false)
    }
  }

  return (
    <AuthShell
      title="Đăng nhập tài khoản"
      subtitle="Tiếp tục hành trình chăm sóc tiêu hóa cùng GastroAI."
      trustItems={TRUST_ITEMS}
    >
      {error && (
        <Alert
          type={lockedUntil ? 'warning' : 'error'}
          message={error}
          description={lockedUntil ? `Tài khoản bị khóa đến ${formatLockedUntil(lockedUntil)}.` : undefined}
          showIcon
          className="mb-4"
        />
      )}
      {unverifiedEmail && (
        <div className="mb-4 space-y-3">
          <Button block loading={resending} onClick={() => void onResendVerification()}>
            Gửi lại email xác thực
          </Button>
          {resendNotice && <Alert type="info" showIcon message={resendNotice} />}
          {resendError && <Alert type="error" showIcon message={resendError} />}
        </div>
      )}
      <Form<LoginFormValues> layout="vertical" onFinish={onFinish} disabled={loading}>
        <Form.Item
          label="Email"
          name="email"
          rules={[
            { required: true, message: 'Nhập email' },
            { type: 'email', message: 'Email không hợp lệ' },
            { max: 255, message: 'Email tối đa 255 ký tự' },
          ]}
        >
          <Input maxLength={255} placeholder="ban@example.com" />
        </Form.Item>
        <Form.Item
          label="Mật khẩu"
          name="password"
          rules={[{ required: true, message: 'Nhập mật khẩu' }, { max: 72, message: 'Mật khẩu tối đa 72 ký tự' }, { validator: (_, value: string) => !value || isAtMostUtf8Bytes(value, 72) ? Promise.resolve() : Promise.reject(new Error('Mật khẩu tối đa 72 byte UTF-8')) }]}
        >
          <Input.Password maxLength={72} />
        </Form.Item>
        <Form.Item>
          <Button type="primary" htmlType="submit" block loading={loading}>
            Đăng nhập
          </Button>
        </Form.Item>
      </Form>
      <div className="flex justify-between text-sm">
        <Link to="/register" className="text-teal-700 hover:text-teal-800">Đăng ký tài khoản</Link>
        <Link to="/forgot-password" className="text-teal-700 hover:text-teal-800">Quên mật khẩu?</Link>
      </div>
    </AuthShell>
  )
}
