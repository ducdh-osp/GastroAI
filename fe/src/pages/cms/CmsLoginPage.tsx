import { Alert, Button, Card, Form, Input, Typography } from 'antd'
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { cmsLogin } from '../../api/cmsAuth'
import { useCmsAuthStore } from '../../stores/cmsAuthStore'

const { Title, Text } = Typography

interface LoginFormValues {
  email: string
  password: string
}

export default function CmsLoginPage() {
  const setUser = useCmsAuthStore((state) => state.setUser)
  const navigate = useNavigate()

  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  async function onFinish(values: LoginFormValues) {
    setError(null)
    setLoading(true)

    try {
      const user = await cmsLogin(values)

      setUser(user)

      navigate('/cms')
    } catch (err: unknown) {
      const message =
        (err as { response?: { data?: { message?: string } } })?.response
          ?.data?.message ?? 'Đăng nhập thất bại, thử lại sau'

      setError(message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-svh flex items-center justify-center bg-gray-50">
      <Card className="w-full max-w-sm">
        <Title level={3} className="text-center! mb-1!">
          Đăng nhập Gastro AI
        </Title>

        <div className="text-center mb-6">
          <Text type="secondary">
            Cổng quản trị dành cho Admin và Bác sĩ
          </Text>
        </div>

        {error && (
          <Alert
            type="error"
            message={error}
            showIcon
            className="mb-4"
          />
        )}

        <Form<LoginFormValues>
          layout="vertical"
          onFinish={onFinish}
          disabled={loading}
        >
          <Form.Item
            label="Email"
            name="email"
            rules={[
              { required: true, message: 'Nhập email' },
              { type: 'email', message: 'Email không hợp lệ' },
            ]}
          >
            <Input placeholder="admin@gastroai.vn" />
          </Form.Item>

          <Form.Item
            label="Mật khẩu"
            name="password"
            rules={[
              { required: true, message: 'Nhập mật khẩu' },
            ]}
          >
            <Input.Password />
          </Form.Item>

          <Form.Item>
            <Button
              type="primary"
              htmlType="submit"
              block
              loading={loading}
            >
              Đăng nhập
            </Button>
          </Form.Item>
        </Form>
      </Card>
    </div>
  )
}

