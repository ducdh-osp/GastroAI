import { DeleteOutlined, EditOutlined, PlusOutlined } from '@ant-design/icons'
import { Alert, Button, Card, Form, Input, InputNumber, Modal, Popconfirm, Table, Tooltip, Typography, message } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { useCallback, useEffect, useRef, useState } from 'react'
import {
  createArticleTopic,
  deleteArticleTopic,
  extractArticleTopicError,
  listArticleTopics,
  updateArticleTopic,
} from '../../api/cmsArticleTopics'
import type { ArticleTopic } from '../../api/cmsArticleTopics'
import { CmsAppShell } from '../../components/cms/CmsAppShell'

const { Title, Text } = Typography

interface TopicFormValues {
  name: string
  sortOrder?: number | null
}

/** UC0066 - Admin/Bác sĩ quản lý chủ đề dùng để phân loại bài viết kiến thức (và FAQ sau này). */
export default function CmsArticleTopicsPage() {
  const [topics, setTopics] = useState<ArticleTopic[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [editing, setEditing] = useState<ArticleTopic | null>(null)
  const [modalOpen, setModalOpen] = useState(false)
  const [saving, setSaving] = useState(false)
  const [deletingId, setDeletingId] = useState<number | null>(null)
  const [form] = Form.useForm<TopicFormValues>()
  const requestId = useRef(0)

  const load = useCallback(() => {
    const currentRequestId = ++requestId.current
    setLoading(true)
    setError(null)
    listArticleTopics()
      .then((data) => {
        if (currentRequestId === requestId.current) setTopics(data)
      })
      .catch((err: unknown) => {
        if (currentRequestId === requestId.current) {
          setError(extractArticleTopicError(err, 'Không thể tải danh sách chủ đề.'))
        }
      })
      .finally(() => {
        if (currentRequestId === requestId.current) setLoading(false)
      })
  }, [])

  useEffect(() => {
    load()
  }, [load])

  function openCreate() {
    setEditing(null)
    form.resetFields()
    setModalOpen(true)
  }

  function openEdit(topic: ArticleTopic) {
    setEditing(topic)
    form.setFieldsValue({ name: topic.name, sortOrder: topic.sortOrder })
    setModalOpen(true)
  }

  async function handleSubmit(values: TopicFormValues) {
    setSaving(true)
    try {
      if (editing) {
        await updateArticleTopic(editing.id, values)
        message.success('Đã cập nhật chủ đề.')
      } else {
        await createArticleTopic(values)
        message.success('Đã thêm chủ đề.')
      }
      setModalOpen(false)
      load()
    } catch (err: unknown) {
      message.error(extractArticleTopicError(err, 'Không thể lưu chủ đề. Vui lòng thử lại.'))
    } finally {
      setSaving(false)
    }
  }

  async function handleDelete(topic: ArticleTopic) {
    setDeletingId(topic.id)
    try {
      await deleteArticleTopic(topic.id)
      message.success('Đã xoá chủ đề.')
      load()
    } catch (err: unknown) {
      message.error(extractArticleTopicError(err, 'Không thể xoá chủ đề. Vui lòng thử lại.'))
    } finally {
      setDeletingId(null)
    }
  }

  const columns: ColumnsType<ArticleTopic> = [
    { title: 'Tên chủ đề', dataIndex: 'name', key: 'name' },
    { title: 'Thứ tự', dataIndex: 'sortOrder', key: 'sortOrder', width: 100 },
    { title: 'Số bài viết', dataIndex: 'articleCount', key: 'articleCount', width: 130 },
    {
      title: 'Thao tác',
      key: 'actions',
      width: 130,
      render: (_, topic) => (
        <div className="flex gap-1">
          <Tooltip title="Sửa">
            <Button type="text" icon={<EditOutlined />} aria-label={`Sửa chủ đề ${topic.name}`} onClick={() => openEdit(topic)} />
          </Tooltip>
          <Popconfirm
            title="Xoá chủ đề này?"
            description={
              topic.articleCount > 0
                ? `Còn ${topic.articleCount} bài viết đang dùng, hệ thống sẽ không cho xoá.`
                : 'Thao tác không thể hoàn tác.'
            }
            okText="Xoá"
            cancelText="Huỷ"
            okButtonProps={{ danger: true }}
            onConfirm={() => handleDelete(topic)}
          >
            <Button
              type="text"
              danger
              icon={<DeleteOutlined />}
              loading={deletingId === topic.id}
              aria-label={`Xoá chủ đề ${topic.name}`}
            />
          </Popconfirm>
        </div>
      ),
    },
  ]

  return (
    <CmsAppShell>
      <div className="mx-auto max-w-4xl">
        <div className="mb-6 flex flex-wrap items-end justify-between gap-3">
          <div>
            <Text type="secondary">Kho tri thức</Text>
            <Title level={2} className="mb-1! mt-1!">Chủ đề bài viết</Title>
            <Text type="secondary">
              Nhóm chủ đề (vd: dạ dày, đại tràng, chế độ ăn) dùng để phân loại bài viết kiến thức.
              Đổi tên một chủ đề sẽ cập nhật theo các bài viết đang dùng nó.
            </Text>
          </div>
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
            Thêm chủ đề
          </Button>
        </div>

        {error && <Alert type="error" message={error} showIcon className="mb-4" />}

        <Card className="rounded-2xl border-black/5 shadow-sm">
          <Table<ArticleTopic>
            columns={columns}
            dataSource={topics}
            rowKey="id"
            loading={loading}
            pagination={false}
            locale={{ emptyText: 'Chưa có chủ đề nào' }}
          />
        </Card>

        <Modal
          title={editing ? 'Sửa chủ đề' : 'Thêm chủ đề'}
          open={modalOpen}
          onCancel={() => setModalOpen(false)}
          onOk={() => form.submit()}
          okText="Lưu"
          cancelText="Huỷ"
          confirmLoading={saving}
          destroyOnHidden
        >
          <Form<TopicFormValues> form={form} layout="vertical" onFinish={handleSubmit} className="mt-4">
            <Form.Item
              name="name"
              label="Tên chủ đề"
              rules={[
                { required: true, whitespace: true, message: 'Vui lòng nhập tên chủ đề' },
                { max: 80, message: 'Tối đa 80 ký tự' },
              ]}
            >
              <Input placeholder="Ví dụ: Dạ dày" maxLength={80} autoFocus />
            </Form.Item>
            <Form.Item
              name="sortOrder"
              label="Thứ tự hiển thị"
              extra="Số nhỏ hiển thị trước. Để trống sẽ xếp cuối danh sách."
            >
              <InputNumber min={0} precision={0} className="w-full" />
            </Form.Item>
          </Form>
        </Modal>
      </div>
    </CmsAppShell>
  )
}
