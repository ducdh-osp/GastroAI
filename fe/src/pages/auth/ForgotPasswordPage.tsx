import { Alert, Button, Card, Form, Input, Typography } from 'antd'
import { useState } from 'react'
import { Link } from 'react-router-dom'
import { requestPasswordReset } from '../../api/auth'

const { Title, Text } = Typography

export default function ForgotPasswordPage() {
  const [message, setMessage] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  async function onFinish(values: { email: string }) {
    setError(null)
    setMessage(null)
    setLoading(true)
    try {
      const result = await requestPasswordReset(values)
      setMessage(result.message || 'Hướng dẫn đặt lại mật khẩu đã được gửi qua email.')
    } catch (err: unknown) {
      setError(
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ??
          'Không thể gửi yêu cầu đặt lại mật khẩu.',
      )
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-svh flex items-center justify-center bg-gray-50 px-4">
      <Card className="w-full max-w-md">
        <Title level={3} className="text-center!">Quên mật khẩu</Title>
        <Text type="secondary">Nhập email để nhận liên kết đặt lại mật khẩu.</Text>
        {error && <Alert type="error" message={error} showIcon className="my-4" />}
        {message && <Alert type="success" message={message} showIcon className="my-4" />}
        <Form layout="vertical" onFinish={onFinish} disabled={loading} className="mt-4">
          <Form.Item label="Email" name="email" rules={[{ required: true, message: 'Nhập email' }, { type: 'email', message: 'Email không hợp lệ' }]}>
            <Input placeholder="ban@example.com" />
          </Form.Item>
          <Button type="primary" htmlType="submit" block loading={loading}>Gửi liên kết đặt lại</Button>
        </Form>
        <div className="text-center mt-4"><Link to="/login">Quay lại đăng nhập</Link></div>
      </Card>
    </div>
  )
}