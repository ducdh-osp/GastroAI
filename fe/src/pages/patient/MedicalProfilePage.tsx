import { Alert, Button, Card, Form, Input, Select, Spin, Typography } from 'antd'
import { LoadingOutlined } from '@ant-design/icons'
import { useEffect, useState } from 'react'
import { getMedicalProfile, updateMedicalProfile } from '../../api/medicalProfile'
import { AppShell } from '../../components/layout/AppShell'

const { Title, Text } = Typography

interface MedicalProfileFormValues {
  medicalHistory: string
  allergies: string[]
  currentMedications: string[]
}

export default function MedicalProfilePage() {
  const [form] = Form.useForm<MedicalProfileFormValues>()
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    getMedicalProfile()
      .then((profile) => {
        if (cancelled) return
        form.setFieldsValue({
          medicalHistory: profile.medicalHistory ?? '',
          allergies: profile.allergies,
          currentMedications: profile.currentMedications,
        })
      })
      .catch(() => {
        if (!cancelled) setError('Không thể tải hồ sơ bệnh lý.')
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => { cancelled = true }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  async function onFinish(values: MedicalProfileFormValues) {
    setError(null)
    setSuccess(null)
    setSaving(true)
    try {
      await updateMedicalProfile({
        medicalHistory: values.medicalHistory ?? '',
        allergies: values.allergies ?? [],
        currentMedications: values.currentMedications ?? [],
      })
      setSuccess('Đã lưu hồ sơ bệnh lý.')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không thể lưu hồ sơ bệnh lý.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <AppShell>
      <div className="mx-auto max-w-2xl">
        <div className="mb-6">
          <Text type="secondary">Thông tin sức khỏe</Text>
          <Title level={2} className="mb-1! mt-1!">Hồ sơ bệnh lý cá nhân</Title>
          <Text type="secondary">
            Khai báo tiền sử bệnh, dị ứng và thuốc đang dùng để trợ lý AI tư vấn chính xác hơn. Đây là thông tin tham khảo, không thay thế chẩn đoán y tế.
          </Text>
        </div>

        <Card className="rounded-2xl border-black/5 shadow-sm">
          {loading ? (
            <div className="flex justify-center py-12">
              <Spin indicator={<LoadingOutlined className="text-teal-600 text-xl" spin />} />
            </div>
          ) : (
            <>
              {error && <Alert type="error" message={error} showIcon className="mb-4" />}
              {success && <Alert type="success" message={success} showIcon className="mb-4" />}
              <Form<MedicalProfileFormValues> form={form} layout="vertical" onFinish={onFinish} disabled={saving}>
                <Form.Item label="Tiền sử bệnh" name="medicalHistory">
                  <Input.TextArea rows={4} placeholder="Vd: từng phẫu thuật ruột thừa năm 2020, viêm dạ dày mạn tính..." />
                </Form.Item>
                <Form.Item label="Dị ứng" name="allergies">
                  <Select mode="tags" placeholder="Gõ tên dị ứng rồi nhấn Enter để thêm" />
                </Form.Item>
                <Form.Item label="Thuốc đang dùng" name="currentMedications">
                  <Select mode="tags" placeholder="Gõ tên thuốc rồi nhấn Enter để thêm" />
                </Form.Item>
                <Button type="primary" htmlType="submit" loading={saving}>Lưu hồ sơ</Button>
              </Form>
            </>
          )}
        </Card>
      </div>
    </AppShell>
  )
}
