import { UndoOutlined } from '@ant-design/icons'
import { Alert, Button, Spin, Table, Typography, message } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { useEffect, useState } from 'react'
import { listFoodDiaryTrash, restoreFoodDiaryEntry } from '../../api/foodDiary'
import type { FoodDiaryTrashItem } from '../../api/foodDiary'
import { useInFlightGuard } from '../../hooks/useInFlightGuard'
import { formatDateTime } from '../../lib/format'

const { Text } = Typography

/** UC0020 - so ngay con lai truoc khi bi xoa han, tinh tu purgeAt. Lam tron len (ceil) de
 * "con 0 ngay" chi hien thi dung luc sap het han trong hom nay, khong phai da qua han. */
function daysRemaining(purgeAt: string): number {
  const diffMs = new Date(purgeAt).getTime() - Date.now()
  return Math.max(0, Math.ceil(diffMs / (1000 * 60 * 60 * 24)))
}

interface FoodDiaryTrashTableProps {
  /** Goi khi khoi phuc thanh cong - de trang cha tai lai danh sach nhat ky chinh. */
  onRestored?: () => void
}

export function FoodDiaryTrashTable({ onRestored }: FoodDiaryTrashTableProps) {
  const [items, setItems] = useState<FoodDiaryTrashItem[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const restoreGuard = useInFlightGuard<number>()

  function reload() {
    setLoading(true)
    setError(null)
    listFoodDiaryTrash()
      .then(setItems)
      .catch((err: unknown) => setError(err instanceof Error ? err.message : 'Không thể tải thùng rác.'))
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    reload()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  async function handleRestore(id: number) {
    await restoreGuard.run(id, async () => {
      try {
        await restoreFoodDiaryEntry(id)
        reload()
        onRestored?.()
      } catch (err) {
        // UC0020 - 409 (qua han 30 ngay) hien thi thang message.error, roi tai lai thung rac
        // de dong qua han bien mat (da bi job don dep xoa hoac se som bi xoa).
        message.error(err instanceof Error ? err.message : 'Không thể khôi phục.')
        reload()
      }
    })
  }

  const columns: ColumnsType<FoodDiaryTrashItem> = [
    { title: 'Món ăn', dataIndex: ['entry', 'description'], key: 'description' },
    { title: 'Đã xoá lúc', dataIndex: 'deletedAt', key: 'deletedAt', width: 180, render: formatDateTime },
    {
      title: 'Hạn khôi phục', key: 'purgeAt', width: 140,
      render: (_, record) => {
        const remaining = daysRemaining(record.purgeAt)
        return remaining > 0 ? `Còn ${remaining} ngày` : <Text type="danger">Sắp bị xoá</Text>
      },
    },
    {
      title: '', key: 'actions', width: 100,
      render: (_, record) => (
        <Button
          size="small"
          icon={<UndoOutlined />}
          loading={restoreGuard.inFlightId === record.entry.id}
          disabled={restoreGuard.inFlightId !== null && restoreGuard.inFlightId !== record.entry.id}
          onClick={() => handleRestore(record.entry.id)}
        >
          Khôi phục
        </Button>
      ),
    },
  ]

  return (
    <>
      {error && <Alert type="error" message={error} showIcon className="mb-4" />}
      <Spin spinning={loading}>
        <Table<FoodDiaryTrashItem>
          columns={columns}
          dataSource={items}
          rowKey={(record) => record.entry.id}
          size="small"
          pagination={false}
          locale={{ emptyText: 'Thùng rác trống' }}
        />
      </Spin>
    </>
  )
}