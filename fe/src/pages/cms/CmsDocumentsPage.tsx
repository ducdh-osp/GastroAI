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
  MoreOutlined,
  FileTextOutlined,
  HistoryOutlined,
  InboxOutlined,
  LinkOutlined,
  LoadingOutlined,
  RedoOutlined,
  ReloadOutlined,
  SearchOutlined,
  UploadOutlined,
  FileSearchOutlined,
  FileWordOutlined,
} from '@ant-design/icons'
import {
  Button,
  Card,
  Col,
  DatePicker,
  Drawer,
  Dropdown,
  Empty,
  Input,
  Modal,
  Popconfirm,
  Row,
  Segmented,
  Select,
  Space,
  Statistic,
  Table,
  Tag,
  Tooltip,
  Typography,
  Upload,
  message,
} from 'antd'
import type { ColumnsType } from 'antd/es/table'
import type { UploadFile } from 'antd/es/upload/interface'
import dayjs, { type Dayjs } from 'dayjs'
import { useCallback, useEffect, useRef, useState } from 'react'
import {
  activateCmsDocumentVersion,
  deleteCmsDocument,
  deleteCmsDocumentVersion,
  listCmsDocuments,
  listCmsDocumentVersions,
  publishCmsDocument,
  retryCmsDocument,
  unpublishCmsDocument,
  updateCmsDocumentInfo,
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
import { DocumentReviewDrawer } from '../../components/cms/DocumentReviewDrawer'
import { useCmsAuth } from '../../stores/cmsAuthStore'

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

// SCR-AD-04 §3.1 - bước xử lý nói bằng lời của người dùng, không dùng từ kỹ thuật.
const STAGE_LABEL: Record<DocumentProcessingStage, string> = {
  PENDING: 'Đang chờ xử lý',
  EXTRACTION: 'Đang đọc nội dung',
  CHUNKING: 'Đang chia đoạn',
  EMBEDDING: 'Đang lập chỉ mục',
  DONE: 'Đã xử lý xong',
  ERROR: 'Lỗi xử lý',
}

function fileIconOf(source: string | null) {
  const ext = source?.split('.').pop()?.toLowerCase()
  if (ext === 'pdf') return <FilePdfOutlined className="text-red-500 text-base" />
  if (ext === 'docx') return <FileWordOutlined className="text-blue-600 text-base" />
  return <FileTextOutlined className="text-slate-400 text-base dark:text-white" />
}

export default function CmsDocumentsPage() {
  const { user } = useCmsAuth()
  // SCR-AD-04 §3.1b - chỉ Biên tập chuyên môn y khoa được xác nhận nội dung cho AI dùng.
  const canApproveContent = user?.userType === 'CONTENT_EDITOR'
  const [items, setItems] = useState<CmsDocumentItem[]>([])
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(false)

  // Filters
  const [search, setSearch] = useState('')
  const [selectedStatus, setSelectedStatus] = useState<DocumentStatus | undefined>(undefined)
  const [updatedRange, setUpdatedRange] = useState<[Dayjs, Dayjs] | null>(null)

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
  const [renameUrl, setRenameUrl] = useState('')
  // Modal xem lỗi chi tiết
  const [errorModalItem, setErrorModalItem] = useState<CmsDocumentItem | null>(null)

  // UC0049 - tai ban moi: null = tai tai lieu moi (nhu cu), co gia tri = tai phien ban moi cho tai lieu do
  const [versionTarget, setVersionTarget] = useState<CmsDocumentItem | null>(null)

  // UC0049 - ngan keo lich su phien ban
  const [historyItem, setHistoryItem] = useState<CmsDocumentItem | null>(null)
  // SCR-AD-04 §3.1b - phiên bản đang mở trong ngăn kéo "Xem & xác nhận nội dung".
  const [reviewId, setReviewId] = useState<number | null>(null)
  // Từ khoá khi mở ngăn kéo từ ô "Tìm theo nội dung" - để ngăn kéo tô vàng và lọc các đoạn khớp.
  const [reviewQuery, setReviewQuery] = useState<string | null>(null)
  // Đoạn vừa bấm trong kết quả tìm - ngăn kéo cuộn thẳng tới đó.
  const [reviewChunkIndex, setReviewChunkIndex] = useState<number | null>(null)

  function openReview(documentId: number, query: string | null = null, chunkIndex: number | null = null) {
    setReviewId(documentId)
    setReviewQuery(query)
    setReviewChunkIndex(chunkIndex)
  }
  // UC0055 - 2 kiểu tìm chung 1 chỗ (BA: cùng 1 màn): theo tên/nguồn lọc ngay trên bảng, theo nội dung
  // thì thay bảng bằng danh sách tài liệu có từ khoá.
  const [searchMode, setSearchMode] = useState<'name' | 'content'>('name')
  const [versions, setVersions] = useState<CmsDocumentVersion[]>([])
  const [versionsLoading, setVersionsLoading] = useState(false)

  const loadData = useCallback(async (silent = false) => {
    if (!silent) setLoading(true)
    try {
      const res = await listCmsDocuments({
        q: search.trim() || undefined,
        status: selectedStatus,
        updatedFrom: updatedRange?.[0].format('YYYY-MM-DD'),
        updatedTo: updatedRange?.[1].format('YYYY-MM-DD'),
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
  }, [search, selectedStatus, updatedRange, page])

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
        await uploadCmsDocumentVersion(versionTarget.id, file)
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
    setRenameUrl(item.sourceUrl ?? '')
  }

  function handleRename() {
    if (!renameTarget) return Promise.resolve()
    const target = renameTarget
    const title = renameValue.trim()
    const sourceUrl = renameUrl.trim() || null
    if (!title) {
      message.warning('Tên tài liệu không được để trống')
      return Promise.resolve()
    }
    if (title === target.title && sourceUrl === (target.sourceUrl ?? null)) {
      setRenameTarget(null)
      return Promise.resolve()
    }
    return runBusy(`rename-${target.id}`, async () => {
      try {
        await updateCmsDocumentInfo(target.id, { title, sourceUrl })
        message.success('Đã cập nhật thông tin tài liệu')
        setRenameTarget(null)
        loadData()
      } catch (err) {
        message.error(errorMessageOf(err, 'Không cập nhật được thông tin tài liệu'))
      }
    })
  }

  function renderStatusTag(status: DocumentStatus, contentApproved = true) {
    switch (status) {
      case 'DONE':
        if (!contentApproved) {
          return (
            <Tooltip title="Biên tập cần mở “Xem & xác nhận”, đối chiếu file gốc với các đoạn rồi xác nhận. Chatbot chưa dùng bản này.">
              <Tag icon={<FileSearchOutlined />} color="gold">
                Chờ xác nhận nội dung
              </Tag>
            </Tooltip>
          )
        }
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



  // SCR-AD-04 §3.1 - gộp trạng thái, phiên bản và bước xử lý thành 1 câu: tài liệu này đang thế nào với chatbot.
  function renderProcessingState(record: CmsDocumentItem) {
    const servingNote = record.currentVersionNo !== null && !record.unpublished
      ? `Chatbot vẫn đang dùng v${record.currentVersionNo}`
      : 'Chatbot chưa dùng tài liệu này'

    if (record.status === 'ERROR') {
      return (
        <div className="space-y-1">
          <Tag icon={<CloseCircleOutlined />} color="error">Lỗi xử lý v{record.versionNo}</Tag>
          {record.errorMessage && (
            <button
              type="button"
              onClick={() => setErrorModalItem(record)}
              className="block max-w-xs cursor-pointer truncate text-left text-xs text-red-500 hover:underline"
            >
              {record.errorMessage}
            </button>
          )}
          <Text type="secondary" className="block text-xs">{servingNote}</Text>
        </div>
      )
    }
    if (record.status === 'PENDING' || record.status === 'PROCESSING') {
      return (
        <div className="space-y-1">
          <Tag icon={record.status === 'PENDING' ? <ClockCircleOutlined /> : <LoadingOutlined spin />} color="processing">
            {STAGE_LABEL[record.processingStage]} v{record.versionNo}
          </Tag>
          <Text type="secondary" className="block text-xs">{servingNote}</Text>
        </div>
      )
    }
    if (!record.contentApproved) {
      return (
        <div className="space-y-1">
          <Tag icon={<FileSearchOutlined />} color="gold">Chờ xác nhận nội dung v{record.versionNo}</Tag>
          <Text type="secondary" className="block text-xs">{servingNote}</Text>
        </div>
      )
    }
    if (record.unpublished) {
      return <Tag color="default">Đã gỡ khỏi chatbot</Tag>
    }
    return (
      <Tag icon={<CheckCircleOutlined />} color="success">
        Đang dùng cho AI · v{record.currentVersionNo ?? record.versionNo}
      </Tag>
    )
  }

  function confirmTogglePublish(record: CmsDocumentItem) {
    Modal.confirm({
      title: record.unpublished ? 'Đăng lại tài liệu cho chatbot?' : 'Gỡ tài liệu khỏi chatbot?',
      content: record.unpublished
        ? 'Chatbot sẽ dùng lại phiên bản đã xác nhận nội dung mới nhất.'
        : 'Chatbot sẽ không trích dẫn tài liệu này nữa. Dữ liệu vẫn được giữ, có thể đăng lại.',
      okText: 'Đồng ý',
      cancelText: 'Hủy',
      onOk: () => handleTogglePublish(record),
    })
  }

  function confirmDelete(record: CmsDocumentItem) {
    Modal.confirm({
      title: `Xóa tài liệu và tất cả ${record.versionCount} phiên bản?`,
      content: 'Chatbot sẽ không còn trích dẫn tài liệu này cho bệnh nhân. Không thể hoàn tác.',
      okText: 'Xóa tất cả',
      okButtonProps: { danger: true },
      cancelText: 'Hủy',
      onOk: () => handleDelete(record),
    })
  }

  // SCR-AD-04 §2 - mọi thao tác gom vào 1 menu [···] có chữ, thay cho hàng biểu tượng không tên.
  function actionMenu(record: CmsDocumentItem) {
    const processing = record.status === 'PENDING' || record.status === 'PROCESSING'
    return {
      items: [
        ...(record.status === 'DONE'
          ? [{
              key: 'review',
              icon: <FileSearchOutlined />,
              label: !record.contentApproved && canApproveContent ? 'Xem & xác nhận nội dung' : 'Xem nội dung',
            }]
          : []),
        ...(record.status === 'ERROR' ? [{ key: 'retry', icon: <RedoOutlined />, label: 'Thử xử lý lại' }] : []),
        { key: 'version', icon: <CloudUploadOutlined />, label: 'Tải phiên bản mới', disabled: processing },
        { key: 'history', icon: <HistoryOutlined />, label: 'Lịch sử phiên bản' },
        { key: 'rename', icon: <EditOutlined />, label: 'Sửa tên / nguồn' },
        {
          key: 'publish',
          icon: record.unpublished ? <EyeOutlined /> : <EyeInvisibleOutlined />,
          label: record.unpublished ? 'Đăng lại cho chatbot' : 'Gỡ khỏi chatbot',
          disabled: !record.unpublished && record.currentVersionNo === null,
        },
        { type: 'divider' as const },
        { key: 'delete', icon: <DeleteOutlined />, label: 'Xóa tài liệu', danger: true },
      ],
      onClick: ({ key }: { key: string }) => {
        if (key === 'review') openReview(record.id)
        if (key === 'retry') void handleRetry(record)
        if (key === 'version') openUploadVersion(record)
        if (key === 'history') void openHistory(record)
        if (key === 'rename') openRename(record)
        if (key === 'publish') confirmTogglePublish(record)
        if (key === 'delete') confirmDelete(record)
      },
    }
  }

  const searchModeSwitch = (
    <Segmented
      value={searchMode}
      onChange={(value) => setSearchMode(value as 'name' | 'content')}
      options={[
        { label: 'Tên / nguồn', value: 'name' },
        { label: 'Nội dung', value: 'content' },
      ]}
    />
  )

  const columns: ColumnsType<CmsDocumentItem> = [
    {
      title: 'Tên tài liệu',
      key: 'title',
      render: (_, record) => (
        <div className="space-y-1">
          <div className="flex items-center gap-2">
            {fileIconOf(record.source)}
            <span className="font-semibold text-slate-800 dark:text-white">{record.title}</span>
          </div>
          {record.sourceUrl && (
            <div className="flex items-center gap-1 text-xs text-teal-700">
              <LinkOutlined />
              <a
                href={record.sourceUrl}
                target="_blank"
                rel="noreferrer"
                className="max-w-sm truncate hover:underline"
              >
                {record.sourceUrl}
              </a>
            </div>
          )}
        </div>
      ),
    },
    {
      title: 'Trạng thái xử lý',
      key: 'state',
      width: 280,
      render: (_, record) => renderProcessingState(record),
    },
    {
      title: 'Cập nhật',
      dataIndex: 'updatedAt',
      key: 'updatedAt',
      width: 130,
      render: (val: string) => (
        <span className="text-xs text-slate-500 dark:text-white">{dayjs(val).format('DD/MM/YYYY HH:mm')}</span>
      ),
    },
    {
      title: '',
      key: 'actions',
      width: 64,
      align: 'center',
      render: (_, record) => (
        <Dropdown menu={actionMenu(record)} trigger={['click']} disabled={busyKey !== null}>
          <Button
            type="text"
            icon={<MoreOutlined />}
            aria-label={`Thao tác với ${record.title}`}
            loading={busyKey !== null && busyKey.endsWith(`-${record.id}`)}
          />
        </Dropdown>
      ),
    },
  ]

  return (
    <CmsAppShell>
      <div className="p-6 w-full space-y-6">
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

          {searchMode === 'content' ? (
            <DocumentContentSearch modeSwitch={searchModeSwitch} onOpenDocument={openReview} />
          ) : (
            <>
              {/* Filter bar */}
              <Card styles={{ body: { padding: 16 } }}>
                <div className="flex flex-wrap items-center gap-3">
                  {searchModeSwitch}
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
                  <DatePicker.RangePicker
                    format="DD/MM/YYYY"
                    placeholder={['Cập nhật từ ngày', 'Đến ngày']}
                    value={updatedRange}
                    onChange={(range) => {
                      setUpdatedRange(range && range[0] && range[1] ? [range[0], range[1]] : null)
                      setPage(0)
                    }}
                    disabledDate={(date) => date.isAfter(dayjs(), 'day')}
                  />
                  {(search || selectedStatus !== undefined || updatedRange) && (
                    <Button
                      type="link"
                      onClick={() => {
                        setSearch('')
                        setSelectedStatus(undefined)
                        setUpdatedRange(null)
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
            </>
          )}
        </div>

        {/* SCR-AD-04 §3.1b - Xem file gốc cạnh các đoạn chatbot sẽ trích, rồi xác nhận */}
        <DocumentReviewDrawer
          documentId={reviewId}
          canApprove={canApproveContent}
          highlightQuery={reviewQuery}
          focusChunkIndex={reviewChunkIndex}
          onClose={() => {
            setReviewId(null)
            setReviewQuery(null)
            setReviewChunkIndex(null)
          }}
          onApproved={() => {
            loadData()
            if (historyItem) void openHistory(historyItem)
          }}
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
              {
                title: 'Trạng thái',
                dataIndex: 'status',
                width: 150,
                render: (s: DocumentStatus, v) => (
                  <Space orientation="vertical" size={2}>
                    {renderStatusTag(s, v.contentApproved)}
                    {v.contentApproved && v.contentApprovedBy && (
                      <Text type="secondary" className="text-xs">Xác nhận: {v.contentApprovedBy}</Text>
                    )}
                  </Space>
                ),
              },
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
                      {v.status === 'DONE' && !v.contentApproved ? (
                        <Button size="small" type={canApproveContent ? 'primary' : 'default'} onClick={() => openReview(v.id)}>
                          {canApproveContent ? 'Xem & xác nhận' : 'Xem nội dung'}
                        </Button>
                      ) : v.currentVersion ? (
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

            {!versionTarget && (
              <div>
                <Text className="font-semibold text-slate-700 dark:text-white block mb-1">
                  Tên tài liệu:
                </Text>
                <Input
                  placeholder="VD: Trào ngược dạ dày thực quản (GERD)"
                  value={titleInput}
                  onChange={(e) => setTitleInput(e.target.value)}
                  maxLength={255}
                  showCount
                />
              </div>
            )}

            {!versionTarget && (
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
            )}
          </div>
        </Modal>

        <Modal
          title="Sửa thông tin tài liệu"
          open={renameTarget !== null}
          onOk={handleRename}
          onCancel={() => setRenameTarget(null)}
          okText="Lưu"
          cancelText="Hủy"
          confirmLoading={renameTarget !== null && busyKey === `rename-${renameTarget.id}`}
          destroyOnHidden
        >
          <div className="space-y-4 py-2">
            <div>
              <Text className="font-semibold text-slate-700 dark:text-white block mb-1">Tên tài liệu:</Text>
              <Input
                value={renameValue}
                onChange={(e) => setRenameValue(e.target.value)}
                onPressEnter={handleRename}
                maxLength={255}
                showCount
                autoFocus
              />
            </div>
            <div>
              <Text className="font-semibold text-slate-700 dark:text-white block mb-1">URL nguồn công khai (Tùy chọn):</Text>
              <Input
                placeholder="VD: https://benhvien.vn/phac-do-dieu-tri-ibs.pdf"
                value={renameUrl}
                onChange={(e) => setRenameUrl(e.target.value)}
                onPressEnter={handleRename}
                prefix={<LinkOutlined className="text-slate-400 dark:text-white" />}
                allowClear
              />
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