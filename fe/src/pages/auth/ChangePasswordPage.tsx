import { Alert, Button, Card, Form, Input, Typography } from 'antd'
import { useState } from 'react'
import { Link } from 'react-router-dom'
import { changePassword } from '../../api/auth'

const { Title, Text } = Typography

interface ChangePasswordFormValues {
  currentPassword: string
  newPassword: string
  confirmPassword: string
}

export default function ChangePasswordPage() {
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  async function onFinish(values: ChangePasswordFormValues) {
    setError(null)
    setSuccess(null)
    if (values.newPassword !== values.confirmPassword) {
      setError('Mật khẩu xác nhận không khớp.')
      return
    }

    setLoading(true)
    try {
      await changePassword({ currentPassword: values.currentPassword, newPassword: values.newPassword })
      setSuccess('Đổi mật khẩu thành công.')
    } catch (err: unknown) {
      setError(
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ??
          (err instanceof Error ? err.message : 'Không thể đổi mật khẩu.'),
      )
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-svh flex items-center justify-center bg-gray-50 px-4">
      <Card className="w-full max-w-md">
        <Title level={3} className="text-center!">Đổi mật khẩu</Title>
        <Text type="secondary">Nhập mật khẩu hiện tại và mật khẩu mới để bảo vệ tài khoản.</Text>
        {error && <Alert type="error" message={error} showIcon className="my-4" />}
        {success && <Alert type="success" message={success} showIcon className="my-4" />}
        <Form<ChangePasswordFormValues> layout="vertical" onFinish={onFinish} disabled={loading} className="mt-4">
          <Form.Item label="Mật khẩu hiện tại" name="currentPassword" rules={[{ required: true, message: 'Nhập mật khẩu hiện tại' }]}>
            <Input.Password autoComplete="current-password" />
          </Form.Item>
          <Form.Item label="Mật khẩu mới" name="newPassword" rules={[{ required: true, message: 'Nhập mật khẩu mới' }, { min: 8, message: 'Mật khẩu tối thiểu 8 ký tự' }]}>
            <Input.Password autoComplete="new-password" />
          </Form.Item>
          <Form.Item label="Xác nhận mật khẩu mới" name="confirmPassword" rules={[{ required: true, message: 'Nhập lại mật khẩu mới' }]}>
            <Input.Password autoComplete="new-password" />
          </Form.Item>
          <Button type="primary" htmlType="submit" block loading={loading}>Đổi mật khẩu</Button>
        </Form>
        <div className="text-center mt-4"><Link to="/">Quay lại trang chủ</Link></div>
      </Card>
    </div>
  )
}
