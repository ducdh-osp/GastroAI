import { DeleteOutlined, EditOutlined, PlusOutlined } from '@ant-design/icons'
import { Alert, Button, Card, Pagination, Popconfirm, Spin, Table, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { useEffect, useState } from 'react'
import {
  createFoodDiaryEntry,
  deleteFoodDiaryEntry,
  listFoodDiary,
  updateFoodDiaryEntry,
} from '../../api/foodDiary'
import type { FoodDiaryEntry, FoodDiaryEntryRequest } from '../../api/foodDiary'
import { AppShell } from '../../components/layout/AppShell'
import { FoodDiaryEntryModal } from '../../components/foodDiary/FoodDiaryEntryModal'
import { formatDateTime } from '../../lib/format'

const { Title, Text } = Typography

export default function FoodDiaryPage() {
  const [items, setItems] = useState<FoodDiaryEntry[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [page, setPage] = useState(0)
  const [pageSize] = useState(10)
  const [totalElements, setTotalElements] = useState(0)

  const [modalOpen, setModalOpen] = useState(false)
  const [editingEntry, setEditingEntry] = useState<FoodDiaryEntry | null>(null)
  const [saving, setSaving] = useState(false)

  function reload() {
    setLoading(true)
    setError(null)
    listFoodDiary(page, pageSize)
      .then((res) => {
        setItems(res.items)
        setTotalElements(res.totalElements)
      })
      .catch((err: unknown) => setError(err instanceof Error ? err.message : 'Không thể tải nhật ký ăn uống.'))
      .finally(() => setLoading(false))
  }

  useEffect(reload, [page])

  function openCreate() {
    setEditingEntry(null)
    setModalOpen(true)
  }

  function openEdit(entry: FoodDiaryEntry) {
    setEditingEntry(entry)
    setModalOpen(true)
  }

  async function handleSubmit(payload: FoodDiaryEntryRequest) {
    setSaving(true)
    try {
      if (editingEntry) {
        await updateFoodDiaryEntry(editingEntry.id, payload)
      } else {
        await createFoodDiaryEntry(payload)
      }
      setModalOpen(false)
      reload()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không thể lưu nhật ký ăn uống.')
    } finally {
      setSaving(false)
    }
  }

  async function handleDelete(id: number) {
    try {
      await deleteFoodDiaryEntry(id)
      // Xoá mục cuối cùng còn lại của 1 trang không phải trang đầu -> lùi về trang trước
      // (useEffect tự reload) thay vì để lại trang hiện tại trống, không có lối quay lại.
      if (items.length === 1 && page > 0) {
        setPage(page - 1)
      } else {
        reload()
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không thể xoá nhật ký ăn uống.')
    }
  }

  const columns: ColumnsType<FoodDiaryEntry> = [
    { title: 'Thời điểm ăn', dataIndex: 'eatenAt', key: 'eatenAt', width: 180, render: formatDateTime },
    { title: 'Món ăn', dataIndex: 'description', key: 'description' },
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
            <Title level={2} className="mb-1! mt-1!">Nhật ký ăn uống</Title>
            <Text type="secondary">Ghi lại các bữa ăn để đối chiếu với triệu chứng tiêu hóa sau này.</Text>
          </div>
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>Thêm mục mới</Button>
        </div>

        {error && <Alert type="error" message={error} showIcon className="mb-4" />}

        <Card className="rounded-2xl border-black/5 shadow-sm">
          <Spin spinning={loading}>
            <Table<FoodDiaryEntry>
              columns={columns}
              dataSource={items}
              rowKey="id"
              size="small"
              pagination={false}
              locale={{ emptyText: 'Chưa có mục nào trong nhật ký ăn uống' }}
            />
          </Spin>
          {totalElements > pageSize && (
            <div className="mt-4 flex justify-end">
              <Pagination current={page + 1} pageSize={pageSize} total={totalElements} onChange={(p) => setPage(p - 1)} showSizeChanger={false} />
            </div>
          )}
        </Card>
      </div>

      <FoodDiaryEntryModal
        open={modalOpen}
        entry={editingEntry}
        saving={saving}
        onCancel={() => setModalOpen(false)}
        onSubmit={handleSubmit}
      />
    </AppShell>
  )
}
