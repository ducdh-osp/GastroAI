import { CalendarOutlined, DeleteOutlined, EditOutlined, FileTextOutlined } from '@ant-design/icons'
import {
  Alert,
  Button,
  Card,
  Empty,
  Pagination,
  Popconfirm,
  Segmented,
  Spin,
  Tag,
  Typography,
  message,
  notification,
} from 'antd'
import { useCallback, useEffect, useRef } from 'react'
import { deleteBristolLog, listBristolLogs, restoreBristolLog } from '../../api/bristol'
import type { BristolLog } from '../../api/bristol'
import { BRISTOL_TYPE_LABELS } from '../../constants/bristol'
import { useInFlightGuard } from '../../hooks/useInFlightGuard'
import { usePagedList } from '../../hooks/usePagedList'
import { BristolTrashTable } from './BristolTrashTable'

const { Text, Title } = Typography
const VIETNAM_TIME_ZONE = 'Asia/Ho_Chi_Minh'

function vietnamDateKey(value: string | Date): string {
  const parts = new Intl.DateTimeFormat('en-CA', {
    timeZone: VIETNAM_TIME_ZONE,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).formatToParts(typeof value === 'string' ? new Date(value) : value)
  const get = (type: Intl.DateTimeFormatPartTypes) => parts.find((part) => part.type === type)?.value ?? ''
  return `${get('year')}-${get('month')}-${get('day')}`
}

function dayHeading(dateKey: string): string {
  const today = vietnamDateKey(new Date())
  const yesterday = vietnamDateKey(new Date(Date.now() - 24 * 60 * 60 * 1000))
  if (dateKey === today) return 'Hôm nay'
  if (dateKey === yesterday) return 'Hôm qua'

  const label = new Intl.DateTimeFormat('vi-VN', {
    timeZone: VIETNAM_TIME_ZONE,
    weekday: 'long',
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  }).format(new Date(`${dateKey}T00:00:00+07:00`))
  return label.charAt(0).toUpperCase() + label.slice(1)
}

function formatTime(value: string): string {
  return new Intl.DateTimeFormat('vi-VN', {
    timeZone: VIETNAM_TIME_ZONE,
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(new Date(value))
}

function formatFilterDate(dateKey: string): string {
  const [year, month, day] = dateKey.split('-')
  return `${day}/${month}/${year}`
}

function typeColor(type: number): string {
  if (type <= 2) return 'orange'
  if (type >= 6) return 'red'
  return 'green'
}

interface BristolJournalTabProps {
  view: 'list' | 'trash'
  date?: string
  reloadVersion: number
  onViewChange: (view: 'list' | 'trash') => void
  onClearFilter: () => void
  onEdit: (log: BristolLog) => void
  onMutated: () => void
}

export function BristolJournalTab({
  view,
  date,
  reloadVersion,
  onViewChange,
  onClearFilter,
  onEdit,
  onMutated,
}: BristolJournalTabProps) {
  const fetchPage = useCallback(
    (page: number, size: number) => listBristolLogs(page, size, date),
    [date],
  )
  const {
    items,
    loading,
    error,
    setError,
    page,
    setPage,
    pageSize,
    totalElements,
    reload,
  } = usePagedList({
    fetchPage,
    pageSize: 10,
    loadErrorMessage: 'Không thể tải nhật ký Bristol.',
  })
  const deleteGuard = useInFlightGuard<number>()
  const previousReloadVersion = useRef(reloadVersion)

  useEffect(() => {
    if (previousReloadVersion.current === reloadVersion) return
    previousReloadVersion.current = reloadVersion
    void reload()
  }, [reload, reloadVersion])

  useEffect(() => {
    setPage(0)
  }, [date, setPage])

  async function handleDelete(id: number) {
    await deleteGuard.run(id, async () => {
      try {
        await deleteBristolLog(id)
        if (items.length === 1 && page > 0) setPage(page - 1)
        onMutated()
        notification.open({
          key: `bristol-undo-${id}`,
          message: 'Đã chuyển vào thùng rác',
          description: 'Bạn có thể khôi phục ngay hoặc mở mục Thùng rác.',
          duration: 6,
          actions: (
            <Button
              size="small"
              type="primary"
              onClick={() => {
                notification.destroy(`bristol-undo-${id}`)
                void handleUndo(id)
              }}
            >
              Hoàn tác
            </Button>
          ),
        })
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Không thể xoá ghi nhận Bristol.')
      }
    })
  }

  async function handleUndo(id: number) {
    try {
      await restoreBristolLog(id)
      onMutated()
      message.success('Đã khôi phục ghi nhận Bristol.')
    } catch (err) {
      message.error(err instanceof Error ? err.message : 'Không thể hoàn tác.')
    }
  }

  const groups = items.reduce<Array<{ date: string; logs: BristolLog[] }>>((result, log) => {
    const logDate = vietnamDateKey(log.loggedAt)
    const currentGroup = result.at(-1)
    if (currentGroup?.date === logDate) currentGroup.logs.push(log)
    else result.push({ date: logDate, logs: [log] })
    return result
  }, [])

  return (
    <div>
      <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
        <Segmented
          value={view}
          options={[
            { value: 'list', label: 'Nhật ký' },
            { value: 'trash', label: 'Thùng rác' },
          ]}
          onChange={(value) => onViewChange(value as 'list' | 'trash')}
        />
        {view === 'list' && <Text type="secondary">{totalElements} lần ghi nhận</Text>}
      </div>

      {view === 'trash' ? (
        <Card className="rounded-2xl border-black/5 shadow-sm dark:border-white/10! dark:bg-neutral-900!">
          <BristolTrashTable onRestored={onMutated} />
        </Card>
      ) : (
        <>
          {date && (
            <Alert
              className="mb-6"
              type="info"
              showIcon
              message={`Đang hiển thị các ghi nhận ngày ${formatFilterDate(date)}`}
              action={<Button size="small" onClick={onClearFilter}>Bỏ lọc</Button>}
            />
          )}
          {error && <Alert type="error" message={error} showIcon className="mb-6" />}

          <Spin spinning={loading}>
            {!loading && groups.length === 0 ? (
              <Card className="rounded-2xl border-black/5 shadow-sm dark:border-white/10! dark:bg-neutral-900!">
                <Empty
                  image={Empty.PRESENTED_IMAGE_SIMPLE}
                  description={date ? 'Không có ghi nhận Bristol trong ngày này' : 'Chưa có ghi nhận Bristol nào'}
                />
              </Card>
            ) : (
              <div className="grid gap-6 pb-2">
                {groups.map((group) => (
                  <Card
                    key={group.date}
                    className="overflow-hidden rounded-2xl border-slate-200/80 shadow-[0_8px_24px_rgba(15,23,42,0.06)] transition-shadow hover:shadow-[0_12px_32px_rgba(15,23,42,0.09)] dark:border-white/10! [&_.ant-card-head]:bg-slate-50/80 dark:[&_.ant-card-head]:bg-white/[0.025]"
                    styles={{ body: { padding: 0 } }}
                    title={(
                      <span className="flex items-center gap-2.5">
                        <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-teal-100 text-teal-700 dark:bg-teal-500/15 dark:text-teal-300">
                          <CalendarOutlined />
                        </span>
                        <span>{dayHeading(group.date)}</span>
                        <Tag bordered={false} className="m-0 ml-1 rounded-full px-2.5 font-normal">
                          {group.logs.length} lần
                        </Tag>
                      </span>
                    )}
                  >
                    {group.logs.map((log) => (
                      <div
                        key={log.id}
                        className="flex flex-col gap-3 border-b border-slate-100 px-5 py-5 transition-colors last:border-b-0 hover:bg-teal-50/35 dark:border-white/5 dark:hover:bg-white/[0.025] sm:flex-row sm:items-start"
                      >
                        <Text strong className="shrink-0 text-base sm:w-20">{formatTime(log.loggedAt)}</Text>

                        <div className="min-w-0 flex-1">
                          <div className="mb-2 flex flex-wrap items-center gap-2">
                            <Title level={5} className="m-0!">Loại {log.bristolType}</Title>
                            <Tag color={typeColor(log.bristolType)} className="m-0">
                              {log.bristolType >= 3 && log.bristolType <= 5 ? 'Trong khoảng bình thường' : 'Cần theo dõi'}
                            </Tag>
                          </div>
                          <Text className="block">{BRISTOL_TYPE_LABELS[log.bristolType] ?? `Loại ${log.bristolType}`}</Text>
                          {log.notes && (
                            <div className="mt-1 flex items-start gap-1.5 text-sm">
                              <FileTextOutlined className="mt-1 text-slate-400" />
                              <Text type="secondary">{log.notes}</Text>
                            </div>
                          )}
                        </div>

                        <div className="flex shrink-0 justify-end gap-1">
                          <Button
                            size="small"
                            type="text"
                            icon={<EditOutlined />}
                            aria-label={`Sửa ghi nhận loại ${log.bristolType}`}
                            onClick={() => onEdit(log)}
                          />
                          <Popconfirm
                            title="Chuyển vào thùng rác?"
                            description="Bạn có thể khôi phục lại trong mục Thùng rác."
                            okText="Chuyển vào thùng rác"
                            cancelText="Huỷ"
                            onConfirm={() => handleDelete(log.id)}
                          >
                            <Button
                              size="small"
                              type="text"
                              danger
                              icon={<DeleteOutlined />}
                              aria-label={`Xoá ghi nhận loại ${log.bristolType}`}
                              loading={deleteGuard.inFlightId === log.id}
                              disabled={deleteGuard.inFlightId !== null && deleteGuard.inFlightId !== log.id}
                            />
                          </Popconfirm>
                        </div>
                      </div>
                    ))}
                  </Card>
                ))}
              </div>
            )}
          </Spin>

          {totalElements > pageSize && (
            <div className="mt-5 flex justify-center sm:justify-end">
              <Pagination
                current={page + 1}
                pageSize={pageSize}
                total={totalElements}
                showSizeChanger={false}
                onChange={(nextPage) => setPage(nextPage - 1)}
              />
            </div>
          )}
        </>
      )}
    </div>
  )
}
