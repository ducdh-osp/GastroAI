import {
  CheckCircleOutlined,
  ClockCircleOutlined,
  CloseCircleOutlined,
  CloudUploadOutlined,
  DeleteOutlined,
  EditOutlined,
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
  Button,
  Card,
  Col,
  Drawer,
  Empty,
  Input,
  Modal,
  Popconfirm,
  Row,
  Select,
  Space,
  Statistic,
  Table,
  Tabs,
  Tag,
  Tooltip,
  Typography,
  Upload,
  message,
} from 'antd'
import type { ColumnsType } from 'antd/es/table'
import type { UploadFile } from 'antd/es/upload/interface'
import dayjs from 'dayjs'
import { useCallback, useEffect, useRef, useState } from 'react'
import {
  activateCmsDocumentVersion,
  deleteCmsDocument,
  deleteCmsDocumentVersion,
  listCmsDocuments,
  listCmsDocumentVersions,
  publishCmsDocument,
  renameCmsDocument,
  retryCmsDocument,
  unpublishCmsDocument,
  uploadCmsDocument,
  uploadCmsDocumentVersion,
  type CmsDocumentItem,
  type CmsDocumentStats,
  type CmsDocumentVersion,
  type DocumentProcessingStage,
  type DocumentStatus,
} from '../../api/cmsDocuments'
import { CmsAppShell } from '../../components/cms/CmsAppShell'
import { DocumentContentSearch } from '../../components/cms/DocumentContentSearch'

const { Title, Text } = Typography
const { Dragger } = Upload

const PAGE_SIZE = 15
const EMPTY_STATS: CmsDocumentStats = { total: 0, ready: 0, processing: 0, error: 0 }

// Code cu viet lap (err as {...})?.response?.data?.message o nhieu cho - gom thanh 1 ham.
function errorMessageOf(err: unknown, fallback: string): string {
  return (err as { response?: { data?: { message?: string } } })?.response?.data?.message || fallback
}

// "trao-nguoc-da-day.pdf" -> "trao-nguoc-da-day": goi y ten mac dinh khi chon file.
function fileNameWithoutExtension(name: string) {
  const dot = name.lastIndexOf('.')
  return dot > 0 ? name.slice(0, dot) : name
}

export default function CmsDocumentsPage() {
  const [items, setItems] = useState<CmsDocumentItem[]>([])
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(false)

  // Filters
  const [search, setSearch] = useState('')
  const [selectedStatus, setSelectedStatus] = useState<DocumentStatus | undefined>(undefined)

  // So lieu 4 o thong ke - BE tinh tren TOAN BO tai lieu, khong theo bo loc/trang.
  const [stats, setStats] = useState<CmsDocumentStats>(EMPTY_STATS)

  // C7 - khoa nut khi mot thao tac dang chay: busyKey de hien spinner / khoa nut,
  // busyRef de chan bam dup truoc khi React kip ve lai.
  const [busyKey, setBusyKey] = useState<string | null>(null)
  const busyRef = useRef<string | null>(null)

  // Modal upload
  const [uploadModalOpen, setUploadModalOpen] = useState(false)
  const [uploadFileList, setUploadFileList] = useState<UploadFile[]>([])
  const [sourceUrlInput, setSourceUrlInput] = useState('')
  const [titleInput, setTitleInput] = useState('')
  const [uploading, setUploading] = useState(false)

  // Doi ten tai lieu
  const [renameTarget, setRenameTarget] = useState<CmsDocumentItem | null>(null)
  const [renameValue, setRenameValue] = useState('')

  // Modal xem lỗi chi tiết
  const [errorModalItem, setErrorModalItem] = useState<CmsDocumentItem | null>(null)

  // UC0049 - tai ban moi: null = tai tai lieu moi (nhu cu), co gia tri = tai phien ban moi cho tai lieu do
  const [versionTarget, setVersionTarget] = useState<CmsDocumentItem | null>(null)

  // UC0049 - ngan keo lich su phien ban
  const [historyItem, setHistoryItem] = useState<CmsDocumentItem | null>(null)
  const [versions, setVersions] = useState<CmsDocumentVersion[]>([])
  const [versionsLoading, setVersionsLoading] = useState(false)

  const loadData = useCallback(async (silent = false) => {
    if (!silent) setLoading(true)
    try {
      const res = await listCmsDocuments({
        q: search.trim() || undefined,
        status: selectedStatus,
        page,
        size: PAGE_SIZE,
      })
      setItems(res.items)
      setTotal(res.total)
      setStats(res.stats ?? EMPTY_STATS)
    } catch {
      if (!silent) message.error('Không thể tải danh sách tài liệu y khoa')
    } finally {
      if (!silent) setLoading(false)
    }
  }, [search, selectedStatus, page])

  useEffect(() => {
    loadData()
  }, [loadData])

  // Tu dong cap nhat moi 4s khi co tai lieu dang xu ly (dem tren toan bo, khong chi trang dang xem).
  const isPolling = stats.processing > 0

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

  function openUploadNew() {
    setVersionTarget(null)
    setUploadFileList([])
    setSourceUrlInput('')
    setTitleInput('')
    setUploadModalOpen(true)
  }

  function openUploadVersion(item: CmsDocumentItem) {
    setVersionTarget(item)
    setUploadFileList([])
    setSourceUrlInput(item.sourceUrl ?? '')
    setTitleInput('')
    setUploadModalOpen(true)
  }

  function closeUploadModal() {
    setUploadModalOpen(false)
    setVersionTarget(null)
  }

  async function handleUpload() {
    if (uploadFileList.length === 0) {
      message.warning('Vui lòng chọn hoặc kéo thả ít nhất 1 tệp tài liệu')
      return
    }

    const file = uploadFileList[0].originFileObj
    if (!file) {
      message.warning('Không đọc được tệp đã chọn, vui lòng chọn lại')
      return
    }
    if (!versionTarget && !titleInput.trim()) {
      message.warning('Vui lòng nhập tên tài liệu')
      return
    }

    setUploading(true)
    try {
      if (versionTarget) {
        await uploadCmsDocumentVersion(versionTarget.id, file, sourceUrlInput || undefined)
        message.success(`Đã tải phiên bản mới cho "${versionTarget.title}". Bản cũ vẫn được dùng cho tới khi bản mới xử lý xong.`)
      } else {
        await uploadCmsDocument(file, sourceUrlInput || undefined, titleInput)
        message.success('Đã tải lên tài liệu thành công. Hệ thống đang tiến hành số hóa tự động.')
      }
      closeUploadModal()
      setUploadFileList([])
      setSourceUrlInput('')
      setTitleInput('')
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
      } catch (err) {
        message.error(errorMessageOf(err, 'Không thể thực hiện thử lại'))
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

  function openRename(item: CmsDocumentItem) {
    setRenameTarget(item)
    setRenameValue(item.title)
  }

  function handleRename() {
    if (!renameTarget) return Promise.resolve()
    const target = renameTarget
    const title = renameValue.trim()
    if (!title) {
      message.warning('Tên tài liệu không được để trống')
      return Promise.resolve()
    }
    if (title === target.title) {
      setRenameTarget(null)
      return Promise.resolve()
    }
    return runBusy(`rename-${target.id}`, async () => {
      try {
        await renameCmsDocument(target.id, title)
        message.success('Đã đổi tên tài liệu')
        setRenameTarget(null)
        loadData()
      } catch (err) {
        message.error(errorMessageOf(err, 'Không đổi được tên tài liệu'))
      }
    })
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
        return <Text type="secondary" className="text-xs">Pending</Text>
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
      render: (id: number) => <span className="text-slate-400 dark:text-white text-xs">#{id}</span>,
    },
    {
      title: 'Tài liệu & Nguồn',
      key: 'title',
      render: (_, record) => (
        <div className="space-y-1">
          <div className="flex items-center gap-2">
            <FilePdfOutlined className="text-red-500 text-base" />
            <span className="font-semibold text-slate-800 dark:text-white">{record.title}</span>
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
        <Space orientation="vertical" size={2}>
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
        <span className="text-xs text-slate-500 dark:text-white">
          {dayjs(val).format('DD/MM/YYYY HH:mm')}
        </span>
      ),
    },
    {
      title: 'Thao tác',
      key: 'actions',
      width: 230,
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
          <Tooltip title="Đổi tên">
            <Button
              type="text"
              size="small"
              icon={<EditOutlined />}
              disabled={busyKey !== null}
              onClick={() => openRename(record)}
            />
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
              : 'Chatbot sẽ không trích dẫn tài liệu này nữa.'}
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
              onClick={openUploadNew}
            >
              Tải lên tài liệu mới
            </Button>
          </div>
        </div>

        <Tabs
          defaultActiveKey="list"
          items={[
            {
              key: 'list',
              label: 'Danh sách tài liệu',
              children: (
                <div className="space-y-6">
                  {/* KPI Stats */}
                  <Row gutter={[16, 16]}>
                    <Col xs={12} sm={6}>
                      <Card className="rounded-xl shadow-xs">
                        <Statistic title="Tổng số tài liệu" value={stats.total} />
                      </Card>
                    </Col>
                    <Col xs={12} sm={6}>
                      <Card className="rounded-xl shadow-xs border-green-100 bg-green-50/20 dark:border-green-900 dark:bg-green-900/20">
                        <Statistic
                          title="Sẵn sàng cho RAG"
                          value={stats.ready}
                          styles={{ content: { color: '#16a34a' } }}
                        />
                      </Card>
                    </Col>
                    <Col xs={12} sm={6}>
                      <Card className="rounded-xl shadow-xs border-blue-100 bg-blue-50/20 dark:border-blue-900 dark:bg-blue-900/20">
                        <Statistic
                          title="Đang trong tiến trình"
                          value={stats.processing}
                          styles={{ content: { color: '#2563eb' } }}
                        />
                      </Card>
                    </Col>
                    <Col xs={12} sm={6}>
                      <Card className="rounded-xl shadow-xs border-red-100 bg-red-50/20 dark:border-red-900 dark:bg-red-900/20">
                        <Statistic
                          title="Lỗi cần xử lý lại"
                          value={stats.error}
                          styles={{ content: { color: '#dc2626' } }}
                        />
                      </Card>
                    </Col>
                  </Row>

                  {/* Filter bar */}
                  <Card styles={{ body: { padding: 16 } }}>
                    <div className="flex flex-wrap items-center gap-3">
                      <Input
                        placeholder="Tìm kiếm tài liệu hoặc URL nguồn..."
                        prefix={<SearchOutlined className="text-slate-400 dark:text-white" />}
                        value={search}
                        onChange={(e) => setSearch(e.target.value)}
                        allowClear
                        style={{ flex: 1, minWidth: 240 }}
                      />
                      <Select
                        placeholder="Tất cả trạng thái"
                        value={selectedStatus}
                        onChange={setSelectedStatus}
                        allowClear
                        style={{ width: 180 }}
                        options={[
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
                  </Card>

                  {/* Table */}
                  <Card styles={{ body: { padding: 0 } }}>
                    <Table
                      rowKey="id"
                      columns={columns}
                      dataSource={items}
                      loading={loading}
                      pagination={{
                        current: page + 1,
                        pageSize: PAGE_SIZE,
                        total,
                        showSizeChanger: false,
                        showTotal: (totalCount) => `Tổng cộng ${totalCount} tài liệu`,
                        onChange: (p) => setPage(p - 1),
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
                </div>
              ),
            },
            {
              key: 'search',
              label: 'Tìm kiếm nội dung tài liệu',
              children: <DocumentContentSearch />,
            },
          ]}
        />

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
                title: 'Thời gian tải lên',
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
            closeUploadModal()
          }}
          footer={[
            <Button key="cancel" onClick={closeUploadModal} disabled={uploading}>
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
          destroyOnHidden
        >
          <div className="space-y-4 py-2">
            <div>
              <Text className="font-semibold text-slate-700 dark:text-white block mb-1">
                Chọn tệp tài liệu:
              </Text>
              <Dragger
                accept=".pdf,.docx"
                fileList={uploadFileList}
                beforeUpload={(file) => {
                  setUploadFileList([{
                    uid: file.uid,
                    name: file.name,
                    size: file.size,
                    type: file.type,
                    status: 'done',
                    originFileObj: file,
                  }])
                  // Goi y ten tu ten file - chi khi la tai lieu moi va nguoi dung chua tu go ten.
                  if (!versionTarget && !titleInput.trim()) {
                    setTitleInput(fileNameWithoutExtension(file.name))
                  }
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
                <p className="ant-upload-hint text-xs text-slate-400 dark:text-white">
                  Chỉ nhận PDF hoặc DOCX, tối đa 20MB.
                </p>
              </Dragger>
            </div>

            <div>
              <Text className="font-semibold text-slate-700 dark:text-white block mb-1">
                Tên tài liệu:
              </Text>
              <Input
                placeholder="VD: Trào ngược dạ dày thực quản (GERD)"
                value={versionTarget ? versionTarget.title : titleInput}
                onChange={(e) => setTitleInput(e.target.value)}
                disabled={!!versionTarget}
                maxLength={255}
                showCount
              />
            </div>

            <div>
              <Text className="font-semibold text-slate-700 dark:text-white block mb-1">
                URL nguồn công khai (Tùy chọn):
              </Text>
              <Input
                placeholder="VD: https://benhvien.vn/phac-do-dieu-tri-ibs.pdf"
                value={sourceUrlInput}
                onChange={(e) => setSourceUrlInput(e.target.value)}
                prefix={<LinkOutlined className="text-slate-400 dark:text-white" />}
              />
            </div>
          </div>
        </Modal>

        {/* Modal Đổi tên tài liệu */}
        <Modal
          title="Đổi tên tài liệu"
          open={renameTarget !== null}
          onOk={handleRename}
          onCancel={() => setRenameTarget(null)}
          okText="Lưu"
          cancelText="Hủy"
          confirmLoading={renameTarget !== null && busyKey === `rename-${renameTarget.id}`}
          destroyOnHidden
        >
          <Input
            value={renameValue}
            onChange={(e) => setRenameValue(e.target.value)}
            onPressEnter={handleRename}
            maxLength={255}
            showCount
            autoFocus
          />
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
                <Text className="text-xs uppercase text-slate-400 dark:text-white font-medium">
                  Tài liệu
                </Text>
                <div className="font-semibold text-slate-800 dark:text-white">
                  {errorModalItem.title} (#{errorModalItem.id})
                </div>
              </div>

              <div>
                <Text className="text-xs uppercase text-slate-400 dark:text-white font-medium">
                  Giai đoạn phát sinh lỗi
                </Text>
                <div>
                  <Tag color="error">{errorModalItem.processingStage}</Tag>
                </div>
              </div>

              <div>
                <Text className="text-xs uppercase text-slate-400 dark:text-white font-medium">
                  Thông báo lỗi từ hệ thống
                </Text>
                <div className="mt-1 rounded-lg bg-red-50 border border-red-200 p-3 text-red-800 font-mono text-xs whitespace-pre-wrap dark:bg-red-900/20 dark:border-red-900 dark:text-red-300">
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