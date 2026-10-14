import { SettingOutlined } from '@ant-design/icons'
import { Alert, Button, Card, Form, InputNumber, Spin, Switch, Typography } from 'antd'
import { useEffect, useState } from 'react'
import { getRuntimeConfig, updateRuntimeConfig } from '../../api/cmsRuntimeConfig'
import { CmsAppShell } from '../../components/cms/CmsAppShell'

const { Title, Text } = Typography

interface FormValues {
  ragTopK: number
  similarityThresholdEnabled: boolean
  ragSimilarityThreshold: number | null
  triageRealtimeAlertsEnabled: boolean
}

export default function CmsRuntimeConfigPage() {
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)
  const [form] = Form.useForm<FormValues>()

  useEffect(() => {
    setLoading(true)
    getRuntimeConfig()
      .then((config) => {
        form.setFieldsValue({
          ragTopK: config.ragTopK,
          similarityThresholdEnabled: config.ragSimilarityThreshold !== null,
          ragSimilarityThreshold: config.ragSimilarityThreshold ?? 0.6,
          triageRealtimeAlertsEnabled: config.triageRealtimeAlertsEnabled,
        })
      })
      .catch((err: unknown) => setError(err instanceof Error ? err.message : 'Không thể tải cấu hình hệ thống.'))
      .finally(() => setLoading(false))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  async function handleSubmit(values: FormValues) {
    setSaving(true)
    setError(null)
    setSuccess(null)
    try {
      const updated = await updateRuntimeConfig({
        ragTopK: values.ragTopK,
        ragSimilarityThreshold: values.similarityThresholdEnabled ? values.ragSimilarityThreshold : null,
        triageRealtimeAlertsEnabled: values.triageRealtimeAlertsEnabled,
      })
      form.setFieldsValue({
        ragTopK: updated.ragTopK,
        similarityThresholdEnabled: updated.ragSimilarityThreshold !== null,
        ragSimilarityThreshold: updated.ragSimilarityThreshold ?? 0.6,
        triageRealtimeAlertsEnabled: updated.triageRealtimeAlertsEnabled,
      })
      setSuccess('Đã lưu cấu hình. Áp dụng ngay cho các câu hỏi/cảnh báo tiếp theo, không cần khởi động lại hệ thống.')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không thể lưu cấu hình hệ thống.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <CmsAppShell>
      <div className="mx-auto max-w-2xl">
        <div className="mb-6">
          <Text type="secondary">Cấu hình</Text>
          <Title level={2} className="mb-1! mt-1!">
            <SettingOutlined className="mr-2" />
            Cấu hình RAG / Triage
          </Title>
          <Text type="secondary">
            Chỉnh tham số truy xuất tri thức (RAG) và cảnh báo Triage mà không cần sửa code hay khởi động lại hệ
            thống. Chỉ Admin được xem/sửa trang này.
          </Text>
        </div>

        {error && <Alert type="error" message={error} showIcon className="mb-4" closable onClose={() => setError(null)} />}
        {success && <Alert type="success" message={success} showIcon className="mb-4" closable onClose={() => setSuccess(null)} />}

        <Spin spinning={loading}>
          <Card className="rounded-2xl border-black/5 shadow-sm">
            <Form form={form} layout="vertical" onFinish={handleSubmit}>
              <Form.Item
                name="ragTopK"
                label="Số lượng đoạn tài liệu truy xuất (top-k)"
                extra="Số đoạn ngữ cảnh gần nhất lấy từ kho tri thức để trả lời mỗi câu hỏi. Giá trị 1-20."
                rules={[{ required: true, message: 'Nhập top-k' }]}
              >
                <InputNumber min={1} max={20} className="w-full" />
              </Form.Item>

              <Form.Item name="similarityThresholdEnabled" label="Lọc theo độ tương đồng" valuePropName="checked">
                <Switch />
              </Form.Item>

              <Form.Item
                noStyle
                shouldUpdate={(prev, cur) => prev.similarityThresholdEnabled !== cur.similarityThresholdEnabled}
              >
                {({ getFieldValue }) =>
                  getFieldValue('similarityThresholdEnabled') && (
                    <Form.Item
                      name="ragSimilarityThreshold"
                      label="Ngưỡng khoảng cách tối đa"
                      extra="0 = chỉ lấy đoạn giống hệt câu hỏi, 2 = không lọc gì (lấy cả đoạn không liên quan). Đoạn xa hơn ngưỡng này sẽ bị bỏ dù vẫn còn trong top-k."
                      rules={[{ required: true, message: 'Nhập ngưỡng' }]}
                    >
                      <InputNumber min={0} max={2} step={0.05} className="w-full" />
                    </Form.Item>
                  )
                }
              </Form.Item>

              <Form.Item
                name="triageRealtimeAlertsEnabled"
                label="Thông báo cảnh báo khẩn cấp realtime (WebSocket)"
                extra="Tắt tạm thời khi bảo trì/debug - cảnh báo vẫn được tạo và lưu lại, chỉ không đẩy ngay qua WebSocket cho CMS."
                valuePropName="checked"
              >
                <Switch />
              </Form.Item>

              <Button type="primary" htmlType="submit" loading={saving}>Lưu cấu hình</Button>
            </Form>
          </Card>
        </Spin>
      </div>
    </CmsAppShell>
  )
}
