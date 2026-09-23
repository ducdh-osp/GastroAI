import { Alert, Button, Form, Input } from 'antd'
import { useState } from 'react'
import { Link } from 'react-router-dom'
import { requestPasswordReset } from '../../api/auth'
import { AuthShell } from '../../components/auth/AuthShell'

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
    <AuthShell
      title="Quên mật khẩu"
      subtitle="Nhập email để nhận liên kết đặt lại mật khẩu."
      eyebrow="Khôi phục truy cập"
      heroTitle="Lấy lại quyền truy cập tài khoản"
      heroDescription="Chúng tôi sẽ gửi một liên kết đặt lại mật khẩu tới email bạn đã đăng ký, có hiệu lực trong 30 phút."
    >
      {error && <Alert type="error" message={error} showIcon className="mb-4" />}
      {message && <Alert type="success" message={message} showIcon className="mb-4" />}
      <Form layout="vertical" onFinish={onFinish} disabled={loading}>
        <Form.Item label="Email" name="email" rules={[{ required: true, message: 'Nhập email' }, { type: 'email', message: 'Email không hợp lệ' }]}>
          <Input placeholder="ban@example.com" />
        </Form.Item>
        <Button type="primary" htmlType="submit" block loading={loading}>Gửi liên kết đặt lại</Button>
      </Form>
      <div className="text-center mt-4"><Link to="/login" className="text-teal-700 hover:text-teal-800">Quay lại đăng nhập</Link></div>
    </AuthShell>
  )
}
