import {
  CheckCircleOutlined,
  ClockCircleOutlined,
  CloseCircleOutlined,
  CloudUploadOutlined,
  DeleteOutlined,
  EyeInvisibleOutlined,
  EyeOutlined,
  FilePdfOutlined,
  FileTextOutlined,
  HistoryOutlined,
  InboxOutlined,
  LinkOutlined,
  LoadingOutlined,
  RedoOutlined,
  ReloadOutlined,
  SearchOutlined,
  UploadOutlined,
} from '@ant-design/icons'
import {
  Alert,
  Button,
  Card,
  Col,
  Drawer,
  Empty,
  Input,
  List,
  Modal,
  Popconfirm,
  Row,
  Select,
  Space,
  Statistic,
  Switch,
  Table,
  Tag,
  Tooltip,
  Typography,
  Upload,
  message,
} from 'antd'
import type { ColumnsType } from 'antd/es/table'
import type { UploadFile } from 'antd/es/upload/interface'
import dayjs from 'dayjs'
import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import {
  activateCmsDocumentVersion,
  deleteCmsDocument,
  deleteCmsDocumentVersion,
  listCmsDocuments,
  listCmsDocumentVersions,
  publishCmsDocument,
  retryCmsDocument,
  searchCmsDocumentContent,
  unpublishCmsDocument,
  uploadCmsDocument,
  uploadCmsDocumentVersion,
  type CmsDocumentItem,
  type CmsDocumentSearchHit,
  type CmsDocumentVersion,
  type DocumentProcessingStage,
  type DocumentStatus,
} from '../../api/cmsDocuments'
import { CmsAppShell } from '../../components/cms/CmsAppShell'
const { Title, Text } = Typography
const { Dragger } = Upload

// Code cu viet lap (err as {...})?.response?.data?.message o nhieu cho - gom thanh 1 ham.
function errorMessageOf(err: unknown, fallback: string): string {
  return (err as { response?: { data?: { message?: string } } })?.response?.data?.message || fallback
}

// To sang tu khoa trong doan trich. Dung JSX (khong ghep chuoi HTML) nen noi dung tai lieu
// khong the chen ma doc vao trang.
function highlight(text: string, q: string) {
  const index = text.toLowerCase().indexOf(q.toLowerCase())
  if (!q || index < 0) return text
  return (
    <>
      {text.slice(0, index)}
      <mark>{text.slice(index, index + q.length)}</mark>
      {text.slice(index + q.length)}
    </>
  )
}

export default function CmsDocumentsPage() {
  const [items, setItems] = useState<CmsDocumentItem[]>([])
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(0)
  const [pageSize, setPageSize] = useState(15)
  const [loading, setLoading] = useState(false)

  // Filters
  const [search, setSearch] = useState('')
  const [selectedStatus, setSelectedStatus] = useState<DocumentStatus | undefined>(undefined)

  // Auto-refresh: mac dinh TAT. Van tu dong cap nhat khi co tai lieu dang xu ly (xem useEffect polling).
  const [autoRefresh, setAutoRefresh] = useState(false)

  // C7 - khoa nut khi mot thao tac dang chay: busyKey de hien spinner / khoa nut,
  // busyRef de chan bam dup truoc khi React kip ve lai.
  const [busyKey, setBusyKey] = useState<string | null>(null)
  const busyRef = useRef<string | null>(null)

  // Modal upload
  const [uploadModalOpen, setUploadModalOpen] = useState(false)
  const [uploadFileList, setUploadFileList] = useState<UploadFile[]>([])
  const [sourceUrlInput, setSourceUrlInput] = useState('')
  const [uploading, setUploading] = useState(false)

  // Modal xem lỗi chi tiết
  const [errorModalItem, setErrorModalItem] = useState<CmsDocumentItem | null>(null)

  // UC0049 - tai ban moi: null = tai tai lieu moi (nhu cu), co gia tri = tai phien ban moi cho tai lieu do
  const [versionTarget, setVersionTarget] = useState<CmsDocumentItem | null>(null)

  // UC0049 - ngan keo lich su phien ban
  const [historyItem, setHistoryItem] = useState<CmsDocumentItem | null>(null)
  const [versions, setVersions] = useState<CmsDocumentVersion[]>([])
  const [versionsLoading, setVersionsLoading] = useState(false)

  // UC0055 - tim trong noi dung
  const [contentQuery, setContentQuery] = useState('')
  const [contentHits, setContentHits] = useState<CmsDocumentSearchHit[] | null>(null)
  const [contentSearching, setContentSearching] = useState(false)
  // C4 - true khi con ket qua chua hien (BE chi tra toi da 50)
  const [contentTruncated, setContentTruncated] = useState(false)

  const loadData = useCallback(async (silent = false) => {
    if (!silent) setLoading(true)
    try {
      const res = await listCmsDocuments({
        q: search.trim() || undefined,
        status: selectedStatus,
        page,
        size: pageSize,
      })
      setItems(res.items)
      setTotal(res.total)
    } catch {
      if (!silent) message.error('Không thể tải danh sách tài liệu y khoa')
    } finally {
      if (!silent) setLoading(false)
    }
  }, [search, selectedStatus, page, pageSize])

  useEffect(() => {
    loadData()
  }, [loadData])

  // Polling tự động mỗi 4s nếu có tài liệu đang xử lý
  const hasProcessingDocs = useMemo(() => {
    return items.some((item) => item.status === 'PROCESSING' || item.status === 'PENDING')
  }, [items])

  const isPolling = autoRefresh || hasProcessingDocs

  useEffect(() => {
    if (!isPolling) return

    const interval = setInterval(() => {
      // C7 - tab dang an thi bo qua, khong goi API vo ich.
      if (document.visibilityState !== 'visible') return
      loadData(true)
    }, 4000)

    return () => clearInterval(interval)
  }, [isPolling, loadData])

  // C7 - quay lai tab thi cap nhat ngay 1 lan (trong luc an tab da bo qua cac lan poll).
  useEffect(() => {
    function onVisibilityChange() {
      if (document.visibilityState === 'visible') loadData(true)
    }
    document.addEventListener('visibilitychange', onVisibilityChange)
    return () => document.removeEventListener('visibilitychange', onVisibilityChange)
  }, [loadData])

  // C7 - chay 1 thao tac "doc quyen": dang co thao tac khac thi bo qua.
  async function runBusy(key: string, action: () => Promise<void>) {
    if (busyRef.current !== null) return
    busyRef.current = key
    setBusyKey(key)
    try {
      await action()
    } finally {
      busyRef.current = null
      setBusyKey(null)
    }
  }

  // KPI stats
  const stats = useMemo(() => {
    const done = items.filter((i) => i.status === 'DONE' && i.currentVersionNo !== null).length
    const processing = items.filter((i) => i.status === 'PROCESSING' || i.status === 'PENDING').length
    const error = items.filter((i) => i.status === 'ERROR').length
    return { done, processing, error, total }
  }, [items, total])

  async function handleUpload() {
    if (uploadFileList.length === 0) {
      message.warning('Vui lòng chọn hoặc kéo thả ít nhất 1 tệp tài liệu')
      return
    }

    const file = uploadFileList[0].originFileObj as File
    setUploading(true)
    try {
      if (versionTarget) {
        await uploadCmsDocumentVersion(versionTarget.id, file, sourceUrlInput || undefined)
        message.success(`Đã tải phiên bản mới cho "${versionTarget.title}". Bản cũ vẫn được dùng cho tới khi bản mới xử lý xong.`)
      } else {
        await uploadCmsDocument(file, sourceUrlInput || undefined)
        message.success('Đã tải lên tài liệu thành công. Hệ thống đang tiến hành số hóa tự động.')
      }
      setUploadModalOpen(false)
      setVersionTarget(null)
      setUploadFileList([])
      setSourceUrlInput('')
      loadData()
    } catch (err: unknown) {
      message.error(errorMessageOf(err, 'Tải lên tài liệu thất bại'))
    } finally {
      setUploading(false)
    }
  }

  function handleRetry(item: CmsDocumentItem) {
    return runBusy(`retry-${item.id}`, async () => {
      try {
        await retryCmsDocument(item.id)
        message.success(`Đã đưa tài liệu #${item.id} vào hàng đợi xử lý lại`)
        loadData()
      } catch {
        message.error('Không thể thực hiện thử lại')
      }
    })
  }

  function handleDelete(item: CmsDocumentItem) {
    return runBusy(`delete-${item.id}`, async () => {
      try {
        await deleteCmsDocument(item.id)
        message.success('Đã xóa tài liệu và các đoạn trích dẫn RAG liên quan')
        loadData()
      } catch (err: unknown) {
        message.error(errorMessageOf(err, 'Xóa tài liệu thất bại'))
      }
    })
  }

  function openUploadVersion(item: CmsDocumentItem) {
    setVersionTarget(item)
    setUploadFileList([])
    setSourceUrlInput(item.sourceUrl ?? '')
    setUploadModalOpen(true)
  }

  async function openHistory(item: CmsDocumentItem) {
    setHistoryItem(item)
    setVersionsLoading(true)
    try {
      setVersions(await listCmsDocumentVersions(item.id))
    } catch (err) {
      message.error(errorMessageOf(err, 'Không tải được lịch sử phiên bản'))
    } finally {
      setVersionsLoading(false)
    }
  }

  function handleActivate(version: CmsDocumentVersion) {
    if (!historyItem) return Promise.resolve()
    const target = historyItem
    return runBusy(`activate-${version.id}`, async () => {
      try {
        await activateCmsDocumentVersion(target.id, version.id)
        message.success(`Chatbot đã chuyển sang dùng phiên bản v${version.versionNo}`)
        await openHistory(target)
        loadData()
      } catch (err) {
        message.error(errorMessageOf(err, 'Không đổi được phiên bản'))
      }
    })
  }

  function handleDeleteVersion(version: CmsDocumentVersion) {
    if (!historyItem) return Promise.resolve()
    const target = historyItem
    return runBusy(`deleteVersion-${version.id}`, async () => {
      try {
        await deleteCmsDocumentVersion(target.id, version.id)
        message.success(`Đã xóa phiên bản v${version.versionNo}`)
        await openHistory(target)
        loadData()
      } catch (err) {
        message.error(errorMessageOf(err, 'Không xóa được phiên bản'))
      }
    })
  }

  function handleTogglePublish(item: CmsDocumentItem) {
    return runBusy(`publish-${item.id}`, async () => {
      try {
        if (item.unpublished) {
          await publishCmsDocument(item.id)
          message.success('Đã đăng lại tài liệu cho chatbot')
        } else {
          await unpublishCmsDocument(item.id)
          message.success('Đã gỡ tài liệu khỏi chatbot (dữ liệu vẫn được giữ)')
        }
        loadData()
      } catch (err) {
        message.error(errorMessageOf(err, 'Thao tác thất bại'))
      }
    })
  }

  async function handleContentSearch(value: string) {
    const q = value.trim()
    if (q.length < 2) {
      message.warning('Nhập ít nhất 2 ký tự để tìm trong nội dung')
      return
    }
    setContentSearching(true)
    try {
      const result = await searchCmsDocumentContent(q)
      setContentHits(result.items)
      setContentTruncated(result.truncated)
    } catch (err) {
      message.error(errorMessageOf(err, 'Tìm kiếm thất bại'))
    } finally {
      setContentSearching(false)
    }
  }

  function renderStatusTag(status: DocumentStatus) {
    switch (status) {
      case 'DONE':
        return (
          <Tag icon={<CheckCircleOutlined />} color="success">
            Sẵn sàng (RAG)
          </Tag>
        )
      case 'PROCESSING':
        return (
          <Tag icon={<LoadingOutlined spin />} color="processing">
            Đang xử lý
          </Tag>
        )
      case 'PENDING':
        return (
          <Tag icon={<ClockCircleOutlined />} color="warning">
            Chờ xử lý
          </Tag>
        )
      case 'ERROR':
        return (
          <Tag icon={<CloseCircleOutlined />} color="error">
            Lỗi xử lý
          </Tag>
        )
      default:
        return <Tag>{status}</Tag>
    }
  }

  function renderStageInfo(stage: DocumentProcessingStage, status: DocumentStatus) {
    if (status === 'ERROR') {
      return <Text type="danger" className="text-xs font-medium">Thất bại</Text>
    }
    switch (stage) {
      case 'PENDING':
        return <Text type="secondary" className="text-xs">Đang xếp hàng</Text>
      case 'EXTRACTION':
        return <Text className="text-xs text-blue-600 font-medium">Trích xuất văn bản</Text>
      case 'CHUNKING':
        return <Text className="text-xs text-purple-600 font-medium">Phân đoạn (Chunking)</Text>
      case 'EMBEDDING':
        return <Text className="text-xs text-amber-600 font-medium">Vector Embedding</Text>
      case 'DONE':
        return <Text type="success" className="text-xs font-medium">Hoàn tất 100%</Text>
      default:
        return <Text className="text-xs">{stage}</Text>
    }
  }

  const columns: ColumnsType<CmsDocumentItem> = [
    {
      title: 'Mã',
      dataIndex: 'id',
      key: 'id',
      width: 65,
      align: 'center',
      render: (id: number) => <span className="text-slate-400 text-xs">#{id}</span>,
    },
    {
      title: 'Tài liệu & Nguồn',
      key: 'title',
      render: (_, record) => (
        <div className="space-y-1">
          <div className="flex items-center gap-2">
            <FilePdfOutlined className="text-red-500 text-base" />
            <span className="font-semibold text-slate-800">{record.title}</span>
          </div>
          {record.sourceUrl && (
            <div className="flex items-center gap-1 text-xs text-teal-700">
              <LinkOutlined />
              <a
                href={record.sourceUrl}
                target="_blank"
                rel="noreferrer"
                className="hover:underline truncate max-w-sm"
              >
                {record.sourceUrl}
              </a>
            </div>
          )}
        </div>
      ),
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      width: 140,
      render: (status: DocumentStatus) => renderStatusTag(status),
    },
    {
      title: 'Phiên bản',
      key: 'version',
      width: 140,
      render: (_, record) => (
        <Space direction="vertical" size={2}>
          <Text className="text-xs">v{record.versionNo} · {record.versionCount} phiên bản</Text>
          {record.unpublished ? (
            <Tag color="default">Đã gỡ</Tag>
          ) : record.currentVersionNo !== null ? (
            <Tag color="green">Đang dùng v{record.currentVersionNo}</Tag>
          ) : (
            <Tag color="orange">Chưa sẵn sàng</Tag>
          )}
        </Space>
      ),
    },
    {
      title: 'Giai đoạn số hóa',
      key: 'processingStage',
      width: 160,
      render: (_, record) => (
        <div className="space-y-0.5">
          {renderStageInfo(record.processingStage, record.status)}
          {record.status === 'ERROR' && record.errorMessage && (
            <button
              type="button"
              onClick={() => setErrorModalItem(record)}
              className="text-xs text-red-500 hover:underline block text-left cursor-pointer"
            >
              Xem chi tiết lỗi
            </button>
          )}
        </div>
      ),
    },
    {
      title: 'Đoạn trích dẫn',
      dataIndex: 'chunkCount',
      key: 'chunkCount',
      width: 120,
      align: 'center',
      render: (chunks: number, record) =>
        record.status === 'DONE' ? (
          <Tag color="cyan">{chunks} chunks</Tag>
        ) : (
          <span className="text-slate-300 text-xs">-</span>
        ),
    },
    {
      title: 'Thời gian tải lên',
      dataIndex: 'uploadedAt',
      key: 'uploadedAt',
      width: 150,
      render: (val: string) => (
        <span className="text-xs text-slate-500">
          {dayjs(val).format('DD/MM/YYYY HH:mm')}
        </span>
      ),
    },
    {
      title: 'Thao tác',
      key: 'actions',
      width: 200,
      align: 'center',
      render: (_, record) => (
        <Space size="small">
          {record.status === 'ERROR' && (
            <Button
              type="text"
              size="small"
              icon={<RedoOutlined />}
              title="Thử xử lý lại"
              className="text-amber-600 hover:text-amber-700"
              loading={busyKey === `retry-${record.id}`}
              disabled={busyKey !== null}
              onClick={() => handleRetry(record)}
            />
          )}
          <Tooltip title="Lịch sử phiên bản">
            <Button type="text" size="small" icon={<HistoryOutlined />} onClick={() => openHistory(record)} />
          </Tooltip>
          <Tooltip title="Tải phiên bản mới">
            <Button
              type="text"
              size="small"
              icon={<CloudUploadOutlined />}
              disabled={busyKey !== null || record.status === 'PENDING' || record.status === 'PROCESSING'}
              onClick={() => openUploadVersion(record)}
            />
          </Tooltip>
          <Popconfirm
            title={record.unpublished ? 'Đăng lại tài liệu?' : 'Gỡ tài liệu khỏi chatbot?'}
            description={record.unpublished
              ? 'Chatbot sẽ dùng lại phiên bản đã xử lý xong mới nhất.'
              : 'Chatbot sẽ không trích dẫn tài liệu này nữa. Dữ liệu vẫn được giữ, có thể đăng lại.'}
            okText="Đồng ý"
            cancelText="Hủy"
            onConfirm={() => handleTogglePublish(record)}
            disabled={busyKey !== null || (!record.unpublished && record.currentVersionNo === null)}
          >
            <Tooltip title={record.unpublished ? 'Đăng lại' : 'Gỡ khỏi chatbot'}>
              <Button
                type="text"
                size="small"
                loading={busyKey === `publish-${record.id}`}
                disabled={busyKey !== null || (!record.unpublished && record.currentVersionNo === null)}
                icon={record.unpublished ? <EyeOutlined /> : <EyeInvisibleOutlined />}
              />
            </Tooltip>
          </Popconfirm>
          <Popconfirm
            title={`Xóa tài liệu và tất cả ${record.versionCount} phiên bản?`}
            description="Mọi phiên bản, file gốc và toàn bộ vector embeddings liên quan sẽ bị xóa khỏi hệ thống RAG. Không khôi phục được. Muốn xóa riêng một phiên bản, hãy mở Lịch sử phiên bản."
            okText="Xóa tất cả"
            cancelText="Hủy"
            okButtonProps={{ danger: true }}
            onConfirm={() => handleDelete(record)}
            disabled={busyKey !== null}
          >
            <Button
              type="text"
              size="small"
              danger
              loading={busyKey === `delete-${record.id}`}
              disabled={busyKey !== null}
              icon={<DeleteOutlined />}
              title="Xóa tài liệu"
            />
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <CmsAppShell>
      <div className="p-6 max-w-7xl mx-auto space-y-6">
        {/* Header */}
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
          <div>
            <Title level={3} className="!mb-1 flex items-center gap-2">
              <FileTextOutlined className="text-teal-600" />
              Tài liệu y khoa & Số hóa RAG
            </Title>
            <Text type="secondary">
              Quản lý tài liệu phác đồ y khoa, giám sát tiến trình số hóa & chia chunking dùng cho trợ lý AI (UC0061)
            </Text>
          </div>
          <div className="flex items-center gap-2">
            <Button
              icon={<ReloadOutlined />}
              onClick={() => loadData()}
              loading={loading}
            >
              Làm mới
            </Button>
            <Button
              type="primary"
              icon={<UploadOutlined />}
              className="bg-teal-600 hover:bg-teal-700"
              onClick={() => {
                setUploadFileList([])
                setSourceUrlInput('')
                setVersionTarget(null)
                setUploadModalOpen(true)
              }}
            >
              Tải lên tài liệu mới
            </Button>
          </div>
        </div>

        {/* KPI Stats */}
        <Row gutter={[16, 16]}>
          <Col xs={12} sm={6}>
            <Card className="rounded-xl shadow-xs">
              <Statistic title="Tổng số tài liệu" value={stats.total} />
            </Card>
          </Col>
          <Col xs={12} sm={6}>
            <Card className="rounded-xl shadow-xs border-green-100 bg-green-50/20">
              <Statistic
                title="Sẵn sàng cho RAG"
                value={stats.done}
                valueStyle={{ color: '#16a34a' }}
              />
            </Card>
          </Col>
          <Col xs={12} sm={6}>
            <Card className="rounded-xl shadow-xs border-blue-100 bg-blue-50/20">
              <Statistic
                title="Đang trong tiến trình"
                value={stats.processing}
                valueStyle={{ color: '#2563eb' }}
              />
            </Card>
          </Col>
          <Col xs={12} sm={6}>
            <Card className="rounded-xl shadow-xs border-red-100 bg-red-50/20">
              <Statistic
                title="Lỗi cần xử lý lại"
                value={stats.error}
                valueStyle={{ color: '#dc2626' }}
              />
            </Card>
          </Col>
        </Row>

        {/* Filter bar */}
        <Card bodyStyle={{ padding: 16 }}>
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div className="flex flex-wrap items-center gap-3">
              <Input
                placeholder="Tìm kiếm tài liệu hoặc URL nguồn..."
                prefix={<SearchOutlined className="text-slate-400" />}
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                allowClear
                className="w-72"
              />
              <Select
                placeholder="Trạng thái"
                value={selectedStatus}
                onChange={setSelectedStatus}
                allowClear
                className="w-44"
                options={[
                  { label: 'Tất cả trạng thái', value: undefined as unknown as DocumentStatus },
                  { label: 'Sẵn sàng (RAG)', value: 'DONE' },
                  { label: 'Đang xử lý', value: 'PROCESSING' },
                  { label: 'Chờ xử lý', value: 'PENDING' },
                  { label: 'Lỗi', value: 'ERROR' },
                ]}
              />
              {(search || selectedStatus !== undefined) && (
                <Button
                  type="link"
                  onClick={() => {
                    setSearch('')
                    setSelectedStatus(undefined)
                  }}
                >
                  Đặt lại bộ lọc
                </Button>
              )}
            </div>

            <div className="flex items-center gap-2 text-xs text-slate-500">
              <span
                className={`inline-block w-2 h-2 rounded-full ${isPolling ? 'bg-blue-500 animate-pulse' : 'bg-emerald-500'
                  }`}
              />
              <span>
                {isPolling
                  ? 'Đang tự động đồng bộ tiến trình (mỗi 4s)...'
                  : 'Hệ thống ở trạng thái ổn định'}
              </span>
              <Switch
                size="small"
                checked={autoRefresh}
                onChange={setAutoRefresh}
                checkedChildren="Tự động"
                unCheckedChildren="Tắt"
              />
            </div>
          </div>
        </Card>

        {/* Table */}
        <Card bodyStyle={{ padding: 0 }}>
          <Table
            rowKey="id"
            columns={columns}
            dataSource={items}
            loading={loading}
            pagination={{
              current: page + 1,
              pageSize,
              total,
              showSizeChanger: true,
              pageSizeOptions: ['10', '15', '30'],
              showTotal: (totalCount) => `Tổng cộng ${totalCount} tài liệu`,
              onChange: (p, ps) => {
                setPage(p - 1)
                setPageSize(ps)
              },
            }}
            locale={{
              emptyText: (
                <Empty
                  image={Empty.PRESENTED_IMAGE_SIMPLE}
                  description="Chưa có tài liệu y khoa nào được tải lên"
                />
              ),
            }}
          />
        </Card>

        {/* UC0055 - Tìm trong nội dung tài liệu */}
        <Card title="Tìm trong nội dung tài liệu" bodyStyle={{ padding: 16 }}>
          <Input.Search
            placeholder="VD: Helicobacter pylori, liều PPI..."
            value={contentQuery}
            onChange={(e) => setContentQuery(e.target.value)}
            onSearch={handleContentSearch}
            loading={contentSearching}
            enterButton="Tìm"
            allowClear
            maxLength={200}
          />
          <Text type="secondary" className="text-xs mt-1 block">
            Chỉ tìm trong phiên bản đang được chatbot dùng. Tìm có phân biệt dấu (gõ "dạ dày", không gõ "da day").
          </Text>
          {contentHits !== null && contentTruncated && (
            <Alert
              type="warning"
              showIcon
              className="mt-3 text-xs"
              message="Chỉ hiển thị 50 kết quả đầu tiên. Hãy nhập từ khóa cụ thể hơn để thu hẹp kết quả."
            />
          )}
          {contentHits !== null && (
            <List
              className="mt-4"
              dataSource={contentHits}
              locale={{ emptyText: 'Không tìm thấy đoạn nào chứa cụm từ này' }}
              renderItem={(hit) => (
                <List.Item>
                  <List.Item.Meta
                    title={<span>{hit.title} <Text type="secondary" className="text-xs">· Đoạn #{hit.chunkIndex}</Text></span>}
                    description={highlight(hit.snippet, contentQuery.trim())}
                  />
                </List.Item>
              )}
            />
          )}
        </Card>

        {/* UC0049 - Ngăn kéo lịch sử phiên bản */}
        <Drawer
          title={historyItem ? `Lịch sử phiên bản — ${historyItem.title}` : ''}
          open={historyItem !== null}
          onClose={() => setHistoryItem(null)}
          width={560}
        >
          <Table<CmsDocumentVersion>
            rowKey="id"
            size="small"
            loading={versionsLoading}
            dataSource={versions}
            pagination={false}
            columns={[
              { title: 'Bản', dataIndex: 'versionNo', width: 60, render: (v: number) => `v${v}` },
              { title: 'Trạng thái', dataIndex: 'status', width: 120, render: (s: DocumentStatus) => renderStatusTag(s) },
              {
                title: 'Tải lên',
                dataIndex: 'uploadedAt',
                render: (val: string) => dayjs(val).format('DD/MM/YYYY HH:mm'),
              },
              {
                title: '',
                key: 'action',
                width: 190,
                render: (_, v) => {
                  const canDelete = !v.currentVersion
                    && v.versionNo !== 1
                    && v.status !== 'PENDING'
                    && v.status !== 'PROCESSING'
                  return (
                    <Space size={4}>
                      {v.currentVersion ? (
                        <Tag color="green">Đang dùng</Tag>
                      ) : v.status === 'DONE' ? (
                        <Popconfirm
                          title={`Chuyển chatbot sang dùng phiên bản v${v.versionNo}?`}
                          description="Từ giờ chatbot sẽ trích dẫn nội dung của phiên bản này thay cho bản đang dùng."
                          okText="Dùng bản này"
                          cancelText="Hủy"
                          disabled={busyKey !== null}
                          onConfirm={() => handleActivate(v)}
                        >
                          <Button
                            size="small"
                            loading={busyKey === `activate-${v.id}`}
                            disabled={busyKey !== null}
                          >
                            Dùng bản này
                          </Button>
                        </Popconfirm>
                      ) : null}
                      {canDelete && (
                        <Popconfirm
                          title={`Xóa phiên bản v${v.versionNo}?`}
                          description="Chỉ phiên bản này bị xóa (kèm file và dữ liệu RAG của nó). Các phiên bản khác giữ nguyên."
                          okText="Xóa"
                          cancelText="Hủy"
                          okButtonProps={{ danger: true }}
                          disabled={busyKey !== null}
                          onConfirm={() => handleDeleteVersion(v)}
                        >
                          <Button
                            size="small"
                            danger
                            icon={<DeleteOutlined />}
                            title="Xóa phiên bản này"
                            loading={busyKey === `deleteVersion-${v.id}`}
                            disabled={busyKey !== null}
                          />
                        </Popconfirm>
                      )}
                    </Space>
                  )
                },
              },
            ]}
          />
        </Drawer>

        {/* Modal Tải lên tài liệu */}
        <Modal
          title={versionTarget ? `Tải phiên bản mới cho "${versionTarget.title}"` : 'Tải lên tài liệu phác đồ y khoa mới'}
          open={uploadModalOpen}
          onCancel={() => {
            if (uploading) return
            setUploadModalOpen(false)
            setVersionTarget(null)
          }}
          footer={[
            <Button
              key="cancel"
              onClick={() => {
                setUploadModalOpen(false)
                setVersionTarget(null)
              }}
              disabled={uploading}
            >
              Hủy
            </Button>,
            <Button
              key="submit"
              type="primary"
              className="bg-teal-600 hover:bg-teal-700"
              loading={uploading}
              onClick={handleUpload}
            >
              Bắt đầu tải lên & số hóa
            </Button>,
          ]}
          width={580}
          destroyOnClose
        >
          <div className="space-y-4 py-2">
            {versionTarget && (
              <Alert
                type="warning"
                showIcon
                message={`Bạn đang tải phiên bản mới cho: ${versionTarget.title}`}
                description="File này sẽ trở thành một phiên bản mới của tài liệu trên, không phải một tài liệu riêng. Muốn thêm tài liệu khác, hãy đóng hộp này và bấm 'Tải lên tài liệu mới'."
                className="text-xs"
              />
            )}

            <Alert
              type="info"
              showIcon
              message="Định dạng hỗ trợ: PDF, DOCX, TXT"
              description="Sau khi tải lên, hệ thống sẽ tự động trích xuất nội dung văn bản, chia đoạn thông minh (Chunking) và sinh vector embeddings phục vụ RAG tư vấn tiêu hóa."
              className="text-xs"
            />

            <div>
              <Text className="font-semibold text-slate-700 block mb-1">
                Chọn tệp tài liệu:
              </Text>
              <Dragger
                fileList={uploadFileList}
                beforeUpload={(file) => {
                  setUploadFileList([file as UploadFile])
                  return false
                }}
                onRemove={() => setUploadFileList([])}
                maxCount={1}
              >
                <p className="ant-upload-drag-icon">
                  <InboxOutlined className="text-teal-600 text-3xl" />
                </p>
                <p className="ant-upload-text text-sm font-medium">
                  Kéo thả file vào đây hoặc bấm để chọn tệp từ máy tính
                </p>
                <p className="ant-upload-hint text-xs text-slate-400">
                  Khuyên dùng các phác đồ điều trị chính thống từ Bệnh viện Nhân Dân 115, Bạch Mai hoặc Bộ Y Tế.
                </p>
              </Dragger>
            </div>

            <div>
              <Text className="font-semibold text-slate-700 block mb-1">
                URL nguồn công khai (Tùy chọn):
              </Text>
              <Input
                placeholder="VD: https://benhvientanphu.vn/phac-do-dieu-tri-ibs.pdf"
                value={sourceUrlInput}
                onChange={(e) => setSourceUrlInput(e.target.value)}
                prefix={<LinkOutlined className="text-slate-400" />}
              />
              <Text type="secondary" className="text-xs mt-1 block">
                URL nguồn sẽ được lưu để AI trích dẫn nguồn minh bạch cho bệnh nhân khi trả lời.
              </Text>
            </div>
          </div>
        </Modal>

        {/* Modal Xem chi tiết lỗi */}
        <Modal
          title={
            <div className="flex items-center gap-2 text-red-600">
              <CloseCircleOutlined />
              <span>Chi tiết lỗi xử lý tài liệu</span>
            </div>
          }
          open={Boolean(errorModalItem)}
          onCancel={() => setErrorModalItem(null)}
          footer={[
            <Button key="close" onClick={() => setErrorModalItem(null)}>
              Đóng
            </Button>,
            errorModalItem && (
              <Button
                key="retry"
                type="primary"
                className="bg-amber-600"
                onClick={() => {
                  const current = errorModalItem
                  setErrorModalItem(null)
                  if (current) handleRetry(current)
                }}
              >
                Thử xử lý lại ngay
              </Button>
            ),
          ]}
        >
          {errorModalItem && (
            <div className="space-y-3 py-2">
              <div>
                <Text className="text-xs uppercase text-slate-400 font-medium">
                  Tài liệu
                </Text>
                <div className="font-semibold text-slate-800">
                  {errorModalItem.title} (#{errorModalItem.id})
                </div>
              </div>

              <div>
                <Text className="text-xs uppercase text-slate-400 font-medium">
                  Giai đoạn phát sinh lỗi
                </Text>
                <div>
                  <Tag color="error">{errorModalItem.processingStage}</Tag>
                </div>
              </div>

              <div>
                <Text className="text-xs uppercase text-slate-400 font-medium">
                  Thông báo lỗi từ hệ thống
                </Text>
                <div className="mt-1 rounded-lg bg-red-50 border border-red-200 p-3 text-red-800 font-mono text-xs whitespace-pre-wrap">
                  {errorModalItem.errorMessage || 'Không có thông tin lỗi chi tiết.'}
                </div>
              </div>
            </div>
          )}
        </Modal>
      </div>
    </CmsAppShell>
  )
}