import { Alert, Button, Form, Input, Typography } from 'antd'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { register as registerApi } from '../../api/auth'
import { AuthShell } from '../../components/auth/AuthShell'

const { Text } = Typography

interface RegisterFormValues {
  fullName: string
  email: string
  password: string
  confirmPassword: string
}

export default function RegisterPage() {
  const navigate = useNavigate()
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  async function onFinish(values: RegisterFormValues) {
    setError(null)
    setSuccess(null)
    if (values.password !== values.confirmPassword) {
      setError('Mật khẩu xác nhận không khớp.')
      return
    }

    setLoading(true)
    try {
      const result = await registerApi(values)
      setSuccess(result.message || 'Đăng ký thành công. Vui lòng xác thực email.')
      setTimeout(() => navigate('/login'), 1800)
    } catch (err: unknown) {
      setError(
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ??
          'Đăng ký thất bại, vui lòng thử lại.',
      )
    } finally {
      setLoading(false)
    }
  }

  return (
    <AuthShell
      title="Đăng ký tài khoản"
      subtitle="Sau khi đăng ký, hãy mở email để xác thực tài khoản."
      eyebrow="Bắt đầu miễn phí"
      heroTitle="Tham gia GastroAI ngay hôm nay"
      heroDescription="Tạo tài khoản để bắt đầu ghi nhật ký sức khỏe tiêu hóa và trò chuyện cùng trợ lý AI."
    >
      {error && <Alert type="error" message={error} showIcon className="mb-4" />}
      {success && <Alert type="success" message={success} showIcon className="mb-4" />}
      <Form<RegisterFormValues> layout="vertical" onFinish={onFinish} disabled={loading}>
        <Form.Item label="Họ và tên" name="fullName" rules={[{ required: true, message: 'Nhập họ và tên' }]}>
          <Input placeholder="Nguyễn Văn A" />
        </Form.Item>
        <Form.Item label="Email" name="email" rules={[{ required: true, message: 'Nhập email' }, { type: 'email', message: 'Email không hợp lệ' }]}>
          <Input placeholder="ban@example.com" />
        </Form.Item>
        <Form.Item label="Mật khẩu" name="password" rules={[{ required: true, message: 'Nhập mật khẩu' }, { min: 8, message: 'Mật khẩu tối thiểu 8 ký tự' }]}>
          <Input.Password />
        </Form.Item>
        <Form.Item label="Xác nhận mật khẩu" name="confirmPassword" rules={[{ required: true, message: 'Nhập lại mật khẩu' }]}>
          <Input.Password />
        </Form.Item>
        <Button type="primary" htmlType="submit" block loading={loading}>Đăng ký</Button>
      </Form>
      <div className="text-center mt-4"><Text type="secondary">Đã có tài khoản? </Text><Link to="/login" className="text-teal-700 hover:text-teal-800">Đăng nhập</Link></div>
    </AuthShell>
  )
}