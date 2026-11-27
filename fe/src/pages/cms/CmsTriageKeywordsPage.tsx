import { DeleteOutlined, EditOutlined, InfoCircleOutlined, PlusOutlined, TagsOutlined } from '@ant-design/icons'
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
  const countByGroup = keywords.reduce<Record<string, number>>((acc, item) => {
    acc[item.groupCode] = (acc[item.groupCode] ?? 0) + 1
    return acc
  }, {})

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
      title: 'Thao tác',
      key: 'actions',
      width: 100,
      align: 'center',
      render: (_, record) => (
        <div className="flex justify-center gap-1">
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
      <div className="flex w-full flex-col gap-5">
        <div className="flex flex-wrap items-end justify-between gap-3">
          <div>
            <Text type="secondary">Cấu hình</Text>
            <Title level={2} className="mb-1! mt-1!">
              <TagsOutlined className="mr-2" />
              Từ khóa Triage
            </Title>
            <Text type="secondary">
              Quản lý bộ từ khóa và cụm từ giúp AI nhận diện sớm dấu hiệu cấp cứu tiêu hóa trong cuộc trò chuyện của người bệnh và kích hoạt hướng dẫn gọi 115.
            </Text>
          </div>
          <div className="flex flex-wrap gap-2">
            <Select
              allowClear
              placeholder="Lọc theo nhóm dấu hiệu"
              style={{ width: 240 }}
              value={groupFilter}
              onChange={(val) => setGroupFilter(val ?? null)}
              options={groups.map((g) => ({ value: g.code, label: g.label }))}
            />
            <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>Thêm từ khóa</Button>
          </div>
        </div>

        <Card
          size="small"
          className="rounded-2xl border-black/5 shadow-sm dark:border-white/10! dark:bg-neutral-900!"
          title={
            <span className="flex items-center gap-2 text-sm font-semibold">
              <InfoCircleOutlined className="text-teal-600" />
              Nhóm dấu hiệu cảnh báo cố định ({groups.length} nhóm theo chuẩn cấp cứu y khoa)
            </span>
          }
        >
          <Text type="secondary" className="mb-3! block text-xs">
            Các nhóm dấu hiệu cấp cứu được cấu hình cố định theo bộ quy tắc sàng lọc khẩn cấp (NHS 111 &amp; Mayo Clinic) của hệ thống để đảm bảo an toàn y tế; quản trị viên không tự tạo/xóa nhóm mà quản lý danh sách từ khóa nhận diện bên trong từng nhóm. Nhấn vào một nhóm để lọc nhanh:
          </Text>
          <div className="flex flex-wrap gap-2">
            <Button
              size="small"
              type={groupFilter === null ? 'primary' : 'default'}
              className="rounded-full"
              onClick={() => setGroupFilter(null)}
            >
              Tất cả nhóm ({keywords.length})
            </Button>
            {groups.map((group) => (
              <Button
                key={group.code}
                size="small"
                type={groupFilter === group.code ? 'primary' : 'default'}
                className="rounded-full"
                onClick={() => setGroupFilter(groupFilter === group.code ? null : group.code)}
              >
                {group.label} ({countByGroup[group.code] ?? 0})
              </Button>
            ))}
          </div>
        </Card>

        {error && <Alert type="error" message={error} showIcon closable onClose={() => setError(null)} />}

        <Card className="rounded-2xl border-black/5 shadow-sm dark:border-white/10! dark:bg-neutral-900!">
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
        cancelText="Huỷ"
        confirmLoading={saving}
        forceRender
        destroyOnHidden
      >
        {formError && <Alert type="error" title={formError} showIcon className="mb-4!" />}
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
