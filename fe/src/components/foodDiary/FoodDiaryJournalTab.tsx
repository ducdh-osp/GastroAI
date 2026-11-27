import {
  CalendarOutlined,
  DeleteOutlined,
  EditOutlined,
  FileTextOutlined,
} from '@ant-design/icons'
import {
  Alert,
  Button,
  Card,
  Empty,
  Input,
  Pagination,
  Popconfirm,
  Segmented,
  Select,
  Spin,
  Tag,
  Typography,
  message,
  notification,
} from 'antd'
import { useCallback, useEffect, useRef, useState } from 'react'
import {
  deleteFoodDiaryEntry,
  listFoodDiary,
  restoreFoodDiaryEntry,
} from '../../api/foodDiary'
import type { FoodDiaryEntry, MealType } from '../../api/foodDiary'
import { useInFlightGuard } from '../../hooks/useInFlightGuard'
import { usePagedList } from '../../hooks/usePagedList'
import { FoodDiaryTrashTable } from './FoodDiaryTrashTable'

const { Text, Title } = Typography
const VIETNAM_TIME_ZONE = 'Asia/Ho_Chi_Minh'

const MEAL_TYPE_META: Record<MealType, { label: string; color: string }> = {
  BREAKFAST: { label: 'Bữa sáng', color: 'gold' },
  LUNCH: { label: 'Bữa trưa', color: 'cyan' },
  DINNER: { label: 'Bữa tối', color: 'purple' },
  SNACK: { label: 'Bữa phụ', color: 'lime' },
  OTHER: { label: 'Khác', color: 'default' },
}

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

  const date = new Date(`${dateKey}T00:00:00+07:00`)
  const label = new Intl.DateTimeFormat('vi-VN', {
    timeZone: VIETNAM_TIME_ZONE,
    weekday: 'long',
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  }).format(date)
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

interface FoodDiaryJournalTabProps {
  view: 'list' | 'trash'
  date?: string
  keyword?: string
  keywordDays?: number
  reloadVersion: number
  onViewChange: (view: 'list' | 'trash') => void
  onClearFilters: () => void
  onKeywordChange: (keyword: string) => void
  onKeywordDaysChange: (days: 30 | 90 | 180) => void
  onEdit: (entry: FoodDiaryEntry) => void
  onMutated: () => void
}

export function FoodDiaryJournalTab({
  view,
  date,
  keyword,
  keywordDays = 90,
  reloadVersion,
  onViewChange,
  onClearFilters,
  onKeywordChange,
  onKeywordDaysChange,
  onEdit,
  onMutated,
}: FoodDiaryJournalTabProps) {
  const fetchPage = useCallback(
    (page: number, size: number) => listFoodDiary(page, size, {
      ...(date ? { date } : {}),
      ...(keyword ? { keyword, days: keywordDays } : {}),
    }),
    [date, keyword, keywordDays],
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
    loadErrorMessage: 'Không thể tải nhật ký ăn uống.',
  })
  const deleteGuard = useInFlightGuard<number>()
  const previousReloadVersion = useRef(reloadVersion)
  const [keywordInput, setKeywordInput] = useState(keyword ?? '')

  useEffect(() => {
    setKeywordInput(keyword ?? '')
  }, [keyword])

  useEffect(() => {
    if (previousReloadVersion.current === reloadVersion) return
    previousReloadVersion.current = reloadVersion
    void reload()
  }, [reload, reloadVersion])

  useEffect(() => {
    setPage(0)
  }, [date, keyword, keywordDays, setPage])

  async function handleDelete(id: number) {
    await deleteGuard.run(id, async () => {
      try {
        await deleteFoodDiaryEntry(id)
        if (items.length === 1 && page > 0) setPage(page - 1)
        onMutated()
        notification.open({
          key: `food-diary-undo-${id}`,
          message: 'Đã chuyển vào thùng rác',
          description: 'Bạn có thể khôi phục ngay hoặc mở mục Thùng rác.',
          duration: 6,
          actions: (
            <Button
              size="small"
              type="primary"
              onClick={() => {
                notification.destroy(`food-diary-undo-${id}`)
                void handleUndo(id)
              }}
            >
              Hoàn tác
            </Button>
          ),
        })
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Không thể xoá nhật ký ăn uống.')
      }
    })
  }

  async function handleUndo(id: number) {
    try {
      await restoreFoodDiaryEntry(id)
      onMutated()
      message.success('Đã khôi phục bữa ăn.')
    } catch (err) {
      message.error(err instanceof Error ? err.message : 'Không thể hoàn tác.')
    }
  }

  const groups = items.reduce<Array<{ date: string; entries: FoodDiaryEntry[] }>>((result, entry) => {
    const entryDate = vietnamDateKey(entry.eatenAt)
    const currentGroup = result.at(-1)
    if (currentGroup?.date === entryDate) currentGroup.entries.push(entry)
    else result.push({ date: entryDate, entries: [entry] })
    return result
  }, [])

  const hasFilters = Boolean(date || keyword)
  const filterDescription = [
    date ? `ngày ${formatFilterDate(date)}` : null,
    keyword ? `có món chứa từ khóa “${keyword}” trong ${keywordDays} ngày gần nhất` : null,
  ].filter(Boolean).join(' và ')

  function applyKeywordFilter(value: string) {
    const nextKeyword = value.trim()
    setKeywordInput(nextKeyword)
    onKeywordChange(nextKeyword)
  }

  return (
    <div>
      <div className="mb-6 flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
        <Segmented
          value={view}
          options={[
            { value: 'list', label: 'Nhật ký' },
            { value: 'trash', label: 'Thùng rác' },
          ]}
          onChange={(value) => onViewChange(value as 'list' | 'trash')}
        />
        {view === 'list' && (
          <div className="flex flex-col gap-2 sm:flex-row sm:items-center">
            <Input.Search
              allowClear
              className="w-full sm:w-72"
              enterButton="Lọc"
              maxLength={100}
              placeholder="Lọc theo tên món ăn"
              value={keywordInput}
              onChange={(event) => {
                const value = event.target.value
                setKeywordInput(value)
                if (!value && keyword) onKeywordChange('')
              }}
              onSearch={applyKeywordFilter}
            />
            <Select
              aria-label="Khoảng thời gian lọc món ăn"
              className="w-full sm:w-32"
              disabled={!keyword}
              value={keywordDays as 30 | 90 | 180}
              options={[
                { value: 30, label: '30 ngày' },
                { value: 90, label: '90 ngày' },
                { value: 180, label: '180 ngày' },
              ]}
              onChange={onKeywordDaysChange}
            />
            <Text type="secondary" className="whitespace-nowrap sm:ml-1">{totalElements} bữa ăn</Text>
          </div>
        )}
      </div>

      {view === 'trash' ? (
        <Card className="rounded-2xl border-black/5 shadow-sm">
          <FoodDiaryTrashTable onRestored={onMutated} />
        </Card>
      ) : (
        <>
          {hasFilters && (
            <Alert
              className="mb-4"
              type="info"
              showIcon
              message={`Đang hiển thị ${filterDescription}`}
              action={<Button size="small" onClick={onClearFilters}>Bỏ lọc</Button>}
            />
          )}
          {error && <Alert type="error" message={error} showIcon className="mb-4" />}

          <Spin spinning={loading}>
            {!loading && groups.length === 0 ? (
              <Card className="rounded-2xl border-black/5 shadow-sm">
                <Empty
                  image={Empty.PRESENTED_IMAGE_SIMPLE}
                  description={hasFilters ? 'Không tìm thấy bữa ăn phù hợp' : 'Chưa có bữa ăn nào được ghi nhận'}
                />
              </Card>
            ) : (
              <div className="grid gap-6 pb-2">
                {groups.map((group) => (
                  <Card
                    key={group.date}
                    className="overflow-hidden rounded-2xl border-slate-200/80 shadow-[0_8px_24px_rgba(15,23,42,0.06)] transition-shadow hover:shadow-[0_12px_32px_rgba(15,23,42,0.09)] dark:border-white/10 [&_.ant-card-head]:bg-slate-50/80 dark:[&_.ant-card-head]:bg-white/[0.025]"
                    styles={{ body: { padding: 0 } }}
                    title={(
                      <span className="flex items-center gap-2.5">
                        <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-teal-100 text-teal-700 dark:bg-teal-500/15 dark:text-teal-300">
                          <CalendarOutlined />
                        </span>
                        <span>{dayHeading(group.date)}</span>
                        <Tag bordered={false} className="m-0 ml-1 rounded-full px-2.5 font-normal">
                          {group.entries.length} bữa
                        </Tag>
                      </span>
                    )}
                  >
                    {group.entries.map((entry) => {
                      const mealMeta = MEAL_TYPE_META[entry.mealType]
                      return (
                        <div
                          key={entry.id}
                          className="flex flex-col gap-3 border-b border-slate-100 px-5 py-5 transition-colors last:border-b-0 hover:bg-teal-50/35 dark:border-white/5 dark:hover:bg-white/[0.025] sm:flex-row sm:items-start"
                        >
                          <div className="flex shrink-0 items-center gap-2 sm:w-32">
                            <Text strong className="text-base">{formatTime(entry.eatenAt)}</Text>
                            <Tag color={mealMeta.color} className="m-0 sm:hidden">{mealMeta.label}</Tag>
                          </div>

                          <div className="min-w-0 flex-1">
                            <div className="mb-2 flex flex-wrap items-center gap-2">
                              <Title level={5} className="m-0!">{entry.description}</Title>
                              <Tag color={mealMeta.color} className="m-0 hidden sm:inline-block">{mealMeta.label}</Tag>
                            </div>
                            {entry.symptomsAfterMeal ? (
                              <div className="mb-1 text-sm text-orange-700 dark:text-orange-300">
                                <Text strong>Triệu chứng: </Text>
                                {entry.symptomsAfterMeal}
                                {entry.symptomOnsetMinutes !== null && ` (sau ${entry.symptomOnsetMinutes} phút)`}
                              </div>
                            ) : (
                              <Text type="secondary" className="mb-1 block text-sm">Không ghi nhận triệu chứng sau ăn</Text>
                            )}
                            {entry.notes && (
                              <div className="flex items-start gap-1.5 text-sm">
                                <FileTextOutlined className="mt-1 text-slate-400" />
                                <Text type="secondary">{entry.notes}</Text>
                              </div>
                            )}
                          </div>

                          <div className="flex shrink-0 justify-end gap-1">
                            <Button
                              size="small"
                              type="text"
                              icon={<EditOutlined />}
                              aria-label={`Sửa ${entry.description}`}
                              onClick={() => onEdit(entry)}
                            />
                            <Popconfirm
                              title="Chuyển vào thùng rác?"
                              description="Bạn có thể khôi phục lại trong mục Thùng rác."
                              okText="Chuyển vào thùng rác"
                              cancelText="Huỷ"
                              onConfirm={() => handleDelete(entry.id)}
                            >
                              <Button
                                size="small"
                                type="text"
                                danger
                                icon={<DeleteOutlined />}
                                aria-label={`Xoá ${entry.description}`}
                                loading={deleteGuard.inFlightId === entry.id}
                                disabled={deleteGuard.inFlightId !== null && deleteGuard.inFlightId !== entry.id}
                              />
                            </Popconfirm>
                          </div>
                        </div>
                      )
                    })}
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
