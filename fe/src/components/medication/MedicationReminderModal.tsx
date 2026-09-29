import { Form, Input, Modal, Switch, TimePicker } from 'antd'
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
  timeOfDay: Dayjs
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
      timeOfDay: reminder ? dayjs(reminder.timeOfDay, ['HH:mm:ss', 'HH:mm']) : dayjs('08:00', 'HH:mm'),
      active: reminder?.active ?? true,
    })
  }, [open, reminder, form])

  function handleOk() {
    form.validateFields().then((values) => {
      onSubmit({
        medicineName: values.medicineName,
        dosage: values.dosage || null,
        timeOfDay: values.timeOfDay.format('HH:mm:ss'),
        active: values.active,
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
        <Form.Item label="Tên thuốc" name="medicineName" rules={[{ required: true, message: 'Nhập tên thuốc' }]}>
          <Input placeholder="Vd: Omeprazole" />
        </Form.Item>
        <Form.Item label="Liều lượng" name="dosage">
          <Input placeholder="Vd: 20mg, 1 viên" />
        </Form.Item>
        <Form.Item label="Giờ nhắc (lặp lại hằng ngày)" name="timeOfDay" rules={[{ required: true, message: 'Chọn giờ nhắc' }]}>
          <TimePicker className="w-full" format="HH:mm" />
        </Form.Item>
        <Form.Item label="Đang bật nhắc" name="active" valuePropName="checked">
          <Switch />
        </Form.Item>
      </Form>
    </Modal>
  )
}
