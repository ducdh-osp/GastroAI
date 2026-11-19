import { CheckCircleFilled } from '@ant-design/icons'
import { Alert, Button, Form, Input } from 'antd'
import { useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { resetPassword } from '../../api/auth'
import { AuthShell } from '../../components/auth/AuthShell'
import {
  isAtMostUtf8Bytes,
  meetsPasswordRequirements,
  PASSWORD_REQUIREMENTS_MESSAGE,
} from '../../lib/validation'

interface ResetPasswordFormValues {
  newPassword: string
}

function PasswordChecklist({ value }: { value: string }) {
  const checks = [
    { ok: value.length >= 8, label: 'Tối thiểu 8 ký tự' },
    { ok: /[A-Z]/.test(value), label: 'Ít nhất 1 chữ in hoa (A-Z)' },
    { ok: /[0-9]/.test(value), label: 'Ít nhất 1 chữ số (0-9)' },
    { ok: /[^A-Za-z0-9]/.test(value), label: 'Ít nhất 1 ký tự đặc biệt' },
  ]
  return (
    <ul className="mt-2 mb-0 list-none space-y-1 p-0 text-xs">
      {checks.map((item) => (
        <li
          key={item.label}
          className={`flex items-center gap-1.5 transition-colors ${
            item.ok ? 'text-emerald-600 dark:text-emerald-400 font-medium' : 'text-slate-500 dark:text-slate-400'
          }`}
        >
          <CheckCircleFilled className={item.ok ? 'text-emerald-600 dark:text-emerald-400' : 'text-slate-300 dark:text-slate-600'} />
          <span>{item.label}</span>
        </li>
      ))}
    </ul>
  )
}

export default function ResetPasswordPage() {
  const navigate = useNavigate()
  const [form] = Form.useForm<ResetPasswordFormValues>()
  const newPasswordValue = Form.useWatch('newPassword', form) ?? ''
  const [searchParams] = useSearchParams()
  const token = searchParams.get('token') ?? ''
  const [error, setError] = useState<string | null>(token ? null : 'Liên kết đặt lại mật khẩu không có token hoặc đã hết hạn.')
  const [tokenInvalid, setTokenInvalid] = useState(!token)
  const [success, setSuccess] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  async function onFinish(values: ResetPasswordFormValues) {
    setError(null)
    setSuccess(null)
    if (!token) {
      setTokenInvalid(true)
      setError('Liên kết đặt lại mật khẩu không hợp lệ.')
      return
    }

    setLoading(true)
    try {
      await resetPassword({ token, newPassword: values.newPassword })
      setSuccess('Đặt lại mật khẩu thành công, hãy đăng nhập bằng mật khẩu mới.')
      setTimeout(() => navigate('/login'), 1500)
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ??
        'Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn.'
      setError(msg)
      const lower = msg.toLowerCase()
      if (lower.includes('token') || lower.includes('hết hạn') || lower.includes('het han') || lower.includes('liên kết')) {
        setTokenInvalid(true)
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <AuthShell
      title="Đặt lại mật khẩu"
      subtitle="Tạo mật khẩu mới an toàn rồi đăng nhập lại."
      eyebrow="Khôi phục truy cập"
      heroTitle="Tạo mật khẩu mới"
      heroDescription="Chọn mật khẩu đủ mạnh và khác với các mật khẩu bạn từng dùng để bảo vệ dữ liệu sức khỏe của bạn."
    >
      {error && (
        <Alert
          type="error"
          message={error}
          description={
            tokenInvalid ? (
              <Link to="/forgot-password" className="font-medium underline">
                Yêu cầu gửi lại liên kết đặt lại mật khẩu mới
              </Link>
            ) : undefined
          }
          showIcon
          className="mb-4"
        />
      )}
      {success && <Alert type="success" message={success} showIcon className="mb-4" />}
      <Form<ResetPasswordFormValues> form={form} layout="vertical" onFinish={onFinish} disabled={loading || !token}>
        <Form.Item
          label="Mật khẩu mới"
          name="newPassword"
          extra={<PasswordChecklist value={newPasswordValue} />}
          rules={[
            { required: true, message: 'Vui lòng nhập mật khẩu mới' },
            {
              validator: (_, value: string) =>
                !value || meetsPasswordRequirements(value)
                  ? Promise.resolve()
                  : Promise.reject(new Error(PASSWORD_REQUIREMENTS_MESSAGE)),
            },
            { max: 72, message: 'Mật khẩu tối đa 72 ký tự' },
            {
              validator: (_, value: string) =>
                !value || isAtMostUtf8Bytes(value, 72)
                  ? Promise.resolve()
                  : Promise.reject(new Error('Mật khẩu tối đa 72 byte UTF-8')),
            },
          ]}
        >
          <Input.Password maxLength={72} autoComplete="new-password" />
        </Form.Item>
        <Button type="primary" htmlType="submit" block loading={loading} disabled={!token}>Đặt lại mật khẩu</Button>
      </Form>
      <div className="text-center mt-4">
        <Link to="/login" className="text-teal-700 hover:text-teal-800 dark:text-teal-400 dark:hover:text-teal-300">
          Quay lại đăng nhập
        </Link>
      </div>
    </AuthShell>
  )
}
