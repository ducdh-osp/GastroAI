import { KeyOutlined } from '@ant-design/icons'
import { Alert, Button, Card, Form, Input, Typography } from 'antd'
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { changeCmsPassword } from '../../api/cmsAuth'
import { CmsAppShell } from '../../components/cms/CmsAppShell'
import { isAtMostUtf8Bytes } from '../../lib/validation'
import { useCmsAuth } from '../../stores/cmsAuthStore'

const { Title, Text } = Typography

interface ChangePasswordFormValues {
  currentPassword: string
  newPassword: string
  confirmPassword: string
}

function errorMessageOf(err: unknown): string {
  const data = (err as { response?: { status?: number; data?: { message?: string; lockedUntil?: string } } })?.response
  if (data?.status === 423 && data.data?.lockedUntil) {
    const until = new Date(data.data.lockedUntil).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })
    return `Tài khoản tạm bị khóa do nhập sai mật khẩu nhiều lần. Vui lòng thử lại sau ${until}.`
  }
  return data?.data?.message ?? 'Không thể đổi mật khẩu.'
}

const utf8Rule = {
  validator: (_: unknown, value: string) =>
    !value || isAtMostUtf8Bytes(value, 72) ? Promise.resolve() : Promise.reject(new Error('Mật khẩu tối đa 72 byte UTF-8')),
}

export default function CmsChangePasswordPage() {
  const { clearUser } = useCmsAuth()
  const navigate = useNavigate()
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  async function onFinish(values: ChangePasswordFormValues) {
    setError(null)
    setSuccess(null)
    setLoading(true)
    try {
      await changeCmsPassword({ currentPassword: values.currentPassword, newPassword: values.newPassword })
      // BE da huy phien hien tai va moi phien khac -> xoa trang thai dang nhap phia FE roi ve trang login.
      setSuccess('Đổi mật khẩu thành công. Vui lòng đăng nhập lại...')
      setTimeout(() => {
        clearUser()
        navigate('/cms/login')
      }, 1500)
    } catch (err: unknown) {
      setError(errorMessageOf(err))
    } finally {
      setLoading(false)
    }
  }

  return (
    <CmsAppShell>
      <div className="mx-auto max-w-md">
        <div className="mb-6">
          <Text type="secondary">Bảo mật tài khoản</Text>
          <Title level={2} className="mb-1! mt-1!">
            <KeyOutlined className="mr-2" />
            Đổi mật khẩu
          </Title>
        </div>

        <Card className="rounded-2xl border-black/5 shadow-sm">
          {error && <Alert type="error" title={error} showIcon className="mb-4" />}
          {success && <Alert type="success" title={success} showIcon className="mb-4" />}
          <Form<ChangePasswordFormValues> layout="vertical" onFinish={onFinish} disabled={loading || success !== null}>
            <Form.Item
              label="Mật khẩu hiện tại"
              name="currentPassword"
              rules={[{ required: true, message: 'Nhập mật khẩu hiện tại' }, { max: 72, message: 'Mật khẩu tối đa 72 ký tự' }, utf8Rule]}
            >
              <Input.Password maxLength={72} autoComplete="current-password" />
            </Form.Item>
                        <Form.Item
              label="Mật khẩu mới"
              name="newPassword"
              dependencies={['currentPassword']}
              rules={[
                { required: true, message: 'Nhập mật khẩu mới' },
                { min: 8, message: 'Mật khẩu tối thiểu 8 ký tự' },
                { max: 72, message: 'Mật khẩu tối đa 72 ký tự' },
                { pattern: /[A-Z]/, message: 'Mật khẩu phải có ít nhất 1 chữ hoa' },
                { pattern: /[a-z]/, message: 'Mật khẩu phải có ít nhất 1 chữ thường' },
                { pattern: /[0-9]/, message: 'Mật khẩu phải có ít nhất 1 chữ số' },
                { pattern: /[^A-Za-z0-9]/, message: 'Mật khẩu phải có ít nhất 1 ký tự đặc biệt' },
                utf8Rule,
                ({ getFieldValue }) => ({
                  validator: (_, value: string) =>
                    !value || value !== getFieldValue('currentPassword')
                      ? Promise.resolve()
                      : Promise.reject(new Error('Mật khẩu mới phải khác mật khẩu hiện tại')),
                }),
              ]}
            >
              <Input.Password maxLength={72} autoComplete="new-password" />
            </Form.Item>
            <Form.Item
              label="Xác nhận mật khẩu mới"
              name="confirmPassword"
              dependencies={['newPassword']}
              rules={[
                { required: true, message: 'Nhập lại mật khẩu mới' },
                ({ getFieldValue }) => ({
                  validator: (_, value: string) =>
                    !value || value === getFieldValue('newPassword')
                      ? Promise.resolve()
                      : Promise.reject(new Error('Mật khẩu xác nhận không khớp')),
                }),
              ]}
            >
              <Input.Password maxLength={72} autoComplete="new-password" />
            </Form.Item>
            <Button type="primary" htmlType="submit" block loading={loading}>
              Đổi mật khẩu
            </Button>
          </Form>
        </Card>
      </div>
    </CmsAppShell>
  )
}