import { Form, Input, Modal, DatePicker, Radio } from 'antd'
import dayjs, { type Dayjs } from 'dayjs'
import { useEffect } from 'react'
import type { BristolLog, BristolLogRequest } from '../../api/bristol'
import { BRISTOL_TYPE_OPTIONS } from '../../constants/bristol'

interface BristolLogModalProps {
  open: boolean
  log: BristolLog | null
  saving: boolean
  onCancel: () => void
  onSubmit: (payload: BristolLogRequest) => void
}

interface BristolLogFormValues {
  loggedAt: Dayjs
  bristolType: number
  notes: string
}

export function BristolLogModal({ open, log, saving, onCancel, onSubmit }: BristolLogModalProps) {
  const [form] = Form.useForm<BristolLogFormValues>()

  useEffect(() => {
    if (!open) return
    form.setFieldsValue({
      loggedAt: log ? dayjs(log.loggedAt) : dayjs(),
      bristolType: log?.bristolType,
      notes: log?.notes ?? '',
    })
  }, [open, log, form])

  function handleOk() {
    form.validateFields().then((values) => {
      onSubmit({
        loggedAt: values.loggedAt.toISOString(),
        bristolType: values.bristolType,
        notes: values.notes || null,
      })
    })
  }

  return (
    <Modal
      title={log ? 'Sửa mục đã ghi nhận' : 'Ghi nhận tình trạng tiêu hóa'}
      open={open}
      onCancel={onCancel}
      onOk={handleOk}
      confirmLoading={saving}
      okText={log ? 'Lưu thay đổi' : 'Ghi nhận'}
      cancelText="Huỷ"
      width={520}
    >
      <Form<BristolLogFormValues> form={form} layout="vertical" className="mt-4">
        <Form.Item label="Thời điểm" name="loggedAt" rules={[{ required: true, message: 'Chọn thời điểm' }]}>
          <DatePicker showTime className="w-full" format="DD/MM/YYYY HH:mm" disabledDate={(d) => d.isAfter(dayjs(), 'day')} />
        </Form.Item>
        <Form.Item label="Phân loại theo thang Bristol" name="bristolType" rules={[{ required: true, message: 'Chọn 1 mức' }]}>
          <Radio.Group style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
            {BRISTOL_TYPE_OPTIONS.map((opt) => (
              <Radio key={opt.value} value={opt.value}>{opt.label}</Radio>
            ))}
          </Radio.Group>
        </Form.Item>
        <Form.Item label="Ghi chú" name="notes" rules={[{ max: 5000, message: 'Ghi chú tối đa 5000 ký tự' }]}>
          <Input.TextArea rows={2} maxLength={5000} showCount placeholder="Vd: kèm đau bụng, sau khi ăn..." />
        </Form.Item>
      </Form>
    </Modal>
  )
}
