import {
  Alert,
  Button,
  Card,
  Form,
  Input,
  Radio,
  Typography,
} from 'antd'
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  cmsLogin,
  type CmsRole,
} from '../../api/cmsAuth'
import { useCmsAuth } from '../../stores/cmsAuthStore'

const { Title, Text } = Typography

interface LoginFormValues {
  email: string
  password: string
  role: CmsRole
}

interface LoginErrorResponse {
  message?: string
  lockedUntil?: string
}

export default function CmsLoginPage() {
  const { setUser } = useCmsAuth()
  const navigate = useNavigate()

  const [error, setError] = useState<string | null>(null)
  const [lockedUntil, setLockedUntil] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  async function onFinish(values: LoginFormValues) {
    setError(null)
    setLockedUntil(null)
    setLoading(true)

    try {
      const user = await cmsLogin(values)

      setUser(user)

      navigate('/cms')
    } catch (err: unknown) {
      const response = (
        err as {
          response?: {
            data?: LoginErrorResponse
          }
        }
      )?.response?.data

      setError(
        response?.message ?? 'Đăng nhập thất bại, thử lại sau'
      )

      if (response?.lockedUntil) {
        setLockedUntil(response.lockedUntil)
      }
    } finally {
      setLoading(false)
    }
  }

  function formatLockedUntil(value: string) {
    const date = new Date(value)

    return date.toLocaleString('vi-VN', {
      dateStyle: 'medium',
      timeStyle: 'short',
    })
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
            type={lockedUntil ? 'warning' : 'error'}
            message={error}
            description={
              lockedUntil
                ? `Tài khoản bị khóa đến ${formatLockedUntil(lockedUntil)}.`
                : undefined
            }
            showIcon
            className="mb-4"
          />
        )}

        <Form<LoginFormValues>
          layout="vertical"
          onFinish={onFinish}
          disabled={loading}
          initialValues={{
            role: 'ADMIN',
          }}
        >
          <Form.Item
            label="Vai trò"
            name="role"
            rules={[
              {
                required: true,
                message: 'Chọn vai trò',
              },
            ]}
          >
            <Radio.Group>
              <Radio value="ADMIN">Admin</Radio>
              <Radio value="DOCTOR">Bác sĩ</Radio>
            </Radio.Group>
          </Form.Item>

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
              {
                required: true,
                message: 'Nhập mật khẩu',
              },
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

