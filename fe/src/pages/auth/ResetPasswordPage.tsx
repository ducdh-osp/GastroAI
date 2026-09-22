import { Alert, Button, Card, Form, Input, Typography } from 'antd'
import { useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { resetPassword } from '../../api/auth'

const { Title, Text } = Typography

interface ResetPasswordFormValues {
  newPassword: string
  confirmPassword: string
}

export default function ResetPasswordPage() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const token = searchParams.get('token') ?? ''
  const [error, setError] = useState<string | null>(token ? null : 'Liên kết đặt lại mật khẩu không có token.')
  const [success, setSuccess] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  async function onFinish(values: ResetPasswordFormValues) {
    setError(null)
    setSuccess(null)
    if (!token) {
      setError('Liên kết đặt lại mật khẩu không hợp lệ.')
      return
    }
    if (values.newPassword !== values.confirmPassword) {
      setError('Mật khẩu xác nhận không khớp.')
      return
    }

    setLoading(true)
    try {
      await resetPassword({ token, newPassword: values.newPassword })
      setSuccess('Đổi mật khẩu thành công. Đang chuyển về trang đăng nhập...')
      setTimeout(() => navigate('/login'), 1500)
    } catch (err: unknown) {
      setError(
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ??
          'Token không hợp lệ hoặc đã hết hạn.',
      )
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-svh flex items-center justify-center bg-gray-50 px-4">
      <Card className="w-full max-w-md">
        <Title level={3} className="text-center!">Đặt lại mật khẩu</Title>
        <Text type="secondary">Liên kết email đã xác thực. Hãy tạo mật khẩu mới rồi đăng nhập lại.</Text>
        {error && <Alert type="error" message={error} showIcon className="my-4" />}
        {success && <Alert type="success" message={success} showIcon className="my-4" />}
        <Form<ResetPasswordFormValues> layout="vertical" onFinish={onFinish} disabled={loading} className="mt-4">
          <Form.Item label="Mật khẩu mới" name="newPassword" rules={[{ required: true, message: 'Nhập mật khẩu mới' }, { min: 8, message: 'Mật khẩu tối thiểu 8 ký tự' }]}>
            <Input.Password />
          </Form.Item>
          <Form.Item label="Xác nhận mật khẩu mới" name="confirmPassword" rules={[{ required: true, message: 'Nhập lại mật khẩu mới' }]}>
            <Input.Password />
          </Form.Item>
          <Button type="primary" htmlType="submit" block loading={loading}>Đổi mật khẩu</Button>
        </Form>
        <div className="text-center mt-4"><Link to="/login">Quay lại đăng nhập</Link></div>
      </Card>
    </div>
  )
}