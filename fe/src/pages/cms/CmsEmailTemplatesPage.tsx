import { EyeOutlined, MailOutlined, SaveOutlined, SendOutlined } from '@ant-design/icons'
import { Alert, Button, Card, Form, Input, Modal, Select, Space, Spin, Tag, Typography } from 'antd'
import { useEffect, useMemo, useState } from 'react'
import {
  listEmailTemplates,
  previewEmailTemplate,
  sendTestEmail,
  updateEmailTemplate,
  type EmailTemplate,
  type EmailTemplateCode,
  type EmailTemplateContent,
  type RenderedEmail,
} from '../../api/cmsEmailTemplates'
import { CmsAppShell } from '../../components/cms/CmsAppShell'

const { Title, Text, Paragraph } = Typography

export default function CmsEmailTemplatesPage() {
  const [templates, setTemplates] = useState<EmailTemplate[]>([])
  const [selectedCode, setSelectedCode] = useState<EmailTemplateCode | null>(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [previewing, setPreviewing] = useState(false)
  const [sendingTest, setSendingTest] = useState(false)
  const [testRecipient, setTestRecipient] = useState('')
  const [preview, setPreview] = useState<RenderedEmail | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)
  const [form] = Form.useForm<EmailTemplateContent>()

  const selected = useMemo(
    () => templates.find((template) => template.code === selectedCode) ?? null,
    [templates, selectedCode],
  )

  useEffect(() => {
    let cancelled = false
    listEmailTemplates()
      .then((items) => {
        if (cancelled) return
        setTemplates(items)
        const firstTemplate = items[0] ?? null
        setSelectedCode(firstTemplate?.code ?? null)
        if (firstTemplate) {
          form.setFieldsValue({
            subjectTemplate: firstTemplate.subjectTemplate,
            htmlBodyTemplate: firstTemplate.htmlBodyTemplate,
            textBodyTemplate: firstTemplate.textBodyTemplate,
          })
        }
      })
      .catch((reason: unknown) => {
        if (!cancelled) setError(reason instanceof Error ? reason.message : 'Không thể tải mẫu email.')
      })
      .finally(() => { if (!cancelled) setLoading(false) })
    return () => { cancelled = true }
  }, [form])

  function handleTemplateChange(code: EmailTemplateCode) {
    const template = templates.find((item) => item.code === code)
    setSelectedCode(code)
    if (template) {
      form.setFieldsValue({
        subjectTemplate: template.subjectTemplate,
        htmlBodyTemplate: template.htmlBodyTemplate,
        textBodyTemplate: template.textBodyTemplate,
      })
    }
    setPreview(null)
    setError(null)
    setSuccess(null)
  }

  async function handleSave(values: EmailTemplateContent) {
    if (!selected) return
    setSaving(true)
    setError(null)
    setSuccess(null)
    try {
      const updated = await updateEmailTemplate(selected.code, values, selected.version)
      setTemplates((current) => current.map((item) => item.code === updated.code ? updated : item))
      setSuccess('Đã lưu mẫu email. Nội dung mới sẽ áp dụng cho các email được gửi tiếp theo.')
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'Không thể lưu mẫu email.')
    } finally {
      setSaving(false)
    }
  }

  async function handlePreview() {
    if (!selected) return
    setPreviewing(true)
    setError(null)
    try {
      const values = await form.validateFields()
      setPreview(await previewEmailTemplate(selected.code, values))
    } catch (reason) {
      if (reason instanceof Error) setError(reason.message)
    } finally {
      setPreviewing(false)
    }
  }

  async function handleSendTest() {
    if (!selected) return
    const recipient = testRecipient.trim()
    if (!recipient) {
      setError('Vui lòng nhập email nhận bản thử.')
      return
    }
    setSendingTest(true)
    setError(null)
    setSuccess(null)
    try {
      const message = await sendTestEmail(selected.code, recipient)
      setSuccess(message)
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'Không thể gửi email thử.')
    } finally {
      setSendingTest(false)
    }
  }

  return (
    <CmsAppShell>
      <div className="mx-auto max-w-5xl">
        <div className="mb-6">
          <Text type="secondary">Cấu hình</Text>
          <Title level={2} className="mb-1! mt-1!">
            <MailOutlined className="mr-2" />
            Mẫu email hệ thống
          </Title>
          <Paragraph type="secondary">
            Chỉnh nội dung email xác thực, đặt lại mật khẩu và nhắc uống thuốc offline mà không cần sửa code.
            Chỉ Admin có quyền truy cập.
          </Paragraph>
        </div>

        {error && <Alert type="error" message={error} showIcon closable onClose={() => setError(null)} className="mb-4" />}
        {success && <Alert type="success" message={success} showIcon closable onClose={() => setSuccess(null)} className="mb-4" />}

        {loading ? <div className="py-16 text-center"><Spin size="large" /></div> : (
          <div className="grid gap-5 lg:grid-cols-[280px_minmax(0,1fr)]">
            <Card className="h-fit rounded-2xl border-black/5 shadow-sm">
              <Text strong>Loại email</Text>
              <Select<EmailTemplateCode>
                className="mt-3 w-full"
                value={selectedCode ?? undefined}
                onChange={handleTemplateChange}
                options={templates.map((template) => ({ value: template.code, label: template.displayName }))}
              />
              {selected && (
                <div className="mt-5 space-y-3">
                  <div>
                    <Text type="secondary" className="text-xs">Placeholder được phép</Text>
                    <div className="mt-2 flex flex-wrap gap-1.5">
                      {selected.allowedVariables.map((variable) => (
                        <Tag key={variable} color="cyan">{`{{${variable}}}`}</Tag>
                      ))}
                    </div>
                  </div>
                  <Text type="secondary" className="block text-xs">
                    Phiên bản {selected.version}
                    {selected.updatedById ? ` · Admin #${selected.updatedById}` : ''}
                  </Text>
                  <Text type="secondary" className="block text-xs">
                    Cập nhật: {new Date(selected.updatedAt).toLocaleString('vi-VN')}
                  </Text>
                </div>
              )}
            </Card>

            <div className="space-y-5">
              <Card className="rounded-2xl border-black/5 shadow-sm">
                <Form<EmailTemplateContent> form={form} layout="vertical" onFinish={handleSave} disabled={!selected || saving}>
                  <Form.Item
                    label="Tiêu đề email"
                    name="subjectTemplate"
                    rules={[{ required: true, message: 'Nhập tiêu đề email' }, { max: 255, message: 'Tối đa 255 ký tự' }]}
                  >
                    <Input maxLength={255} showCount />
                  </Form.Item>
                  <Form.Item
                    label="Nội dung HTML"
                    name="htmlBodyTemplate"
                    extra="Không cho phép script, iframe, form hoặc thuộc tính sự kiện JavaScript."
                    rules={[{ required: true, message: 'Nhập nội dung HTML' }, { max: 100000, message: 'Tối đa 100.000 ký tự' }]}
                  >
                    <Input.TextArea rows={11} className="font-mono text-sm" maxLength={100000} />
                  </Form.Item>
                  <Form.Item
                    label="Nội dung text dự phòng"
                    name="textBodyTemplate"
                    extra="Dùng khi ứng dụng email của người nhận không hiển thị HTML."
                    rules={[{ required: true, message: 'Nhập nội dung text' }, { max: 100000, message: 'Tối đa 100.000 ký tự' }]}
                  >
                    <Input.TextArea rows={6} maxLength={100000} />
                  </Form.Item>
                  <Space wrap>
                    <Button type="primary" htmlType="submit" icon={<SaveOutlined />} loading={saving}>Lưu mẫu</Button>
                    <Button icon={<EyeOutlined />} onClick={() => void handlePreview()} loading={previewing}>Xem trước</Button>
                  </Space>
                </Form>
              </Card>

              <Card title="Gửi email thử" className="rounded-2xl border-black/5 shadow-sm">
                <Space.Compact className="w-full">
                  <Input
                    type="email"
                    value={testRecipient}
                    onChange={(event) => setTestRecipient(event.target.value)}
                    onPressEnter={() => void handleSendTest()}
                    placeholder="admin@example.com"
                    maxLength={255}
                  />
                  <Button icon={<SendOutlined />} loading={sendingTest} onClick={() => void handleSendTest()}>
                    Gửi thử
                  </Button>
                </Space.Compact>
                <Text type="secondary" className="mt-2 block text-xs">
                  Email thử dùng nội dung đã lưu gần nhất và dữ liệu placeholder mẫu.
                </Text>
              </Card>
            </div>
          </div>
        )}
      </div>

      <Modal
        title={preview ? `Xem trước: ${preview.subject}` : 'Xem trước email'}
        open={Boolean(preview)}
        onCancel={() => setPreview(null)}
        footer={<Button onClick={() => setPreview(null)}>Đóng</Button>}
        width={760}
      >
        {preview && (
          <div className="space-y-4">
            <div>
              <Text strong>HTML</Text>
              <iframe
                title="Xem trước nội dung email"
                sandbox=""
                srcDoc={preview.htmlBody}
                className="mt-2 h-80 w-full rounded-lg border border-slate-200 bg-white"
              />
            </div>
            <div>
              <Text strong>Text dự phòng</Text>
              <pre className="mt-2 whitespace-pre-wrap rounded-lg bg-slate-50 p-4 text-sm dark:bg-slate-900">{preview.textBody}</pre>
            </div>
          </div>
        )}
      </Modal>
    </CmsAppShell>
  )
}
