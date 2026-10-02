import { Alert, Card, Spin, Table, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { useEffect, useRef, useState } from 'react'
import { getBristolTrend, listBristolLogs } from '../../api/bristol'
import type { BristolLog, BristolLogPoint } from '../../api/bristol'
import { AppShell } from '../../components/layout/AppShell'
import { BristolTrendChart } from '../../components/bristol/BristolTrendChart'
import { BRISTOL_TYPE_LABELS } from '../../constants/bristol'
import { formatDateTime } from '../../lib/format'

const { Title, Text } = Typography

const columns: ColumnsType<BristolLog> = [
  { title: 'Thời điểm', dataIndex: 'loggedAt', key: 'loggedAt', width: 180, render: formatDateTime },
  {
    title: 'Phân loại', dataIndex: 'bristolType', key: 'bristolType',
    render: (val: number) => BRISTOL_TYPE_LABELS[val] ?? `Loại ${val}`,
  },
]

export default function BristolHistoryPage() {
  const [points, setPoints] = useState<BristolLogPoint[]>([])
  const [recentLogs, setRecentLogs] = useState<BristolLog[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const requestId = useRef(0)

  useEffect(() => {
    const currentRequestId = ++requestId.current
    setLoading(true)
    setError(null)
    Promise.all([getBristolTrend(30), listBristolLogs(0, 10)])
      .then(([trendPoints, recent]) => {
        if (currentRequestId !== requestId.current) return
        setPoints(trendPoints)
        setRecentLogs(recent.items)
      })
      .catch((err: unknown) => {
        if (currentRequestId !== requestId.current) return
        setError(err instanceof Error ? err.message : 'Không thể tải xu hướng Bristol.')
      })
      .finally(() => {
        if (currentRequestId === requestId.current) setLoading(false)
      })
  }, [])

  return (
    <AppShell>
      <div className="mx-auto max-w-4xl">
        <div className="mb-6">
          <Text type="secondary">Theo dõi sức khỏe</Text>
          <Title level={2} className="mb-1! mt-1!">Xu hướng Bristol</Title>
          <Text type="secondary">Mỗi lần ghi nhận trong 30 ngày gần nhất, theo thang Bristol Stool Chart.</Text>
        </div>

        {error && <Alert type="error" message={error} showIcon className="mb-4" />}

        <Card className="mb-6 rounded-2xl border-black/5 shadow-sm">
          <Spin spinning={loading}>
            {points.length === 0 && !loading ? (
              <Text type="secondary">Chưa có dữ liệu để hiển thị biểu đồ.</Text>
            ) : (
              <BristolTrendChart points={points} />
            )}
          </Spin>
        </Card>

        <Card className="rounded-2xl border-black/5 shadow-sm" title="Các mục gần đây">
          <Spin spinning={loading}>
            <Table<BristolLog>
              columns={columns}
              dataSource={recentLogs}
              rowKey="id"
              size="small"
              pagination={false}
              locale={{ emptyText: 'Chưa có mục nào được ghi nhận' }}
            />
          </Spin>
        </Card>
      </div>
    </AppShell>
  )
}
