import { PlusOutlined } from '@ant-design/icons'
import { Button } from 'antd'
import { HealthReportButton } from './HealthReportButton'

interface JournalActionsProps {
  createLabel: string
  onCreate: () => void
}

export function JournalActions({ createLabel, onCreate }: JournalActionsProps) {
  const buttonClass = 'h-12! min-w-0! w-full! justify-center'

  return (
    <div className="grid w-full grid-cols-1 gap-2 min-[480px]:w-[400px] min-[480px]:grid-cols-2 lg:shrink-0">
      <HealthReportButton className={buttonClass} size="large" />
      <Button className={buttonClass} type="primary" size="large" icon={<PlusOutlined />} onClick={onCreate}>
        {createLabel}
      </Button>
    </div>
  )
}
