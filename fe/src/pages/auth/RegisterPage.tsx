import { Alert, Button, Card, Form, Input, Typography } from 'antd'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { register as registerApi } from '../../api/auth'

const { Title, Text } = Typography

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
    <div className="min-h-svh flex items-center justify-center bg-gray-50 px-4">
      <Card className="w-full max-w-md">
        <Title level={3} className="text-center!">Đăng ký tài khoản</Title>
        <Text type="secondary">Sau khi đăng ký, hãy mở email để xác thực tài khoản.</Text>
        {error && <Alert type="error" message={error} showIcon className="my-4" />}
        {success && <Alert type="success" message={success} showIcon className="my-4" />}
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
        <div className="text-center mt-4"><Text type="secondary">Đã có tài khoản? </Text><Link to="/login">Đăng nhập</Link></div>
      </Card>
    </div>
  )
}