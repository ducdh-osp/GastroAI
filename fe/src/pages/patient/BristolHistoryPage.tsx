import { Alert, Card, Spin, Table, Tabs, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { useCallback, useEffect, useState } from 'react'
import { getBristolTrend, listBristolLogs } from '../../api/bristol'
import type { BristolLog, BristolLogPoint } from '../../api/bristol'
import { AppShell } from '../../components/layout/AppShell'
import { BristolTrendChart } from '../../components/bristol/BristolTrendChart'
import { BRISTOL_TYPE_LABELS } from '../../constants/bristol'
import { usePagedList } from '../../hooks/usePagedList'
import { formatDateTime } from '../../lib/format'

const { Title, Text } = Typography
const PAGE_SIZE = 10

const columns: ColumnsType<BristolLog> = [
  { title: 'Thời điểm', dataIndex: 'loggedAt', key: 'loggedAt', width: 180, render: formatDateTime },
  {
    title: 'Phân loại', dataIndex: 'bristolType', key: 'bristolType',
    render: (value: number) => BRISTOL_TYPE_LABELS[value] ?? `Loại ${value}`,
  },
  {
    title: 'Ghi chú', dataIndex: 'notes', key: 'notes',
    render: (value: string | null) => value || <Text type="secondary">—</Text>,
  },
]

export default function BristolHistoryPage() {
  const [points, setPoints] = useState<BristolLogPoint[]>([])
  const [trendLoading, setTrendLoading] = useState(true)
  const [trendError, setTrendError] = useState<string | null>(null)
  const fetchHistory = useCallback((page: number, size: number) => listBristolLogs(page, size), [])
  const history = usePagedList({
    fetchPage: fetchHistory,
    pageSize: PAGE_SIZE,
    loadErrorMessage: 'Không thể tải lịch sử Bristol.',
  })

  useEffect(() => {
    let cancelled = false
    getBristolTrend(30)
      .then((trendPoints) => { if (!cancelled) setPoints(trendPoints) })
      .catch((error: unknown) => {
        if (!cancelled) setTrendError(error instanceof Error ? error.message : 'Không thể tải biểu đồ Bristol.')
      })
      .finally(() => { if (!cancelled) setTrendLoading(false) })
    return () => { cancelled = true }
  }, [])

  return (
    <AppShell>
      <div className="mx-auto max-w-5xl">
        <div className="mb-6">
          <Text type="secondary">Theo dõi sức khỏe</Text>
          <Title level={2} className="mb-1! mt-1!">Xu hướng Bristol</Title>
          <Text type="secondary">Xem riêng biểu đồ 30 ngày và lịch sử chi tiết theo thang Bristol Stool Chart.</Text>
        </div>

        <Tabs
          defaultActiveKey="chart"
          type="card"
          items={[
            {
              key: 'chart',
              label: 'Biểu đồ',
              children: (
                <>
                  {trendError && <Alert type="error" message={trendError} showIcon className="mb-4" />}
                  <Card className="rounded-2xl border-black/5 shadow-sm" title="Xu hướng trong 30 ngày gần nhất">
                    <Spin spinning={trendLoading}>
                      {points.length === 0 && !trendLoading ? (
                        <Text type="secondary">Chưa có dữ liệu để hiển thị biểu đồ.</Text>
                      ) : (
                        <BristolTrendChart points={points} />
                      )}
                    </Spin>
                  </Card>
                </>
              ),
            },
            {
              key: 'history',
              label: 'Lịch sử',
              children: (
                <>
                  {history.error && <Alert type="error" message={history.error} showIcon className="mb-4" />}
                  <Card className="rounded-2xl border-black/5 shadow-sm" title="Các lần đã ghi nhận">
                    <Table<BristolLog>
                      columns={columns}
                      dataSource={history.items}
                      rowKey="id"
                      size="small"
                      loading={history.loading}
                      pagination={{
                        current: history.page + 1,
                        pageSize: PAGE_SIZE,
                        total: history.totalElements,
                        showSizeChanger: false,
                        onChange: (nextPage) => history.setPage(nextPage - 1),
                      }}
                      locale={{ emptyText: 'Chưa có mục nào được ghi nhận' }}
                    />
                  </Card>
                </>
              ),
            },
          ]}
        />
      </div>
    </AppShell>
  )
}
