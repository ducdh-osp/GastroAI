import { CopyOutlined, ReloadOutlined, UserAddOutlined } from '@ant-design/icons'
import { Alert, Button, Card, Form, Input, message, Select, Typography } from 'antd'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { createCmsAccount, type CmsPermissionRole } from '../../api/cmsRbac'
import { CmsAppShell } from '../../components/cms/CmsAppShell'

const { Title, Text, Paragraph } = Typography

const ROLE_OPTIONS: { value: CmsPermissionRole; label: string }[] = [
  { value: 'ADMIN', label: 'Admin' },
  { value: 'CONTENT_EDITOR', label: 'Biên tập chuyên môn y khoa' },
  { value: 'CONTENT_AUTHOR', label: 'Cộng tác viên nội dung' },
  { value: 'OPERATIONS', label: 'Giám sát vận hành' },
]

const UPPER = 'ABCDEFGHJKLMNPQRSTUVWXYZ'
const LOWER = 'abcdefghjkmnpqrstuvwxyz'
const DIGIT = '23456789'
const SPECIAL = '!@#$%*?'

// Dam bao luon dat PasswordPolicy (>=8 ky tu, co hoa/so/ky tu dac biet) du random the nao.
function generatePassword(): string {
  const pick = (chars: string) => chars[Math.floor(Math.random() * chars.length)]
  const all = UPPER + LOWER + DIGIT + SPECIAL
  const required = [pick(UPPER), pick(LOWER), pick(DIGIT), pick(SPECIAL)]
  const rest = Array.from({ length: 6 }, () => pick(all))
  return [...required, ...rest].sort(() => Math.random() - 0.5).join('')
}

interface FormValues {
  fullName: string
  email: string
  role: CmsPermissionRole
  password: string
}

export default function CmsCreateAccountPage() {
  const navigate = useNavigate()
  const [form] = Form.useForm<FormValues>()
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [created, setCreated] = useState<{ fullName: string; password: string } | null>(null)

  async function onFinish(values: FormValues) {
    setSubmitting(true)
    setError(null)
    try {
      await createCmsAccount(values)
      setCreated({ fullName: values.fullName, password: values.password })
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không thể tạo tài khoản.')
    } finally {
      setSubmitting(false)
    }
  }

  function copyPassword() {
    if (!created) return
    void navigator.clipboard.writeText(created.password)
    message.success('Đã sao chép mật khẩu.')
  }

  if (created) {
    return (
      <CmsAppShell>
        <div className="mx-auto w-full max-w-xl">
          <Card className="rounded-2xl border-black/5 shadow-sm dark:border-white/10! dark:bg-neutral-900!">
            <Title level={4} className="mb-1!">✓ Đã tạo tài khoản cho "{created.fullName}"</Title>
            <Paragraph type="secondary" className="mb-4!">
              Mật khẩu ban đầu chỉ hiện <strong>duy nhất 1 lần</strong> ngay tại đây — hãy gửi cho nhân sự này ngay,
              không thể xem lại sau khi rời khỏi màn hình này.
            </Paragraph>
            <div className="mb-4 flex items-center gap-2 rounded-xl bg-gray-50 p-3 font-mono text-base dark:bg-white/5">
              <span className="grow">{created.password}</span>
              <Button icon={<CopyOutlined />} onClick={copyPassword}>Sao chép</Button>
            </div>
            <Alert
              type="info"
              showIcon
              className="mb-4"
              message="Khuyến nghị nhắc nhân sự đổi mật khẩu ngay sau lần đăng nhập đầu tiên."
            />
            <Button type="primary" onClick={() => navigate('/cms/rbac')}>Về danh sách Phân quyền</Button>
          </Card>
        </div>
      </CmsAppShell>
    )
  }

  return (
    <CmsAppShell>
      <div className="mx-auto w-full max-w-xl">
        <Link to="/cms/rbac" className="mb-3 inline-block text-sm">‹ Quay lại Phân quyền nhân sự</Link>
        <Title level={3} className="mb-1! mt-1!"><UserAddOutlined className="mr-2" />Tạo tài khoản nhân sự CMS</Title>
        <Text type="secondary">Chỉ Admin tạo được tài khoản mới — xem SCR-AD-14.</Text>
        {error && <Alert type="error" message={error} showIcon className="mt-4" closable onClose={() => setError(null)} />}
        <Card className="mt-4 rounded-2xl border-black/5 shadow-sm dark:border-white/10! dark:bg-neutral-900!">
          <Form<FormValues> form={form} layout="vertical" onFinish={onFinish} disabled={submitting}>
            <Form.Item label="Họ và tên" name="fullName" rules={[{ required: true, message: 'Nhập họ và tên' }]}>
              <Input placeholder="Vd: Lê Văn E" />
            </Form.Item>
            <Form.Item
              label="Email"
              name="email"
              rules={[{ required: true, type: 'email', message: 'Nhập đúng định dạng email' }]}
            >
              <Input placeholder="ten@gastroai.vn" autoComplete="off" />
            </Form.Item>
            <Form.Item label="Vai trò" name="role" rules={[{ required: true, message: 'Chọn 1 vai trò' }]}>
              <Select placeholder="Chọn vai trò..." options={ROLE_OPTIONS} />
            </Form.Item>
            <Form.Item
              label="Mật khẩu ban đầu"
              name="password"
              extra="Tối thiểu 8 ký tự, có hoa, số và ký tự đặc biệt"
              rules={[{ required: true, min: 8, message: 'Nhập mật khẩu đạt chuẩn' }]}
            >
              <Input.Password
                placeholder="Nhập hoặc bấm Tạo ngẫu nhiên"
                addonAfter={
                  <ReloadOutlined onClick={() => form.setFieldValue('password', generatePassword())} />
                }
              />
            </Form.Item>
            <Button type="primary" htmlType="submit" loading={submitting}>Tạo tài khoản</Button>
          </Form>
        </Card>
      </div>
    </CmsAppShell>
  )
}
