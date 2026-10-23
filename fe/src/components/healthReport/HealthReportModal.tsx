import { FilePdfOutlined } from '@ant-design/icons'
import { Alert, DatePicker, Modal, Typography } from 'antd'
import dayjs, { type Dayjs } from 'dayjs'
import { useState } from 'react'
import { downloadHealthReport } from '../../api/healthReport'

const { RangePicker } = DatePicker
const { Text } = Typography

const DATE_FORMAT = 'DD/MM/YYYY'
const MAX_RANGE_DAYS = 90

function vnToday(): Dayjs {
  return dayjs(dayjs().tz('Asia/Ho_Chi_Minh').format('YYYY-MM-DD'))
}

interface HealthReportModalProps {
  open: boolean
  onClose: () => void
}

/** UC0019 - xuat nhat ky suc khoe (ho so benh ly + an uong + Bristol + thuoc) ra PDF de mang di kham. */
export function HealthReportModal({ open, onClose }: HealthReportModalProps) {
  const [range, setRange] = useState<[Dayjs, Dayjs] | null>([vnToday().subtract(29, 'day'), vnToday()])
  const [error, setError] = useState<string | null>(null)
  const [downloading, setDownloading] = useState(false)

  // Chi cho chon ngay hom nay tro ve truoc - khop voi validateRange() ben HealthReportService.
  function disabledDate(date: Dayjs) {
    return date.isAfter(vnToday(), 'day')
  }

  function handleClose() {
    if (downloading) return
    setError(null)
    onClose()
  }

  async function handleDownload() {
    if (!range) {
      setError('Vui lòng chọn khoảng thời gian.')
      return
    }
    const [from, to] = range
    if (to.startOf('day').diff(from.startOf('day'), 'day') + 1 > MAX_RANGE_DAYS) {
      setError(`Chỉ xuất tối đa ${MAX_RANGE_DAYS} ngày mỗi lần.`)
      return
    }

    setError(null)
    setDownloading(true)
    try {
      await downloadHealthReport(from.format('YYYY-MM-DD'), to.format('YYYY-MM-DD'))
      onClose()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không thể tải nhật ký sức khỏe. Vui lòng thử lại.')
    } finally {
      setDownloading(false)
    }
  }

  return (
    <Modal
      title="Xuất báo cáo cho bác sĩ"
      open={open}
      onOk={handleDownload}
      onCancel={handleClose}
      okText="Tải PDF"
      cancelText="Hủy"
      okButtonProps={{ icon: <FilePdfOutlined /> }}
      cancelButtonProps={{ disabled: downloading }}
      confirmLoading={downloading}
      destroyOnHidden
    >
      {error && <Alert type="error" title={error} showIcon className="mb-4" />}
      <Text className="mb-1 block text-sm font-medium">Khoảng thời gian (tối đa {MAX_RANGE_DAYS} ngày)</Text>
      <RangePicker
        className="w-full"
        value={range}
        onChange={(values) => setRange(values as [Dayjs, Dayjs] | null)}
        format={DATE_FORMAT}
        disabledDate={disabledDate}
        presets={[
          { label: '7 ngày qua', value: [vnToday().subtract(6, 'day'), vnToday()] },
          { label: '30 ngày qua', value: [vnToday().subtract(29, 'day'), vnToday()] },
          { label: '90 ngày qua', value: [vnToday().subtract(89, 'day'), vnToday()] },
        ]}
      />
    </Modal>
  )
}