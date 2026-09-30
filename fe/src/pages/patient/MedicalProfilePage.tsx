import { Alert, Button, Card, DatePicker, Form, Input, InputNumber, Select, Spin, Typography } from 'antd'
import { LoadingOutlined } from '@ant-design/icons'
import dayjs, { type Dayjs } from 'dayjs'
import { useCallback, useEffect, useRef, useState } from 'react'
import { getMedicalProfile, updateMedicalProfile } from '../../api/medicalProfile'
import type { Gender } from '../../api/medicalProfile'
import { AppShell } from '../../components/layout/AppShell'

const { Title, Text } = Typography

const GENDER_OPTIONS: { value: Gender; label: string }[] = [
  { value: 'MALE', label: 'Nam' },
  { value: 'FEMALE', label: 'Nữ' },
  { value: 'OTHER', label: 'Khác' },
]

interface MedicalProfileFormValues {
  dateOfBirth: Dayjs | null
  gender: Gender | null
  heightCm: number | null
  weightKg: number | null
  medicalHistory: string
  allergies: string[]
  chronicConditions: string[]
  pastSurgeries: string[]
  currentMedications: string[]
  dietaryRestrictions: string[]
}

function formatUpdatedAt(iso: string): string {
  return new Intl.DateTimeFormat('vi-VN', {
    day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit',
  }).format(new Date(iso))
}

export default function MedicalProfilePage() {
  const [form] = Form.useForm<MedicalProfileFormValues>()
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [saveError, setSaveError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)
  // exists + updatedAt tach rieng khoi form (khong phai du lieu nguoi dung go) - dung de
  // hien "Cap nhat lan cuoi" va doi nhan nut Tao/Cap nhat.
  const [exists, setExists] = useState(false)
  const [version, setVersion] = useState<number | null>(null)
  const [updatedAt, setUpdatedAt] = useState<string | null>(null)
  const loadSequence = useRef(0)

  const loadProfile = useCallback(() => {
    const sequence = ++loadSequence.current
    return getMedicalProfile()
      .then((profile) => {
        if (sequence !== loadSequence.current) return
        form.setFieldsValue({
          dateOfBirth: profile.dateOfBirth ? dayjs(profile.dateOfBirth) : null,
          gender: profile.gender,
          heightCm: profile.heightCm,
          weightKg: profile.weightKg,
          medicalHistory: profile.medicalHistory ?? '',
          allergies: profile.allergies,
          chronicConditions: profile.chronicConditions,
          pastSurgeries: profile.pastSurgeries,
          currentMedications: profile.currentMedications,
          dietaryRestrictions: profile.dietaryRestrictions,
        })
        setExists(profile.exists)
        setVersion(profile.version)
        setUpdatedAt(profile.updatedAt)
      })
      .catch((err: unknown) => {
        if (sequence !== loadSequence.current) return
        setLoadError(err instanceof Error ? err.message : 'Không thể tải hồ sơ bệnh lý.')
      })
      .finally(() => {
        if (sequence === loadSequence.current) setLoading(false)
      })
  }, [form])

  useEffect(() => {
    void loadProfile()
    return () => { loadSequence.current += 1 }
  }, [loadProfile])

  function retryLoad() {
    setLoading(true)
    setLoadError(null)
    setSaveError(null)
    setSuccess(null)
    void loadProfile()
  }

  async function onFinish(values: MedicalProfileFormValues) {
    setSaveError(null)
    setSuccess(null)
    setSaving(true)
    try {
      const profile = await updateMedicalProfile({
        version,
        dateOfBirth: values.dateOfBirth ? values.dateOfBirth.format('YYYY-MM-DD') : null,
        gender: values.gender ?? null,
        heightCm: values.heightCm ?? null,
        weightKg: values.weightKg ?? null,
        medicalHistory: values.medicalHistory ?? '',
        allergies: values.allergies ?? [],
        chronicConditions: values.chronicConditions ?? [],
        pastSurgeries: values.pastSurgeries ?? [],
        currentMedications: values.currentMedications ?? [],
        dietaryRestrictions: values.dietaryRestrictions ?? [],
      })
      setExists(profile.exists)
      setVersion(profile.version)
      setUpdatedAt(profile.updatedAt)
      setSuccess('Đã lưu hồ sơ bệnh lý.')
    } catch (err) {
      setSaveError(err instanceof Error ? err.message : 'Không thể lưu hồ sơ bệnh lý.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <AppShell>
      <div className="mx-auto max-w-4xl">
        <div className="mb-6">
          <Text type="secondary">Thông tin sức khỏe</Text>
          <Title level={2} className="mb-1! mt-1!">Hồ sơ bệnh lý cá nhân</Title>
          <Text type="secondary">
            Khai báo thông tin cơ bản, tiền sử bệnh, dị ứng và thuốc đang dùng để trợ lý AI tư vấn chính xác hơn. Đây là thông tin tham khảo, không thay thế chẩn đoán y tế.
          </Text>
        </div>

        <Card className="rounded-2xl border-black/5 shadow-sm">
          {loading ? (
            <div className="flex justify-center py-12">
              <Spin indicator={<LoadingOutlined className="text-teal-600 text-xl" spin />} />
            </div>
          ) : loadError ? (
            <Alert
              type="error"
              message={loadError}
              description="Hồ sơ sẽ không được mở để chỉnh sửa cho tới khi tải thành công."
              showIcon
              action={<Button size="small" onClick={retryLoad}>Thử lại</Button>}
            />
          ) : (
            <>
              {saveError && <Alert type="error" message={saveError} showIcon className="mb-4" />}
              {success && <Alert type="success" message={success} showIcon className="mb-4" />}
              {exists && updatedAt && (
                <Text type="secondary" className="mb-4 block text-xs">
                  Cập nhật lần cuối: {formatUpdatedAt(updatedAt)}
                </Text>
              )}
              <Form<MedicalProfileFormValues> form={form} layout="vertical" onFinish={onFinish} disabled={saving}>
                <div className="grid grid-cols-1 gap-x-4 sm:grid-cols-2">
                  <Form.Item label="Ngày sinh" name="dateOfBirth">
                    <DatePicker
                      className="w-full"
                      format="DD/MM/YYYY"
                      placeholder="Chọn ngày sinh"
                      disabledDate={(d) => !d.isBefore(dayjs(), 'day')}
                    />
                  </Form.Item>
                  <Form.Item label="Giới tính" name="gender">
                    <Select allowClear placeholder="Chọn giới tính" options={GENDER_OPTIONS} />
                  </Form.Item>
                  <Form.Item label="Chiều cao (cm)" name="heightCm">
                    <InputNumber className="w-full" min={30} max={300} placeholder="Vd: 170" />
                  </Form.Item>
                  <Form.Item label="Cân nặng (kg)" name="weightKg">
                    <InputNumber className="w-full" min={1} max={500} placeholder="Vd: 65" />
                  </Form.Item>
                </div>

                <Form.Item label="Bệnh nền đang mắc" name="chronicConditions">
                  <Select mode="tags" tokenSeparators={[',']} notFoundContent="Nhập nội dung rồi nhấn Enter để thêm" placeholder="Gõ tên bệnh rồi nhấn Enter để thêm" />
                </Form.Item>
                <Form.Item label="Tiền sử phẫu thuật" name="pastSurgeries">
                  <Select mode="tags" tokenSeparators={[',']} notFoundContent="Nhập nội dung rồi nhấn Enter để thêm" placeholder="Gõ tên phẫu thuật rồi nhấn Enter để thêm" />
                </Form.Item>
                <Form.Item label="Dị ứng" name="allergies">
                  <Select mode="tags" tokenSeparators={[',']} notFoundContent="Nhập nội dung rồi nhấn Enter để thêm" placeholder="Gõ tên dị ứng rồi nhấn Enter để thêm" />
                </Form.Item>
                <Form.Item label="Thuốc đang dùng" name="currentMedications">
                  <Select mode="tags" tokenSeparators={[',']} notFoundContent="Nhập nội dung rồi nhấn Enter để thêm" placeholder="Gõ tên thuốc rồi nhấn Enter để thêm" />
                </Form.Item>
                <Form.Item label="Chế độ ăn đặc biệt / không dung nạp" name="dietaryRestrictions">
                  <Select mode="tags" tokenSeparators={[',']} notFoundContent="Nhập nội dung rồi nhấn Enter để thêm" placeholder="Vd: không dung nạp lactose, ăn chay..." />
                </Form.Item>
                <Form.Item label="Ghi chú thêm về tiền sử bệnh" name="medicalHistory">
                  <Input.TextArea rows={3} placeholder="Thông tin khác chưa có mục riêng ở trên" />
                </Form.Item>

                <Button type="primary" htmlType="submit" loading={saving}>
                  {exists ? 'Cập nhật hồ sơ' : 'Tạo hồ sơ bệnh lý'}
                </Button>
              </Form>
            </>
          )}
        </Card>
      </div>
    </AppShell>
  )
}
