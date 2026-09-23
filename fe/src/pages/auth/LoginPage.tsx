import { HeartOutlined, ReadOutlined, SafetyCertificateOutlined } from '@ant-design/icons'
import { Alert, Button, Form, Input } from 'antd'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { AuthShell } from '../../components/auth/AuthShell'
import { useAuth } from '../../stores/authStore'

interface LoginFormValues {
  email: string
  password: string
}

const TRUST_ITEMS = [
  { icon: SafetyCertificateOutlined, text: 'Dữ liệu sức khỏe được mã hóa và bảo mật' },
  { icon: ReadOutlined, text: 'Trả lời dựa trên tài liệu y khoa có nguồn gốc rõ ràng' },
  { icon: HeartOutlined, text: 'Luôn nhắc bạn gặp bác sĩ khi có dấu hiệu bất thường' },
]

/** Map message ASCII từ BE → tiếng Việt có dấu đầy đủ + dấu chấm cuối câu */
const BE_MESSAGE_MAP: Record<string, string> = {
  'Email hoac mat khau khong dung': 'Email hoặc mật khẩu không đúng.',
  'Email chua duoc xac thuc': 'Email chưa được xác thực. Vui lòng kiểm tra hộp thư.',
  'Tai khoan bi khoa': 'Tài khoản đã bị khóa tạm thời do đăng nhập sai nhiều lần.',
}

function formatError(msg: string): string {
  const mapped = BE_MESSAGE_MAP[msg.trim()]
  if (mapped) return mapped
  // Nếu không có trong map, chỉ đảm bảo có dấu chấm cuối
  return msg.trim().endsWith('.') || msg.trim().endsWith('!') || msg.trim().endsWith('?')
    ? msg.trim()
    : msg.trim() + '.'
}

export default function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  async function onFinish(values: LoginFormValues) {
    setError(null)
    setLoading(true)
    try {
      await login(values)
      navigate('/')
    } catch (err: unknown) {
      const raw =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ??
        'Đăng nhập thất bại, thử lại sau.'
      setError(formatError(raw))
    } finally {
      setLoading(false)
    }
  }

  return (
    <AuthShell
      title="Đăng nhập tài khoản"
      subtitle="Tiếp tục hành trình chăm sóc tiêu hóa cùng GastroAI."
      trustItems={TRUST_ITEMS}
    >
      {error && <Alert type="error" message={error} showIcon className="mb-4" />}
      <Form<LoginFormValues> layout="vertical" onFinish={onFinish} disabled={loading}>
        <Form.Item
          label="Email"
          name="email"
          rules={[
            { required: true, message: 'Nhập email' },
            { type: 'email', message: 'Email không hợp lệ' },
          ]}
        >
          <Input placeholder="ban@example.com" />
        </Form.Item>
        <Form.Item
          label="Mật khẩu"
          name="password"
          rules={[{ required: true, message: 'Nhập mật khẩu' }]}
        >
          <Input.Password />
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
