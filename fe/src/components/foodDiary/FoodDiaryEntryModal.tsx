import { Form, Input, Modal, DatePicker, InputNumber, Select } from 'antd'
import dayjs, { type Dayjs } from 'dayjs'
import { useEffect } from 'react'
import type { FoodDiaryEntry, FoodDiaryEntryRequest } from '../../api/foodDiary'

interface FoodDiaryEntryModalProps {
  open: boolean
  entry: FoodDiaryEntry | null
  saving: boolean
  onCancel: () => void
  onSubmit: (payload: FoodDiaryEntryRequest) => void
}

interface FoodDiaryEntryFormValues {
  eatenAt: Dayjs
  description: string
  mealType: 'BREAKFAST' | 'LUNCH' | 'DINNER' | 'SNACK' | 'OTHER'
  symptomsAfterMeal: string
  symptomOnsetMinutes: number | null
  notes: string
}

export function FoodDiaryEntryModal({ open, entry, saving, onCancel, onSubmit }: FoodDiaryEntryModalProps) {
  const [form] = Form.useForm<FoodDiaryEntryFormValues>()

  useEffect(() => {
    if (!open) return
    form.setFieldsValue({
      eatenAt: entry ? dayjs(entry.eatenAt) : dayjs(),
      description: entry?.description ?? '',
      mealType: entry?.mealType ?? 'OTHER',
      symptomsAfterMeal: entry?.symptomsAfterMeal ?? '',
      symptomOnsetMinutes: entry?.symptomOnsetMinutes ?? null,
      notes: entry?.notes ?? '',
    })
  }, [open, entry, form])

  function handleOk() {
    form.validateFields().then((values) => {
      onSubmit({
        eatenAt: values.eatenAt.toISOString(),
        description: values.description,
        mealType: values.mealType,
        symptomsAfterMeal: values.symptomsAfterMeal || null,
        symptomOnsetMinutes: values.symptomOnsetMinutes ?? null,
        notes: values.notes || null,
      })
    })
  }

  return (
    <Modal
      title={entry ? 'Sửa nhật ký ăn uống' : 'Thêm nhật ký ăn uống'}
      open={open}
      onCancel={onCancel}
      onOk={handleOk}
      confirmLoading={saving}
      okText={entry ? 'Lưu thay đổi' : 'Thêm'}
      cancelText="Huỷ"
    >
      <Form<FoodDiaryEntryFormValues> form={form} layout="vertical" className="mt-4">
        <Form.Item label="Thời điểm ăn" name="eatenAt" rules={[{ required: true, message: 'Chọn thời điểm ăn' }]}>
          <DatePicker showTime className="w-full" format="DD/MM/YYYY HH:mm" disabledDate={(d) => d.isAfter(dayjs(), 'day')} />
        </Form.Item>
        <Form.Item label="Món ăn" name="description" rules={[{ required: true, message: 'Nhập món đã ăn' }, { max: 500, message: 'Món ăn tối đa 500 ký tự' }]}>
          <Input maxLength={500} showCount placeholder="Vd: Phở bò, cơm gà..." />
        </Form.Item>
        <Form.Item label="Phân loại bữa" name="mealType" rules={[{ required: true, message: 'Chọn loại bữa' }]}>
          <Select options={[{ value: 'BREAKFAST', label: 'Bữa sáng' }, { value: 'LUNCH', label: 'Bữa trưa' }, { value: 'DINNER', label: 'Bữa tối' }, { value: 'SNACK', label: 'Bữa phụ' }, { value: 'OTHER', label: 'Khác' }]} />
        </Form.Item>
        <Form.Item label="Triệu chứng sau ăn" name="symptomsAfterMeal" rules={[{ max: 1000, message: 'Triệu chứng tối đa 1000 ký tự' }]}>
          <Input.TextArea rows={2} maxLength={1000} showCount placeholder="Vd: đầy bụng, đau bụng, tiêu chảy..." />
        </Form.Item>
        <Form.Item label="Thời gian khởi phát (phút)" name="symptomOnsetMinutes">
          <InputNumber min={0} max={10080} className="w-full" placeholder="Để trống nếu không ghi nhận" />
        </Form.Item>
        <Form.Item label="Ghi chú" name="notes" rules={[{ max: 5000, message: 'Ghi chú tối đa 5000 ký tự' }]}>
          <Input.TextArea rows={3} maxLength={5000} showCount placeholder="Vd: cảm giác sau khi ăn, khẩu phần..." />
        </Form.Item>
      </Form>
    </Modal>
  )
}
