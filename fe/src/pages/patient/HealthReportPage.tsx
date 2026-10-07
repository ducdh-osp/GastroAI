import { FilePdfOutlined } from '@ant-design/icons'
import { Alert, Button, Card, DatePicker, Typography } from 'antd'
import dayjs, { type Dayjs } from 'dayjs'
import { useState } from 'react'
import { downloadHealthReport } from '../../api/healthReport'
import { AppShell } from '../../components/layout/AppShell'

const { RangePicker } = DatePicker
const { Title, Text } = Typography

const DATE_FORMAT = 'DD/MM/YYYY'
const MAX_RANGE_DAYS = 90

export default function HealthReportPage() {
  const [range, setRange] = useState<[Dayjs, Dayjs] | null>([dayjs().subtract(29, 'day'), dayjs()])
  const [error, setError] = useState<string | null>(null)
  const [downloading, setDownloading] = useState(false)

  // Chi cho chon ngay hom nay tro ve truoc (khong cho xuat du lieu tuong lai) - khop voi
  // validateRange() ben HealthReportService.
  function disabledDate(date: Dayjs) {
    return date.isAfter(dayjs(), 'day')
  }

  async function handleDownload() {
    if (!range) {
      setError('Vui lòng chọn khoảng thời gian.')
      return
    }
    const [from, to] = range
    // Kiem tra truoc o phia FE de bao loi ngay, khong can doi goi API roi moi bao - BE van se
    // kiem tra lai (validateRange trong HealthReportService) neu ai do bo qua FE goi API truc tiep.
    if (to.diff(from, 'day') + 1 > MAX_RANGE_DAYS) {
      setError(`Chỉ xuất tối đa ${MAX_RANGE_DAYS} ngày mỗi lần.`)
      return
    }

    setError(null)
    setDownloading(true)
    try {
      await downloadHealthReport(from.format('YYYY-MM-DD'), to.format('YYYY-MM-DD'))
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không thể tải nhật ký sức khỏe. Vui lòng thử lại.')
    } finally {
      setDownloading(false)
    }
  }

  return (
    <AppShell>
      <div className="mx-auto max-w-4xl">
        <div className="mb-6">
          <Text type="secondary">Theo dõi sức khỏe</Text>
          <Title level={2} className="mb-1! mt-1!">Xuất nhật ký sức khỏe</Title>
          <Text type="secondary">
            Chọn khoảng thời gian (tối đa {MAX_RANGE_DAYS} ngày) để xuất file PDF gồm hồ sơ bệnh lý,
            nhật ký ăn uống, nhật ký Bristol và thuốc đang nhắc, mang theo khi đi khám.
          </Text>
        </div>

        {error && <Alert type="error" message={error} showIcon className="mb-4" closable onClose={() => setError(null)} />}

        <Card className="rounded-2xl border-black/5 shadow-sm">
          <div className="flex flex-wrap items-end gap-3">
            <div>
              <Text className="mb-1 block text-sm font-medium">Khoảng thời gian</Text>
              <RangePicker
                value={range}
                onChange={(values) => setRange(values as [Dayjs, Dayjs] | null)}
                format={DATE_FORMAT}
                disabledDate={disabledDate}
                presets={[
                  { label: '7 ngày qua', value: [dayjs().subtract(6, 'day'), dayjs()] },
                  { label: '30 ngày qua', value: [dayjs().subtract(29, 'day'), dayjs()] },
                  { label: '90 ngày qua', value: [dayjs().subtract(89, 'day'), dayjs()] },
                ]}
              />
            </div>
            <Button type="primary" icon={<FilePdfOutlined />} loading={downloading} onClick={handleDownload}>
              Tải PDF
            </Button>
          </div>
        </Card>
      </div>
    </AppShell>
  )
}