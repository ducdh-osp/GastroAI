import { Alert, Button, Form, Input } from 'antd'
import { useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { resetPassword } from '../../api/auth'
import { AuthShell } from '../../components/auth/AuthShell'

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
    <AuthShell
      title="Đặt lại mật khẩu"
      subtitle="Liên kết email đã xác thực. Hãy tạo mật khẩu mới rồi đăng nhập lại."
      eyebrow="Khôi phục truy cập"
      heroTitle="Tạo mật khẩu mới"
      heroDescription="Chọn mật khẩu đủ mạnh và không trùng với mật khẩu cũ để bảo vệ dữ liệu sức khỏe của bạn."
    >
      {error && <Alert type="error" message={error} showIcon className="mb-4" />}
      {success && <Alert type="success" message={success} showIcon className="mb-4" />}
      <Form<ResetPasswordFormValues> layout="vertical" onFinish={onFinish} disabled={loading}>
        <Form.Item label="Mật khẩu mới" name="newPassword" rules={[{ required: true, message: 'Nhập mật khẩu mới' }, { min: 8, message: 'Mật khẩu tối thiểu 8 ký tự' }]}>
          <Input.Password />
        </Form.Item>
        <Form.Item label="Xác nhận mật khẩu mới" name="confirmPassword" rules={[{ required: true, message: 'Nhập lại mật khẩu mới' }]}>
          <Input.Password />
        </Form.Item>
        <Button type="primary" htmlType="submit" block loading={loading}>Đổi mật khẩu</Button>
      </Form>
      <div className="text-center mt-4"><Link to="/login" className="text-teal-700 hover:text-teal-800">Quay lại đăng nhập</Link></div>
    </AuthShell>
  )
}
