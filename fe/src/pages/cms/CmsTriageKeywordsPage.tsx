import { DeleteOutlined, EditOutlined, PlusOutlined, TagsOutlined } from '@ant-design/icons'
import { Alert, Button, Card, Form, Input, Modal, Popconfirm, Select, Spin, Table, Tag, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { useEffect, useState } from 'react'
import {
  createTriageKeyword,
  deleteTriageKeyword,
  listTriageKeywordGroups,
  listTriageKeywords,
  updateTriageKeyword,
} from '../../api/cmsTriageKeywords'
import type { TriageKeyword, TriageKeywordGroup } from '../../api/cmsTriageKeywords'
import { CmsAppShell } from '../../components/cms/CmsAppShell'
import { useInFlightGuard } from '../../hooks/useInFlightGuard'
import { formatDateTime } from '../../lib/format'

const { Title, Text } = Typography

export default function CmsTriageKeywordsPage() {
  const [keywords, setKeywords] = useState<TriageKeyword[]>([])
  const [groups, setGroups] = useState<TriageKeywordGroup[]>([])
  const [groupFilter, setGroupFilter] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState<TriageKeyword | null>(null)
  const [saving, setSaving] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)
  const [form] = Form.useForm<{ groupCode: string; keyword: string }>()
  const deleteGuard = useInFlightGuard<number>()

  function reload() {
    setLoading(true)
    setError(null)
    Promise.all([listTriageKeywords(), listTriageKeywordGroups()])
      .then(([keywordList, groupList]) => {
        setKeywords(keywordList)
        setGroups(groupList)
      })
      .catch((err: unknown) => setError(err instanceof Error ? err.message : 'Không thể tải danh sách từ khóa.'))
      .finally(() => setLoading(false))
  }

  useEffect(reload, [])

  function openCreate() {
    setEditing(null)
    setFormError(null)
    form.resetFields()
    setModalOpen(true)
  }

  function openEdit(keyword: TriageKeyword) {
    setEditing(keyword)
    setFormError(null)
    form.setFieldsValue({ groupCode: keyword.groupCode, keyword: keyword.keyword })
    setModalOpen(true)
  }

  async function handleSubmit() {
    const values = await form.validateFields()
    setSaving(true)
    setFormError(null)
    try {
      if (editing) {
        await updateTriageKeyword(editing.id, values)
      } else {
        await createTriageKeyword(values)
      }
      setModalOpen(false)
      reload()
    } catch (err) {
      setFormError(err instanceof Error ? err.message : 'Không thể lưu từ khóa này.')
    } finally {
      setSaving(false)
    }
  }

  async function handleDelete(keyword: TriageKeyword) {
    await deleteGuard.run(keyword.id, async () => {
      try {
        await deleteTriageKeyword(keyword.id)
        reload()
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Không thể xóa từ khóa này.')
      }
    })
  }

  const visibleKeywords = groupFilter ? keywords.filter((k) => k.groupCode === groupFilter) : keywords

  const columns: ColumnsType<TriageKeyword> = [
    {
      title: 'Nhóm',
      dataIndex: 'groupLabel',
      key: 'groupLabel',
      width: 260,
      render: (label: string) => <Tag>{label}</Tag>,
    },
    { title: 'Từ khóa', dataIndex: 'keyword', key: 'keyword' },
    { title: 'Cập nhật lúc', dataIndex: 'updatedAt', key: 'updatedAt', width: 170, render: formatDateTime },
    {
      title: '',
      key: 'actions',
      width: 100,
      render: (_, record) => (
        <div className="flex gap-1">
          <Button size="small" type="text" icon={<EditOutlined />} onClick={() => openEdit(record)} />
          <Popconfirm
            title="Xóa từ khóa này?"
            description="Chat tự do sẽ không còn nhận diện được câu nói chứa từ khóa này nữa."
            okText="Xóa"
            okButtonProps={{ danger: true }}
            cancelText="Huỷ"
            onConfirm={() => handleDelete(record)}
          >
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
    <CmsAppShell>
      <div className="mx-auto max-w-5xl">
        <div className="mb-6 flex flex-wrap items-end justify-between gap-3">
          <div>
            <Text type="secondary">Cấu hình</Text>
            <Title level={2} className="mb-1! mt-1!">
              <TagsOutlined className="mr-2" />
              Từ khóa Triage
            </Title>
            <Text type="secondary">
              Quản lý các từ khóa/câu nói dùng để nhận diện dấu hiệu cấp cứu trong chat tự do (UC0034). Chỉ có thể
              thêm/sửa/xóa từ khóa trong 1 nhóm cố định - không thể tạo nhóm mới.
            </Text>
          </div>
          <div className="flex gap-2">
            <Select
              allowClear
              placeholder="Lọc theo nhóm"
              style={{ width: 220 }}
              value={groupFilter}
              onChange={setGroupFilter}
              options={groups.map((g) => ({ value: g.code, label: g.label }))}
            />
            <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>Thêm từ khóa</Button>
          </div>
        </div>

        {error && <Alert type="error" message={error} showIcon className="mb-4" closable onClose={() => setError(null)} />}

        <Card className="rounded-2xl border-black/5 shadow-sm">
          <Spin spinning={loading}>
            <Table<TriageKeyword>
              columns={columns}
              dataSource={visibleKeywords}
              rowKey="id"
              size="small"
              pagination={{ pageSize: 20, hideOnSinglePage: true }}
              locale={{ emptyText: 'Chưa có từ khóa nào' }}
            />
          </Spin>
        </Card>
      </div>

      <Modal
        title={editing ? 'Sửa từ khóa' : 'Thêm từ khóa'}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        onOk={handleSubmit}
        okText={editing ? 'Lưu' : 'Thêm'}
        confirmLoading={saving}
        destroyOnHidden
      >
        {formError && <Alert type="error" message={formError} showIcon className="mb-4" />}
        <Form form={form} layout="vertical">
          <Form.Item
            name="groupCode"
            label="Nhóm dấu hiệu"
            rules={[{ required: true, message: 'Chọn 1 nhóm' }]}
          >
            <Select
              placeholder="Chọn nhóm"
              options={groups.map((g) => ({ value: g.code, label: g.label }))}
            />
          </Form.Item>
          <Form.Item
            name="keyword"
            label="Từ khóa / câu nói"
            rules={[{ required: true, message: 'Nhập từ khóa' }]}
            extra="Có dấu hay không dấu đều được - hệ thống tự chuẩn hóa trước khi lưu."
          >
            <Input placeholder="vd: đau bụng dữ dội" />
          </Form.Item>
        </Form>
      </Modal>
    </CmsAppShell>
  )
}
