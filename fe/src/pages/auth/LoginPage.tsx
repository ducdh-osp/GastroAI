import { Alert, Button, Card, Form, Input, Typography } from 'antd'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../../stores/authStore'

const { Title } = Typography

interface LoginFormValues {
  email: string
  password: string
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
      const message =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ??
        'Đăng nhập thất bại, thử lại sau'
      setError(message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-svh flex items-center justify-center bg-gray-50">
      <Card className="w-full max-w-sm">
        <Title level={3} className="text-center!">
          Đăng nhập Gastro AI
        </Title>
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
          <Link to="/register">Đăng ký tài khoản</Link>
          <Link to="/forgot-password">Quên mật khẩu?</Link>
        </div>
      </Card>
    </div>
  )
}
