import { CheckCircleFilled, ExclamationCircleOutlined, LockOutlined, MailOutlined, ReadOutlined } from '@ant-design/icons'
import { Alert, Button, Form, Input, Typography } from 'antd'
import { useState } from 'react'
import { Link } from 'react-router-dom'
import { register as registerApi, resendVerification } from '../../api/auth'
import { AuthShell } from '../../components/auth/AuthShell'
import {
  isAtMostUtf8Bytes,
  meetsPasswordRequirements,
  PASSWORD_REQUIREMENTS_MESSAGE,
} from '../../lib/validation'

const { Text } = Typography

const REGISTER_TRUST_ITEMS = [
  { icon: ReadOutlined, text: 'Câu trả lời có trích dẫn tài liệu y khoa' },
  { icon: ExclamationCircleOutlined, text: 'Cảnh báo ngay khi có dấu hiệu nguy hiểm' },
  { icon: LockOutlined, text: 'Dữ liệu sức khỏe được mã hóa riêng tư' },
]

interface RegisterFormValues {
  fullName: string
  email: string
  password: string
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

export default function RegisterPage() {
  const [form] = Form.useForm<RegisterFormValues>()
  const passwordValue = Form.useWatch('password', form) ?? ''
  const [error, setError] = useState<string | null>(null)
  const [registeredEmail, setRegisteredEmail] = useState<string | null>(null)
  const [resendMessage, setResendMessage] = useState<string | null>(null)
  const [resendError, setResendError] = useState<string | null>(null)
  const [resending, setResending] = useState(false)
  const [loading, setLoading] = useState(false)

  async function onFinish(values: RegisterFormValues) {
    setError(null)
    setLoading(true)
    try {
      await registerApi(values)
      setRegisteredEmail(values.email.trim())
    } catch (err: unknown) {
      setError(
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ??
          'Đăng ký thất bại, vui lòng thử lại.',
      )
    } finally {
      setLoading(false)
    }
  }

  async function handleResend() {
    if (!registeredEmail) return
    setResending(true)
    setResendMessage(null)
    setResendError(null)
    try {
      const res = await resendVerification(registeredEmail)
      setResendMessage(res.message || 'Đã gửi lại email xác thực. Vui lòng kiểm tra hộp thư (kể cả thư rác).')
    } catch (err: unknown) {
      setResendError(
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ??
          'Không thể gửi lại email lúc này, vui lòng thử lại sau ít phút.',
      )
    } finally {
      setResending(false)
    }
  }

  return (
    <AuthShell
      title={registeredEmail ? 'Kiểm tra hộp thư của bạn' : 'Tạo tài khoản GastroAI'}
      subtitle={
        registeredEmail
          ? 'Mở liên kết xác thực trong email để kích hoạt tài khoản và bắt đầu sử dụng.'
          : 'Bắt đầu hành trình chăm sóc hệ tiêu hóa cùng AI.'
      }
      eyebrow="Bắt đầu miễn phí"
      heroTitle="Trợ lý AI tiêu hóa, trích dẫn nguồn y khoa thật"
      heroDescription="Bắt đầu hành trình chăm sóc hệ tiêu hóa với thông tin tham khảo đáng tin cậy."
      trustItems={REGISTER_TRUST_ITEMS}
    >
      {registeredEmail ? (
        <div className="space-y-4">
          <div className="rounded-xl border border-teal-200 bg-teal-50/70 p-4 text-sm text-teal-900 dark:border-teal-800 dark:bg-teal-950/40 dark:text-teal-100">
            <div className="mb-1.5 flex items-center gap-2 font-semibold">
              <MailOutlined />
              <span>Đã gửi email xác thực</span>
            </div>
            <p className="mb-0 leading-relaxed">
              Chúng tôi đã gửi liên kết kích hoạt tài khoản đến <strong>{registeredEmail}</strong>. Vui lòng mở hộp thư (bao gồm mục Thư rác / Spam) và nhấn vào liên kết để hoàn tất đăng ký.
            </p>
          </div>

          {resendMessage && <Alert type="success" message={resendMessage} showIcon />}
          {resendError && <Alert type="error" message={resendError} showIcon />}

          <Button block loading={resending} onClick={() => void handleResend()}>
            Gửi lại email xác thực
          </Button>
          <div className="text-center pt-1">
            <Link to="/login" className="text-teal-700 hover:text-teal-800 dark:text-teal-400 dark:hover:text-teal-300">
              Đi tới trang Đăng nhập
            </Link>
          </div>
        </div>
      ) : (
        <>
          {error && (
            <Alert
              type="error"
              message={error}
              description={
                error.toLowerCase().includes('tồn tại') || error.toLowerCase().includes('đã được') ? (
                  <span>
                    Bạn có thể{' '}
                    <Link to="/login" className="underline">Đăng nhập</Link> hoặc{' '}
                    <Link to="/forgot-password" className="underline">Quên mật khẩu</Link>.
                  </span>
                ) : undefined
              }
              showIcon
              className="mb-4"
            />
          )}
          <Form<RegisterFormValues> form={form} layout="vertical" onFinish={onFinish} disabled={loading}>
            <Form.Item label="Họ và tên" name="fullName" rules={[{ required: true, whitespace: true, message: 'Vui lòng nhập họ tên của bạn' }]}>
              <Input placeholder="Nguyễn Văn A" />
            </Form.Item>
            <Form.Item
              label="Email"
              name="email"
              rules={[
                { required: true, message: 'Vui lòng nhập email' },
                { type: 'email', message: 'Email chưa đúng định dạng' },
                { max: 255, message: 'Email tối đa 255 ký tự' },
              ]}
            >
              <Input maxLength={255} placeholder="ban@example.com" />
            </Form.Item>
            <Form.Item
              label="Mật khẩu"
              name="password"
              extra={<PasswordChecklist value={passwordValue} />}
              rules={[
                { required: true, message: 'Vui lòng nhập mật khẩu' },
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
            <Button type="primary" htmlType="submit" block loading={loading}>Đăng ký</Button>
          </Form>
          <div className="text-center mt-4">
            <Text type="secondary">Đã có tài khoản? </Text>
            <Link to="/login" className="text-teal-700 hover:text-teal-800 dark:text-teal-400 dark:hover:text-teal-300">Đăng nhập</Link>
          </div>
        </>
      )}
    </AuthShell>
  )
}
