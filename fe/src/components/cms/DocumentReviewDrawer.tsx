import { CheckCircleOutlined, CopyOutlined, FileSearchOutlined, LeftOutlined, RightOutlined } from '@ant-design/icons'
import { Alert, Button, Drawer, Empty, Popconfirm, Segmented, Spin, Switch, Tag, Typography, message } from 'antd'
import dayjs from 'dayjs'
import { Fragment, useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import {
  approveCmsDocumentContent,
  getCmsDocumentFile,
  getCmsDocumentReview,
  type CmsDocumentReview,
} from '../../api/cmsDocuments'
import { extractErrorMessage } from '../../lib/errors'
import { findMatches, toDisplayBlocks } from '../../lib/vietnameseText'
import { HighlightedText } from './HighlightedText'

const { Text } = Typography

/**
 * Chữ đã nối dòng: tiêu đề in đậm, đoạn thường, và bảng nhãn - giá trị (file PDF dạng bảng bị tách mỗi ô
 * 1 dòng thì gom lại thành 2 cột). Mọi chỗ đều tô vàng từ khoá.
 */
function renderBlocks(text: string, query: string | null): ReactNode {
  return toDisplayBlocks(text).map((block, index) => {
    if (block.kind === 'heading') {
      return (
        <p key={index} className="mb-1 mt-2 text-sm font-semibold text-slate-900 first:mt-0 dark:text-white">
          <HighlightedText text={block.text} query={query} />
        </p>
      )
    }
    if (block.kind === 'pairs') {
      return (
        <dl key={index} className="my-1 grid grid-cols-[minmax(0,auto)_1fr] gap-x-4 gap-y-0.5 text-sm">
          {block.rows.map(([label, value], rowIndex) => (
            <Fragment key={rowIndex}>
              <dt className="text-slate-500 dark:text-white/60">
                <HighlightedText text={label} query={query} />
              </dt>
              <dd className="m-0 text-slate-800 dark:text-white">
                {/^[—–-]$/.test(value) ? (
                  <span className="text-slate-400 dark:text-white/40">(trống)</span>
                ) : (
                  <HighlightedText text={value} query={query} />
                )}
              </dd>
            </Fragment>
          ))}
        </dl>
      )
    }
    return (
      <p key={index} className="mb-1 text-sm leading-relaxed text-slate-700 dark:text-white/90">
        <HighlightedText text={block.text} query={query} />
      </p>
    )
  })
}

/** Cách xem: 2 bên cạnh nhau để đối chiếu, hoặc phóng to 1 bên khi cần đọc kỹ. */
type ViewMode = 'split' | 'file' | 'chunks'
const VIEW_MODE_KEY = 'gastroai.cms.documentReview.viewMode'

/** Nhớ cách xem lần trước; trình duyệt chặn bộ nhớ (chế độ ẩn danh...) thì mặc định song song. */
function readViewMode(): ViewMode {
  try {
    const saved = localStorage.getItem(VIEW_MODE_KEY)
    if (saved === 'split' || saved === 'file' || saved === 'chunks') return saved
  } catch {
    // Không đọc được bộ nhớ trình duyệt - dùng mặc định.
  }
  return 'split'
}

interface DocumentReviewDrawerProps {
  /** Mã phiên bản cần xem; null là đang đóng. */
  documentId: number | null
  canApprove: boolean
  onClose: () => void
  /** Gọi sau khi xác nhận thành công để trang tải lại danh sách. */
  onApproved: () => void
  /** Từ khoá vừa tìm (mở từ ô "Tìm theo nội dung"): tô vàng, lọc và cuộn tới các đoạn khớp. */
  highlightQuery?: string | null
  /** Đoạn người dùng vừa bấm trong kết quả tìm - mở ra là cuộn thẳng tới đoạn này. */
  focusChunkIndex?: number | null
}

/**
 * SCR-AD-04 §3.1b - Biên tập đối chiếu file gốc (bên trái) với các đoạn chatbot sẽ trích dẫn
 * (bên phải) rồi mới bấm "Xác nhận dùng cho AI". Admin cũng mở được để xem, nhưng không có nút xác nhận.
 */
export function DocumentReviewDrawer({
  documentId,
  canApprove,
  onClose,
  onApproved,
  highlightQuery,
  focusChunkIndex,
}: DocumentReviewDrawerProps) {
  const [review, setReview] = useState<CmsDocumentReview | null>(null)
  const [fileUrl, setFileUrl] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [approving, setApproving] = useState(false)
  // Mở từ kết quả tìm thì mặc định chỉ hiện các đoạn có từ khoá - tài liệu dài có cả trăm đoạn.
  const [onlyMatching, setOnlyMatching] = useState(true)
  // Đoạn khớp đang xem (nút "‹ ›" chuyển qua lại giữa các đoạn khớp, giống Ctrl+F).
  const [activeChunkIndex, setActiveChunkIndex] = useState<number | null>(null)
  const textPanelRef = useRef<HTMLDivElement>(null)
  const [viewMode, setViewMode] = useState<ViewMode>(readViewMode)

  function changeViewMode(mode: ViewMode) {
    setViewMode(mode)
    try {
      localStorage.setItem(VIEW_MODE_KEY, mode)
    } catch {
      // Không lưu được thì chỉ mất phần "nhớ cho lần sau", màn hình vẫn đổi bình thường.
    }
  }
  const query = highlightQuery?.trim() || null

  useEffect(() => {
    if (documentId === null) return
    let cancelled = false
    let objectUrl: string | null = null

    async function load(id: number) {
      setLoading(true)
      setError(null)
      setReview(null)
      setFileUrl(null)
      setOnlyMatching(true)
      setActiveChunkIndex(focusChunkIndex ?? null)
      try {
        const data = await getCmsDocumentReview(id)
        if (cancelled) return
        setReview(data)
        if (data.originalFileType === 'PDF') {
          // Tải file bằng axios (có phiên đăng nhập) rồi tạo đường dẫn blob: cho iframe,
          // nên BE không phải cho phép trang khác nhúng (X-Frame-Options vẫn giữ mặc định).
          const blob = await getCmsDocumentFile(id)
          if (cancelled) return
          objectUrl = URL.createObjectURL(new Blob([blob], { type: 'application/pdf' }))
          setFileUrl(objectUrl)
        }
      } catch (err) {
        if (!cancelled) setError(extractErrorMessage(err, 'Không tải được nội dung tài liệu').message)
      } finally {
        if (!cancelled) setLoading(false)
      }
    }

    void load(documentId)
    return () => {
      cancelled = true
      // Giải phóng bộ nhớ của file PDF khi đóng ngăn kéo hoặc chuyển sang tài liệu khác.
      if (objectUrl) URL.revokeObjectURL(objectUrl)
    }
  }, [documentId, focusChunkIndex])

  async function handleApprove() {
    if (!review) return
    setApproving(true)
    try {
      await approveCmsDocumentContent(review.id)
      message.success(review.replacesExisting
        ? 'Đã xác nhận bản thay thế. Chatbot dùng bản mới, bản cũ đã bị xoá vĩnh viễn'
        : 'Đã xác nhận nội dung, chatbot bắt đầu dùng tài liệu này')
      onApproved()
      onClose()
    } catch (err) {
      message.error(extractErrorMessage(err, 'Không xác nhận được nội dung').message)
    } finally {
      setApproving(false)
    }
  }

  const awaitingApproval = review?.status === 'DONE' && !review.contentApproved

  // Các đoạn có từ khoá theo thứ tự trong tài liệu (cùng luật có dấu / không dấu với ô tìm).
  const matchingChunks = useMemo(() => {
    if (!query || !review) return [] as number[]
    return review.chunks
      .filter((chunk) => findMatches(chunk.content, query).length > 0)
      .map((chunk) => chunk.chunkIndex)
  }, [review, query])
  const filtering = query !== null && onlyMatching && matchingChunks.length > 0
  const visibleChunks = review
    ? review.chunks.filter((chunk) => !filtering || matchingChunks.includes(chunk.chunkIndex))
    : []
  // Chưa chọn đoạn nào (mở bằng nút "Mở tài liệu") thì bắt đầu từ đoạn khớp đầu tiên.
  const activePosition = Math.max(0, activeChunkIndex === null ? 0 : matchingChunks.indexOf(activeChunkIndex))
  const currentChunk = matchingChunks.length > 0 ? matchingChunks[activePosition] : null

  function goToMatch(step: number) {
    if (matchingChunks.length === 0) return
    const next = (activePosition + step + matchingChunks.length) % matchingChunks.length
    setActiveChunkIndex(matchingChunks[next])
  }

  // Cuộn tới đoạn khớp đang chọn; chữ đã trích bên trái (DOCX) cuộn tới chỗ tô vàng đầu tiên.
  useEffect(() => {
    if (currentChunk === null) return
    const frame = requestAnimationFrame(() => {
      document.getElementById(`review-chunk-${currentChunk}`)?.scrollIntoView({ block: 'start', behavior: 'smooth' })
    })
    return () => cancelAnimationFrame(frame)
  }, [currentChunk, filtering])

  useEffect(() => {
    if (!query || !review) return
    const frame = requestAnimationFrame(() => {
      textPanelRef.current?.querySelector('mark')?.scrollIntoView({ block: 'center' })
    })
    return () => cancelAnimationFrame(frame)
  }, [query, review])

  async function copyQuery() {
    if (!query) return
    try {
      await navigator.clipboard.writeText(query)
      message.success('Đã chép từ khoá, bấm vào khung PDF rồi Ctrl+F và dán vào')
    } catch {
      message.info(`Bấm vào khung PDF rồi Ctrl+F và gõ “${query}”`)
    }
  }

  return (
    <Drawer
      open={documentId !== null}
      onClose={onClose}
      width="90vw"
      destroyOnHidden
      title={
        <div className="flex flex-wrap items-center gap-2">
          <FileSearchOutlined className="text-teal-600" />
          <span>{review ? review.title : 'Xem nội dung tài liệu'}</span>
          {review?.contentApproved && <Tag color="green">Đã xác nhận</Tag>}
          {awaitingApproval && <Tag color="gold">{review?.replacesExisting ? 'Bản thay thế chờ xác nhận' : 'Chờ xác nhận nội dung'}</Tag>}
        </div>
      }
      extra={
        awaitingApproval && canApprove ? (
          <Popconfirm
            title={review?.replacesExisting ? 'Xác nhận bản thay thế?' : 'Xác nhận dùng cho AI?'}
            description={
              review?.replacesExisting ? (
                <div className="max-w-xs">
                  Bạn đã đối chiếu nội dung với nguồn y khoa đã công bố? Bản đang dùng sẽ bị{' '}
                  <strong>xoá vĩnh viễn</strong> và <strong>không thể khôi phục</strong>.
                </div>
              ) : (
                'Bạn đã đối chiếu nội dung với nguồn y khoa đã công bố (phác đồ, hướng dẫn điều trị chính thức)?'
              )
            }
            okText={review?.replacesExisting ? 'Xác nhận, xoá bản cũ' : 'Xác nhận dùng cho AI'}
            okButtonProps={review?.replacesExisting ? { danger: true } : undefined}
            cancelText="Hủy"
            onConfirm={handleApprove}
          >
            <Button type="primary" icon={<CheckCircleOutlined />} loading={approving}>
              Xác nhận dùng cho AI
            </Button>
          </Popconfirm>
        ) : awaitingApproval ? (
          <Text type="secondary">Chờ Biên tập chuyên môn xác nhận</Text>
        ) : null
      }
    >
      {loading && (
        <div className="flex h-full items-center justify-center">
          <Spin />
        </div>
      )}
      {error && <Alert type="error" message={error} showIcon />}
      {review && !loading && awaitingApproval && review.replacesExisting && (
        <Alert
          type="warning"
          showIcon
          className="mb-4"
          message="Đây là bản thay thế"
          description="Chatbot vẫn đang dùng bản cũ. Khi bạn xác nhận bản này, bản cũ sẽ bị xoá vĩnh viễn (cả nội dung đã chia đoạn) và không thể khôi phục."
        />
      )}
      {review && !loading && (
        <div className="flex h-full flex-col gap-4">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <Text type="secondary" className="text-xs">
              {review.contentApproved && review.contentApprovedBy
                ? `Xác nhận bởi ${review.contentApprovedBy}${
                    review.contentApprovedAt ? ` lúc ${dayjs(review.contentApprovedAt).format('DD/MM/YYYY HH:mm')}` : ''
                  }`
                : ''}
            </Text>
            <Segmented
              size="small"
              value={viewMode}
              onChange={(value) => changeViewMode(value as ViewMode)}
              options={[
                { label: 'Song song', value: 'split' },
                { label: 'File gốc', value: 'file' },
                { label: 'Các đoạn', value: 'chunks' },
              ]}
            />
          </div>
          <div className={`grid min-h-0 flex-1 grid-cols-1 gap-4 ${viewMode === 'split' ? 'lg:grid-cols-5' : ''}`}>
            {viewMode !== 'chunks' && (
              <section
                className={`flex min-h-[60vh] flex-col rounded-xl border border-black/10 dark:border-white/10 ${
                  viewMode === 'split' ? 'lg:col-span-3' : ''
                }`}
              >
                <div className="flex flex-wrap items-center justify-between gap-2 border-b border-black/10 px-4 py-2 dark:border-white/10">
                  <Text strong>File gốc</Text>
                  {fileUrl && query && (
                    // Trình xem PDF có sẵn của trình duyệt không cho trang web tô chữ bên trong,
                    // nên chỉ giúp chép từ khoá để tìm bằng Ctrl+F.
                    <Button size="small" type="link" icon={<CopyOutlined />} onClick={() => void copyQuery()}>
                      Chép “{query}” để tìm bằng Ctrl+F
                    </Button>
                  )}
                </div>
                {fileUrl ? (
                  // view=FitH: vừa khít bề ngang; navpanes=0: ẩn cột ảnh thu nhỏ. Chrome/Edge làm theo,
                  // trình duyệt khác bỏ qua và hiện như mặc định.
                  <iframe
                    title="File gốc của tài liệu"
                    src={`${fileUrl}#view=FitH&navpanes=0`}
                    className="min-h-0 w-full flex-1 rounded-b-xl"
                  />
                ) : review.fullText ? (
                  <div ref={textPanelRef} className="min-h-0 flex-1 overflow-y-auto px-4 py-3">
                    <Text type="secondary" className="mb-2 block text-xs">
                      {review.originalFileType === 'DOCX'
                        ? 'Trình duyệt không xem trực tiếp được file Word, đây là chữ hệ thống đã trích ra.'
                        : 'Tài liệu không còn file gốc, đây là chữ hệ thống đã trích ra.'}
                    </Text>
                    {renderBlocks(review.fullText, query)}
                  </div>
                ) : (
                  <Empty className="my-auto" description="Không có nội dung để hiển thị" />
                )}
              </section>
            )}
            {viewMode !== 'file' && (
              <section
                className={`flex min-h-[60vh] flex-col rounded-xl border border-black/10 dark:border-white/10 ${
                  viewMode === 'split' ? 'lg:col-span-2' : ''
                }`}
              >
                <div className="flex flex-wrap items-center justify-between gap-2 border-b border-black/10 px-4 py-2 dark:border-white/10">
                  <Text strong>Các đoạn chatbot sẽ trích dẫn ({review.chunks.length})</Text>
                  {query && matchingChunks.length > 0 && (
                    <div className="flex flex-wrap items-center gap-3 text-xs">
                      <span className="flex items-center gap-1">
                        <Button size="small" type="text" icon={<LeftOutlined />} aria-label="Đoạn khớp trước" onClick={() => goToMatch(-1)} />
                        <span className="tabular-nums">
                          Đoạn khớp {activePosition + 1}/{matchingChunks.length}
                        </span>
                        <Button size="small" type="text" icon={<RightOutlined />} aria-label="Đoạn khớp sau" onClick={() => goToMatch(1)} />
                      </span>
                      <label className="flex cursor-pointer items-center gap-2">
                        <Switch size="small" checked={onlyMatching} onChange={setOnlyMatching} />
                        Chỉ đoạn có “{query}”
                      </label>
                    </div>
                  )}
                  {query && matchingChunks.length === 0 && (
                    <Text type="secondary" className="text-xs">
                      Phiên bản này không có đoạn nào chứa “{query}”
                    </Text>
                  )}
                </div>
                <div className="min-h-0 flex-1 space-y-3 overflow-y-auto px-4 py-3">
                  {review.chunks.length === 0 ? (
                    <Empty description="Chưa có đoạn nào" />
                  ) : (
                    visibleChunks.map((chunk) => (
                      <div
                        key={chunk.chunkIndex}
                        id={`review-chunk-${chunk.chunkIndex}`}
                        className={`scroll-mt-3 rounded-lg p-3 transition-colors ${
                          chunk.chunkIndex === currentChunk
                            ? 'bg-amber-50 ring-2 ring-amber-400 dark:bg-amber-500/10'
                            : matchingChunks.includes(chunk.chunkIndex)
                              ? 'bg-slate-50 ring-1 ring-amber-200 dark:bg-white/5'
                              : 'bg-slate-50 dark:bg-white/5'
                        }`}
                      >
                        <span className="mb-1 inline-block rounded bg-white px-1.5 py-0.5 text-xs text-slate-500 dark:bg-white/10 dark:text-white/70">
                          Đoạn {chunk.chunkIndex + 1}
                        </span>
                        {renderBlocks(chunk.content, query)}
                      </div>
                    ))
                  )}
                </div>
              </section>
            )}
          </div>
        </div>
      )}
    </Drawer>
  )
}