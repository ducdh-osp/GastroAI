import { DeleteOutlined, EditOutlined, PlusOutlined } from '@ant-design/icons'
import { Alert, Button, Card, Pagination, Popconfirm, Spin, Table, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { useEffect, useState } from 'react'
import {
  createBristolLog,
  deleteBristolLog,
  listBristolLogs,
  updateBristolLog,
} from '../../api/bristol'
import type { BristolLog, BristolLogRequest } from '../../api/bristol'
import { AppShell } from '../../components/layout/AppShell'
import { BristolLogModal } from '../../components/bristol/BristolLogModal'
import { BRISTOL_TYPE_LABELS } from '../../constants/bristol'
import { formatDateTime } from '../../lib/format'

const { Title, Text } = Typography

export default function BristolPage() {
  const [items, setItems] = useState<BristolLog[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [page, setPage] = useState(0)
  const [pageSize] = useState(10)
  const [totalElements, setTotalElements] = useState(0)

  const [modalOpen, setModalOpen] = useState(false)
  const [editingLog, setEditingLog] = useState<BristolLog | null>(null)
  const [saving, setSaving] = useState(false)

  function reload() {
    setLoading(true)
    setError(null)
    listBristolLogs(page, pageSize)
      .then((res) => {
        setItems(res.items)
        setTotalElements(res.totalElements)
      })
      .catch(() => setError('Không thể tải danh sách đã ghi nhận.'))
      .finally(() => setLoading(false))
  }

  useEffect(reload, [page])

  function openCreate() {
    setEditingLog(null)
    setModalOpen(true)
  }

  function openEdit(log: BristolLog) {
    setEditingLog(log)
    setModalOpen(true)
  }

  async function handleSubmit(payload: BristolLogRequest) {
    setSaving(true)
    try {
      if (editingLog) {
        await updateBristolLog(editingLog.id, payload)
      } else {
        await createBristolLog(payload)
      }
      setModalOpen(false)
      reload()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không thể lưu.')
    } finally {
      setSaving(false)
    }
  }

  async function handleDelete(id: number) {
    try {
      await deleteBristolLog(id)
      if (items.length === 1 && page > 0) {
        setPage(page - 1)
      } else {
        reload()
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không thể xoá.')
    }
  }

  const columns: ColumnsType<BristolLog> = [
    { title: 'Thời điểm', dataIndex: 'loggedAt', key: 'loggedAt', width: 180, render: formatDateTime },
    {
      title: 'Phân loại', dataIndex: 'bristolType', key: 'bristolType', width: 320,
      render: (val: number) => BRISTOL_TYPE_LABELS[val] ?? `Loại ${val}`,
    },
    {
      title: 'Ghi chú', dataIndex: 'notes', key: 'notes',
      render: (val: string | null) => val || <Text type="secondary">—</Text>,
    },
    {
      title: '', key: 'actions', width: 100,
      render: (_, record) => (
        <div className="flex gap-1">
          <Button size="small" type="text" icon={<EditOutlined />} onClick={() => openEdit(record)} />
          <Popconfirm title="Xoá mục này?" okText="Xoá" cancelText="Huỷ" onConfirm={() => handleDelete(record.id)}>
            <Button size="small" type="text" danger icon={<DeleteOutlined />} />
          </Popconfirm>
        </div>
      ),
    },
  ]

  return (
    <AppShell>
      <div className="mx-auto max-w-4xl">
        <div className="mb-6 flex items-center justify-between">
          <div>
            <Text type="secondary">Theo dõi sức khỏe</Text>
            <Title level={2} className="mb-1! mt-1!">Nhật ký Bristol</Title>
            <Text type="secondary">Ghi nhận tình trạng tiêu hóa theo thang Bristol Stool Chart (7 mức).</Text>
          </div>
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>Ghi nhận mới</Button>
        </div>

        {error && <Alert type="error" message={error} showIcon className="mb-4" />}

        <Card className="rounded-2xl border-black/5 shadow-sm">
          <Spin spinning={loading}>
            <Table<BristolLog>
              columns={columns}
              dataSource={items}
              rowKey="id"
              size="small"
              pagination={false}
              locale={{ emptyText: 'Chưa có mục nào được ghi nhận' }}
            />
          </Spin>
          {totalElements > pageSize && (
            <div className="mt-4 flex justify-end">
              <Pagination current={page + 1} pageSize={pageSize} total={totalElements} onChange={(p) => setPage(p - 1)} showSizeChanger={false} />
            </div>
          )}
        </Card>
      </div>

      <BristolLogModal
        open={modalOpen}
        log={editingLog}
        saving={saving}
        onCancel={() => setModalOpen(false)}
        onSubmit={handleSubmit}
      />
    </AppShell>
  )
}
