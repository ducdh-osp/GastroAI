import { FilePdfOutlined } from '@ant-design/icons'
import { Button } from 'antd'
import { useState } from 'react'
import { HealthReportModal } from './HealthReportModal'


export function HealthReportButton() {
  const [open, setOpen] = useState(false)

  return (
    <>
      <Button icon={<FilePdfOutlined />} onClick={() => setOpen(true)}>
        Xuất báo cáo cho bác sĩ
      </Button>
      <HealthReportModal open={open} onClose={() => setOpen(false)} />
    </>
  )
}