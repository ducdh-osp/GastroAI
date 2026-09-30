import { DeleteOutlined, EditOutlined, PlusOutlined } from '@ant-design/icons'
import { Alert, Button, Card, Pagination, Popconfirm, Spin, Table, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { useState } from 'react'
import {
  createFoodDiaryEntry,
  deleteFoodDiaryEntry,
  listFoodDiary,
  updateFoodDiaryEntry,
} from '../../api/foodDiary'
import type { FoodDiaryEntry, FoodDiaryEntryRequest } from '../../api/foodDiary'
import { AppShell } from '../../components/layout/AppShell'
import { FoodDiaryEntryModal } from '../../components/foodDiary/FoodDiaryEntryModal'
import { useInFlightGuard } from '../../hooks/useInFlightGuard'
import { usePagedList } from '../../hooks/usePagedList'
import { formatDateTime } from '../../lib/format'

const { Title, Text } = Typography

export default function FoodDiaryPage() {
  const {
    items, loading, error, setError, page, setPage, pageSize, totalElements, reload,
  } = usePagedList({
    fetchPage: listFoodDiary,
    pageSize: 10,
    loadErrorMessage: 'Không thể tải nhật ký ăn uống.',
  })

  const [modalOpen, setModalOpen] = useState(false)
  const [editingEntry, setEditingEntry] = useState<FoodDiaryEntry | null>(null)
  const [saving, setSaving] = useState(false)
  const deleteGuard = useInFlightGuard<number>()

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
    await deleteGuard.run(id, async () => {
      try {
        await deleteFoodDiaryEntry(id)
        if (items.length === 1 && page > 0) {
          setPage(page - 1)
        } else {
          void reload()
        }
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Không thể xoá nhật ký ăn uống.')
      }
    })
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
            <Button
              size="small"
              type="text"
              danger
              icon={<DeleteOutlined />}
              loading={deleteGuard.inFlightId === record.id}
              disabled={deleteGuard.inFlightId !== null && deleteGuard.inFlightId !== record.id}
            />
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
