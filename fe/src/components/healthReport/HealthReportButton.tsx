import { FilePdfOutlined } from '@ant-design/icons'
import { Button } from 'antd'
import type { ButtonProps } from 'antd'
import { useState } from 'react'
import { HealthReportModal } from './HealthReportModal'

type HealthReportButtonProps = Pick<ButtonProps, 'className' | 'size'>

export function HealthReportButton({ className, size }: HealthReportButtonProps = {}) {
  const [open, setOpen] = useState(false)

  return (
    <>
      <Button className={className} size={size} icon={<FilePdfOutlined />} onClick={() => setOpen(true)}>
        Xuất báo cáo
      </Button>
      <HealthReportModal open={open} onClose={() => setOpen(false)} />
    </>
  )
}
