import { KeyOutlined } from '@ant-design/icons'
import { Alert, Button, Card, Form, Input, Typography } from 'antd'
import { useState } from 'react'
import { changeCmsPassword } from '../../api/cmsAuth'
import { CmsAppShell } from '../../components/cms/CmsAppShell'
import { PASSWORD_REQUIREMENTS_MESSAGE, isAtMostUtf8Bytes, meetsPasswordRequirements } from '../../lib/validation'

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

const CURRENT_PASSWORD_WRONG = 'Mật khẩu hiện tại không đúng'

const utf8Rule = {
  validator: (_: unknown, value: string) =>
    !value || isAtMostUtf8Bytes(value, 72) ? Promise.resolve() : Promise.reject(new Error('Mật khẩu tối đa 72 byte UTF-8')),
}

export default function CmsChangePasswordPage() {
  const [form] = Form.useForm<ChangePasswordFormValues>()
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  async function onFinish(values: ChangePasswordFormValues) {
    setError(null)
    setSuccess(null)
    setLoading(true)
    try {
      await changeCmsPassword({ currentPassword: values.currentPassword, newPassword: values.newPassword })
      // Phien hien tai van dang nhap; chi cac phien o thiet bi khac bi dang xuat.
      setSuccess('Đổi mật khẩu thành công. Các thiết bị khác đã được đăng xuất.')
      form.resetFields()
    } catch (err: unknown) {
      const message = errorMessageOf(err)
      // Sai mat khau hien tai: bao ngay duoi o do, giu nguyen mat khau moi da go.
      if (message === CURRENT_PASSWORD_WRONG) {
        form.setFields([{ name: 'currentPassword', errors: [message] }])
      } else {
        setError(message)
      }
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
          <Form<ChangePasswordFormValues> form={form} layout="vertical" onFinish={onFinish} disabled={loading}>
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
              extra={PASSWORD_REQUIREMENTS_MESSAGE}
              dependencies={['currentPassword']}
              rules={[
                { required: true, message: 'Nhập mật khẩu mới' },
                {
                  validator: (_, value: string) =>
                    !value || meetsPasswordRequirements(value)
                      ? Promise.resolve()
                      : Promise.reject(new Error(PASSWORD_REQUIREMENTS_MESSAGE)),
                },
                { max: 72, message: 'Mật khẩu tối đa 72 ký tự' },
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