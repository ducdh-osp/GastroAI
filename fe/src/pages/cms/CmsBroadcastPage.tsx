import {
  BellOutlined,
  EyeOutlined,
  InfoCircleOutlined,
  LinkOutlined,
  ReloadOutlined,
  SendOutlined,
  SoundOutlined,
  UsergroupAddOutlined,
} from '@ant-design/icons'
import {
  Alert,
  Button,
  Card,
  Descriptions,
  Form,
  Input,
  Modal,
  notification,
  Select,
  Space,
  Switch,
  Table,
  Tag,
  Tooltip,
  Typography,
} from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { useCallback, useEffect, useState } from 'react'
import {
  getBroadcastAnnouncements,
  sendBroadcastNotification,
  type BroadcastAnnouncement,
  type SendBroadcastRequest,
} from '../../api/notifications'
import { CmsAppShell } from '../../components/cms/CmsAppShell'
import { extractErrorMessage } from '../../lib/errors'

const { Title, Text } = Typography
const { TextArea } = Input

export function CmsBroadcastPage() {
  const [data, setData] = useState<BroadcastAnnouncement[]>([])
  const [loading, setLoading] = useState(false)
  const [page, setPage] = useState(0)
  const [pageSize, setPageSize] = useState(10)
  const [totalElements, setTotalElements] = useState(0)

  // Modal create
  const [createModalOpen, setCreateModalOpen] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [form] = Form.useForm<SendBroadcastRequest>()

  // Detail modal
  const [selectedItem, setSelectedItem] = useState<BroadcastAnnouncement | null>(null)
  const [detailModalOpen, setDetailModalOpen] = useState(false)

  // Watch form values for live preview
  const previewTitle = Form.useWatch('title', form)
  const previewMessage = Form.useWatch('message', form)

  const loadData = useCallback(async (targetPage = page, targetSize = pageSize) => {
    setLoading(true)
    try {
      const res = await getBroadcastAnnouncements(targetPage, targetSize)
      setData(res.content ?? res.items ?? [])
      setTotalElements(res.totalElements ?? 0)
      setPage(res.number ?? res.page ?? targetPage)
      setPageSize(res.size ?? targetSize)
    } catch (err) {
      notification.error({
        message: 'Lỗi tải danh sách',
        description: extractErrorMessage(err, 'Không thể tải lịch sử thông báo broadcast').message,
      })
    } finally {
      setLoading(false)
    }
  }, [page, pageSize])

  useEffect(() => {
    loadData(0, pageSize)
  }, [loadData, pageSize])

  const handleOpenCreateModal = () => {
    form.resetFields()
    form.setFieldsValue({
      targetGroup: 'ALL',
      sendWebSocket: true,
    })
    setCreateModalOpen(true)
  }

  const handleSendBroadcast = async (values: SendBroadcastRequest) => {
    setSubmitting(true)
    try {
      const created = await sendBroadcastNotification(values)
      notification.success({
        message: 'Gửi broadcast thành công',
        description: `Đã gửi thông báo tới ${created.recipientCount} bệnh nhân.`,
      })
      setCreateModalOpen(false)
      loadData(0, pageSize)
    } catch (err) {
      notification.error({
        message: 'Gửi thất bại',
        description: extractErrorMessage(err, 'Không thể gửi thông báo broadcast').message,
      })
    } finally {
      setSubmitting(false)
    }
  }

  const handleViewDetail = (item: BroadcastAnnouncement) => {
    setSelectedItem(item)
    setDetailModalOpen(true)
  }

  const columns: ColumnsType<BroadcastAnnouncement> = [
    {
      title: 'ID',
      dataIndex: 'id',
      key: 'id',
      width: 70,
      render: (id: number) => <span className="font-mono text-xs text-slate-500">#{id}</span>,
    },
    {
      title: 'Tiêu đề thông báo',
      dataIndex: 'title',
      key: 'title',
      render: (title: string, record: BroadcastAnnouncement) => (
        <div>
          <div className="font-medium text-slate-900">{title}</div>
          <div className="text-xs text-slate-500 line-clamp-1 max-w-md">{record.message}</div>
        </div>
      ),
    },
    {
      title: 'Đối tượng nhận',
      dataIndex: 'targetGroup',
      key: 'targetGroup',
      width: 170,
      render: (aud: string) => {
        if (!aud || aud === 'ALL' || aud === 'ALL_PATIENTS') {
          return <Tag color="blue" icon={<UsergroupAddOutlined />}>Tất cả bệnh nhân</Tag>
        }
        return <Tag color="purple">{aud}</Tag>
      },
    },
    {
      title: 'Số người nhận',
      dataIndex: 'recipientCount',
      key: 'recipientCount',
      width: 130,
      align: 'right',
      render: (count: number) => (
        <span className="font-semibold text-emerald-600 bg-emerald-50 px-2 py-0.5 rounded-full border border-emerald-200 text-xs">
          {count.toLocaleString('vi-VN')} người
        </span>
      ),
    },
    {
      title: 'Thời gian gửi',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 170,
      render: (dateStr: string) => (
        <span className="text-xs text-slate-600">
          {new Intl.DateTimeFormat('vi-VN', {
            dateStyle: 'short',
            timeStyle: 'short',
          }).format(new Date(dateStr))}
        </span>
      ),
    },
    {
      title: 'Thao tác',
      key: 'actions',
      width: 100,
      align: 'center',
      render: (_: unknown, record: BroadcastAnnouncement) => (
        <Tooltip title="Xem chi tiết">
          <Button
            type="text"
            icon={<EyeOutlined />}
            size="small"
            onClick={() => handleViewDetail(record)}
          />
        </Tooltip>
      ),
    },
  ]

  return (
    <CmsAppShell>
      <div className="space-y-6">
        {/* Header section */}
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <div>
            <div className="flex items-center gap-3">
              <div className="p-2.5 rounded-xl bg-teal-50 border border-teal-100 text-teal-600 text-xl flex items-center justify-center">
                <SoundOutlined />
              </div>
              <div>
                <Title level={3} className="!mb-0 text-slate-900">
                  Thông báo Broadcast
                </Title>
                <Text type="secondary" className="text-xs">
                  Soạn và gửi thông báo hàng loạt tới người dùng (UC0075)
                </Text>
              </div>
            </div>
          </div>
          <Space>
            <Button
              icon={<ReloadOutlined />}
              onClick={() => loadData(page, pageSize)}
              loading={loading}
            >
              Làm mới
            </Button>
            <Button
              type="primary"
              icon={<SendOutlined />}
              onClick={handleOpenCreateModal}
              className="bg-teal-600 hover:bg-teal-500 shadow-sm"
            >
              Soạn thông báo mới
            </Button>
          </Space>
        </div>

        {/* Stats banner */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <Card className="rounded-xl border-slate-200/80 shadow-xs">
            <div className="text-xs text-slate-500 font-medium">Tổng đợt broadcast</div>
            <div className="text-2xl font-bold text-slate-800 mt-1">{totalElements}</div>
          </Card>
          <Card className="rounded-xl border-slate-200/80 shadow-xs">
            <div className="text-xs text-slate-500 font-medium">Đối tượng hỗ trợ</div>
            <div className="text-base font-semibold text-teal-700 mt-1">Toàn bộ bệnh nhân</div>
          </Card>
          <Card className="rounded-xl border-slate-200/80 shadow-xs">
            <div className="text-xs text-slate-500 font-medium">Kênh phát sóng</div>
            <div className="text-base font-semibold text-blue-600 mt-1">WebSocket + In-App Notification</div>
          </Card>
        </div>

        {/* Announcements Table */}
        <Card className="rounded-xl border-slate-200/80 shadow-sm" bodyStyle={{ padding: 0 }}>
          <Table
            columns={columns}
            dataSource={data}
            rowKey="id"
            loading={loading}
            pagination={{
              current: page + 1,
              pageSize: pageSize,
              total: totalElements,
              showSizeChanger: true,
              pageSizeOptions: ['10', '20', '50'],
              onChange: (p, s) => {
                setPage(p - 1)
                setPageSize(s)
                loadData(p - 1, s)
              },
            }}
          />
        </Card>

        {/* Modal Soạn thông báo mới */}
        <Modal
          title={
            <div className="flex items-center gap-2">
              <SoundOutlined className="text-teal-600" />
              <span>Soạn thông báo Broadcast mới</span>
            </div>
          }
          open={createModalOpen}
          onCancel={() => !submitting && setCreateModalOpen(false)}
          footer={null}
          width={720}
          destroyOnClose
          centered
        >
          <Form
            form={form}
            layout="vertical"
            onFinish={handleSendBroadcast}
            initialValues={{
              targetGroup: 'ALL',
              sendWebSocket: true,
            }}
            className="mt-4"
          >
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              {/* Cột trái: Form nhập liệu */}
              <div className="space-y-4">
                <Form.Item
                  name="title"
                  label={<span className="font-medium text-slate-700">Tiêu đề thông báo</span>}
                  rules={[
                    { required: true, message: 'Vui lòng nhập tiêu đề thông báo' },
                    { max: 160, message: 'Tiêu đề tối đa 160 ký tự' },
                  ]}
                >
                  <Input placeholder="Ví dụ: Cập nhật hệ thống hoặc tin tức mới" />
                </Form.Item>

                <Form.Item
                  name="message"
                  label={<span className="font-medium text-slate-700">Nội dung thông báo</span>}
                  rules={[
                    { required: true, message: 'Vui lòng nhập nội dung thông báo' },
                    { max: 2000, message: 'Nội dung tối đa 2000 ký tự' },
                  ]}
                >
                  <TextArea
                    rows={4}
                    placeholder="Nhập nội dung chi tiết thông báo gửi đến người dùng..."
                    showCount
                    maxLength={2000}
                  />
                </Form.Item>

                <Form.Item
                  name="targetGroup"
                  label={<span className="font-medium text-slate-700">Đối tượng nhận</span>}
                  rules={[{ required: true }]}
                >
                  <Select
                    options={[
                      { value: 'ALL', label: 'Tất cả bệnh nhân (Toàn hệ thống)' },
                    ]}
                  />
                </Form.Item>

                <Form.Item
                  name="actionUrl"
                  label={
                    <span className="font-medium text-slate-700">
                      Đường dẫn liên kết (tùy chọn)
                    </span>
                  }
                >
                  <Input
                    prefix={<LinkOutlined className="text-slate-400" />}
                    placeholder="Ví dụ: /articles hoặc /food-diary"
                  />
                </Form.Item>

                <Form.Item
                  name="sendWebSocket"
                  valuePropName="checked"
                  label={
                    <div className="flex items-center gap-1.5">
                      <span className="font-medium text-slate-700">Phát sóng realtime (WebSocket)</span>
                      <Tooltip title="Gửi thông báo tức thời tới các bệnh nhân đang trực tuyến">
                        <InfoCircleOutlined className="text-slate-400 text-xs" />
                      </Tooltip>
                    </div>
                  }
                >
                  <Switch defaultChecked />
                </Form.Item>
              </div>

              {/* Cột phải: Xem trước giao diện bệnh nhân */}
              <div>
                <div className="text-xs font-semibold uppercase tracking-wider text-slate-500 mb-2">
                  Xem trước thông báo (Notification Preview)
                </div>
                <div className="p-4 rounded-xl border border-teal-200 bg-teal-50/40 space-y-3">
                  <div className="flex items-start gap-3">
                    <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-blue-100 text-blue-600">
                      <BellOutlined />
                    </span>
                    <div className="min-w-0 flex-1">
                      <div className="flex flex-wrap items-center gap-2">
                        <span className="font-semibold text-slate-900 text-sm">
                          {previewTitle || 'Tiêu đề thông báo mẫu'}
                        </span>
                        <Tag color="blue">Hệ thống</Tag>
                        <span className="h-2 w-2 rounded-full bg-teal-500" />
                      </div>
                      <p className="mt-1.5 mb-1 text-xs text-slate-700 whitespace-pre-line">
                        {previewMessage || 'Nội dung thông báo sẽ xuất hiện ở đây sau khi người quản trị soạn thảo...'}
                      </p>
                      <span className="text-[11px] text-slate-400">Vừa xong</span>
                    </div>
                  </div>
                </div>

                <Alert
                  type="info"
                  showIcon
                  className="mt-4 text-xs"
                  message="Lưu ý về quyền riêng tư"
                  description="Bệnh nhân đã tắt mục 'Thông báo hệ thống' trong phần Cài đặt cá nhân sẽ tự động được bỏ qua theo quy định UC0073."
                />
              </div>
            </div>

            <div className="flex justify-end gap-2 mt-6 pt-4 border-t border-slate-100">
              <Button onClick={() => setCreateModalOpen(false)} disabled={submitting}>
                Hủy bỏ
              </Button>
              <Button
                type="primary"
                htmlType="submit"
                icon={<SendOutlined />}
                loading={submitting}
                className="bg-teal-600 hover:bg-teal-500"
              >
                Phát sóng ngay
              </Button>
            </div>
          </Form>
        </Modal>

        {/* Modal Xem chi tiết broadcast */}
        <Modal
          title="Chi tiết thông báo Broadcast"
          open={detailModalOpen}
          onCancel={() => setDetailModalOpen(false)}
          footer={[
            <Button key="close" onClick={() => setDetailModalOpen(false)}>
              Đóng
            </Button>,
          ]}
          centered
        >
          {selectedItem && (
            <Descriptions bordered column={1} size="small" className="mt-4">
              <Descriptions.Item label="Mã thông báo">#{selectedItem.id}</Descriptions.Item>
              <Descriptions.Item label="Tiêu đề">{selectedItem.title}</Descriptions.Item>
              <Descriptions.Item label="Nội dung">
                <div className="whitespace-pre-line text-slate-800">{selectedItem.message}</div>
              </Descriptions.Item>
              <Descriptions.Item label="Đối tượng">
                {selectedItem.targetGroup === 'ALL'
                  ? 'Tất cả bệnh nhân'
                  : selectedItem.targetGroup}
              </Descriptions.Item>
              <Descriptions.Item label="Số người nhận">
                <span className="font-semibold text-emerald-600">
                  {selectedItem.recipientCount} người
                </span>
              </Descriptions.Item>
              <Descriptions.Item label="Liên kết hành động">
                {selectedItem.actionUrl ? (
                  <span className="font-mono text-xs text-blue-600">{selectedItem.actionUrl}</span>
                ) : (
                  <span className="text-slate-400">Không có</span>
                )}
              </Descriptions.Item>
              <Descriptions.Item label="Thời gian gửi">
                {new Intl.DateTimeFormat('vi-VN', {
                  dateStyle: 'full',
                  timeStyle: 'medium',
                }).format(new Date(selectedItem.createdAt))}
              </Descriptions.Item>
            </Descriptions>
          )}
        </Modal>
      </div>
    </CmsAppShell>
  )
}

export default CmsBroadcastPage
