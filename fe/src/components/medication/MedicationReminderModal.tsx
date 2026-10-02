import { Alert, DatePicker, Form, Input, Modal, Select, Switch } from 'antd'
import dayjs, { type Dayjs } from 'dayjs'
import { useEffect } from 'react'
import type { MedicationReminder, MedicationReminderRequest } from '../../api/medication'

interface MedicationReminderModalProps {
  open: boolean
  reminder: MedicationReminder | null
  saving: boolean
  onCancel: () => void
  onSubmit: (payload: MedicationReminderRequest) => void
}

interface MedicationReminderFormValues {
  medicineName: string
  dosage: string
  timesOfDay: string[]
  treatmentDates: [Dayjs, Dayjs] | null
  instructions: string
  active: boolean
}

export function MedicationReminderModal({ open, reminder, saving, onCancel, onSubmit }: MedicationReminderModalProps) {
  const [form] = Form.useForm<MedicationReminderFormValues>()

  useEffect(() => {
    if (!open) return
    form.setFieldsValue({
      medicineName: reminder?.medicineName ?? '',
      dosage: reminder?.dosage ?? '',
      // BE co the tra "HH:mm" hoac "HH:mm:ss" tuy LocalTime co giay hay khong - thu ca 2.
      timesOfDay: reminder ? reminder.timesOfDay.map((time) => time.slice(0, 5)) : ['08:00'],
      treatmentDates: reminder?.startDate && reminder?.endDate
        ? [dayjs(reminder.startDate), dayjs(reminder.endDate)]
        : null,
      instructions: reminder?.instructions ?? '',
      active: reminder?.active ?? true,
    })
  }, [open, reminder, form])

  function handleOk() {
    form.validateFields().then((values) => {
      onSubmit({
        medicineName: values.medicineName,
        dosage: values.dosage || null,
        timesOfDay: values.timesOfDay.map((time) => `${time}:00`),
        startDate: values.treatmentDates?.[0].format('YYYY-MM-DD') ?? null,
        endDate: values.treatmentDates?.[1].format('YYYY-MM-DD') ?? null,
        instructions: values.instructions || null,
        active: values.active,
        version: reminder?.version ?? null,
      })
    })
  }

  return (
    <Modal
      title={reminder ? 'Sửa lịch nhắc thuốc' : 'Thêm lịch nhắc thuốc'}
      open={open}
      onCancel={onCancel}
      onOk={handleOk}
      confirmLoading={saving}
      okText={reminder ? 'Lưu thay đổi' : 'Thêm'}
      cancelText="Huỷ"
    >
      <Form<MedicationReminderFormValues> form={form} layout="vertical" className="mt-4">
        <Alert
          className="mb-4"
          type="info"
          showIcon
          message="Chỉ tạo lịch cho thuốc/liều cần được nhắc"
          description="Lịch này được quản lý độc lập với danh sách thuốc không cần lịch nhắc trong Hồ sơ bệnh lý."
        />
        <Form.Item label="Tên thuốc" name="medicineName" rules={[{ required: true, message: 'Nhập tên thuốc' }, { max: 200, message: 'Tên thuốc tối đa 200 ký tự' }]}>
          <Input maxLength={200} showCount placeholder="Vd: Omeprazole" />
        </Form.Item>
        <Form.Item label="Liều lượng" name="dosage" rules={[{ max: 200, message: 'Liều lượng tối đa 200 ký tự' }]}>
          <Input maxLength={200} showCount placeholder="Vd: 20mg, 1 viên" />
        </Form.Item>
        <Form.Item label="Các giờ uống trong ngày" name="timesOfDay" rules={[{ required: true, message: 'Chọn ít nhất một giờ nhắc' }]}>
          <Select mode="multiple" className="w-full" options={Array.from({ length: 96 }, (_, index) => {
            const hour = Math.floor(index / 4).toString().padStart(2, '0')
            const minute = ((index % 4) * 15).toString().padStart(2, '0')
            const value = `${hour}:${minute}`
            return { value, label: value }
          })} maxTagCount="responsive" />
        </Form.Item>
        <Form.Item label="Thời gian điều trị" name="treatmentDates">
          <DatePicker.RangePicker className="w-full" format="DD/MM/YYYY" />
        </Form.Item>
        <Form.Item label="Hướng dẫn dùng thuốc" name="instructions" rules={[{ max: 1000, message: 'Hướng dẫn tối đa 1000 ký tự' }]}>
          <Input.TextArea rows={2} maxLength={1000} showCount placeholder="Vd: uống trước ăn sáng 30 phút" />
        </Form.Item>
        <Form.Item label="Đang bật nhắc" name="active" valuePropName="checked">
          <Switch />
        </Form.Item>
      </Form>
    </Modal>
  )
}
