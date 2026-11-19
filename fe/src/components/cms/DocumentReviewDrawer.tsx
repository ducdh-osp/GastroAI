import { CheckCircleOutlined, FileSearchOutlined } from '@ant-design/icons'
import { Alert, Button, Drawer, Empty, Popconfirm, Spin, Tag, Typography, message } from 'antd'
import dayjs from 'dayjs'
import { useEffect, useState } from 'react'
import {
  approveCmsDocumentContent,
  getCmsDocumentFile,
  getCmsDocumentReview,
  type CmsDocumentReview,
} from '../../api/cmsDocuments'
import { extractErrorMessage } from '../../lib/errors'

const { Text, Paragraph } = Typography

interface DocumentReviewDrawerProps {
  /** Mã phiên bản cần xem; null là đang đóng. */
  documentId: number | null
  canApprove: boolean
  onClose: () => void
  /** Gọi sau khi xác nhận thành công để trang tải lại danh sách. */
  onApproved: () => void
}

export function DocumentReviewDrawer({ documentId, canApprove, onClose, onApproved }: DocumentReviewDrawerProps) {
  const [review, setReview] = useState<CmsDocumentReview | null>(null)
  const [fileUrl, setFileUrl] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [approving, setApproving] = useState(false)

  useEffect(() => {
    if (documentId === null) return
    let cancelled = false
    let objectUrl: string | null = null

    async function load(id: number) {
      setLoading(true)
      setError(null)
      setReview(null)
      setFileUrl(null)
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
  }, [documentId])

  async function handleApprove() {
    if (!review) return
    setApproving(true)
    try {
      await approveCmsDocumentContent(review.id, review.id)
      message.success(`Đã xác nhận nội dung v${review.versionNo}, chatbot bắt đầu dùng tài liệu này`)
      onApproved()
      onClose()
    } catch (err) {
      message.error(extractErrorMessage(err, 'Không xác nhận được nội dung').message)
    } finally {
      setApproving(false)
    }
  }

  const awaitingApproval = review?.status === 'DONE' && !review.contentApproved

  return (
    <Drawer
      open={documentId !== null}
      onClose={onClose}
      width="90vw"
      destroyOnHidden
      title={
        <div className="flex flex-wrap items-center gap-2">
          <FileSearchOutlined className="text-teal-600" />
          <span>{review ? `${review.title} · v${review.versionNo}` : 'Xem nội dung tài liệu'}</span>
          {review?.contentApproved && <Tag color="green">Đã xác nhận</Tag>}
          {awaitingApproval && <Tag color="gold">Chờ xác nhận nội dung</Tag>}
        </div>
      }
      extra={
        awaitingApproval && canApprove ? (
          <Popconfirm
            title="Xác nhận dùng cho AI?"
            description="Bạn đã đối chiếu nội dung với nguồn y khoa đã công bố (phác đồ, hướng dẫn điều trị chính thức)?"
            okText="Xác nhận dùng cho AI"
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
      {review && !loading && (
        <div className="flex h-full flex-col gap-4">
          {review.contentApproved && review.contentApprovedBy && (
            <Text type="secondary" className="text-xs">
              Xác nhận bởi {review.contentApprovedBy}
              {review.contentApprovedAt && ` lúc ${dayjs(review.contentApprovedAt).format('DD/MM/YYYY HH:mm')}`}
            </Text>
          )}
          <div className="grid min-h-0 flex-1 grid-cols-1 gap-4 lg:grid-cols-2">
            <section className="flex min-h-[60vh] flex-col rounded-xl border border-black/10 dark:border-white/10">
              <div className="border-b border-black/10 px-4 py-2 dark:border-white/10">
                <Text strong>File gốc</Text>
              </div>
              {fileUrl ? (
                <iframe title="File gốc của tài liệu" src={fileUrl} className="min-h-0 w-full flex-1 rounded-b-xl" />
              ) : review.fullText ? (
                <div className="min-h-0 flex-1 overflow-y-auto px-4 py-3">
                  <Text type="secondary" className="mb-2 block text-xs">
                    {review.originalFileType === 'DOCX'
                      ? 'Trình duyệt không xem trực tiếp được file Word, đây là chữ hệ thống đã trích ra.'
                      : 'Tài liệu không còn file gốc, đây là chữ hệ thống đã trích ra.'}
                  </Text>
                  <Paragraph className="whitespace-pre-wrap text-sm">{review.fullText}</Paragraph>
                </div>
              ) : (
                <Empty className="my-auto" description="Không có nội dung để hiển thị" />
              )}
            </section>
            <section className="flex min-h-[60vh] flex-col rounded-xl border border-black/10 dark:border-white/10">
              <div className="border-b border-black/10 px-4 py-2 dark:border-white/10">
                <Text strong>Các đoạn chatbot sẽ trích dẫn ({review.chunks.length})</Text>
              </div>
              <div className="min-h-0 flex-1 space-y-3 overflow-y-auto px-4 py-3">
                {review.chunks.length === 0 ? (
                  <Empty description="Chưa có đoạn nào" />
                ) : (
                  review.chunks.map((chunk) => (
                    <div key={chunk.chunkIndex} className="rounded-lg bg-slate-50 p-3 dark:bg-white/5">
                      <Text type="secondary" className="text-xs">Đoạn {chunk.chunkIndex + 1}</Text>
                      <Paragraph className="mb-0! mt-1 whitespace-pre-wrap text-sm">{chunk.content}</Paragraph>
                    </div>
                  ))
                )}
              </div>
            </section>
          </div>
        </div>
      )}
    </Drawer>
  )
}