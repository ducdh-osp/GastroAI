import { Alert, Typography } from 'antd'
import { useState } from 'react'
import { Navigate, useSearchParams } from 'react-router-dom'
import { createFoodDiaryEntry, updateFoodDiaryEntry } from '../../api/foodDiary'
import type { FoodDiaryEntry, FoodDiaryEntryRequest } from '../../api/foodDiary'
import { FoodDiaryEntryModal } from '../../components/foodDiary/FoodDiaryEntryModal'
import { FoodDiaryJournalTab } from '../../components/foodDiary/FoodDiaryJournalTab'
import { JournalActions } from '../../components/healthReport/JournalActions'
import { AppShell } from '../../components/layout/AppShell'

const { Title, Text } = Typography
function parseOption<T extends number>(value: string | null, options: readonly T[], fallback: T): T {
  const parsed = Number(value)
  return options.includes(parsed as T) ? parsed as T : fallback
}

export default function FoodDiaryPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const requestedTab = searchParams.get('tab')
  const journalView = searchParams.get('view') === 'trash' ? 'trash' : 'list'
  const date = searchParams.get('date') || undefined
  const keyword = searchParams.get('keyword') || undefined
  const keywordDays = parseOption(searchParams.get('keywordDays'), [30, 90, 180] as const, 90)

  const [modalOpen, setModalOpen] = useState(false)
  const [editingEntry, setEditingEntry] = useState<FoodDiaryEntry | null>(null)
  const [saving, setSaving] = useState(false)
  const [actionError, setActionError] = useState<string | null>(null)
  const [reloadVersion, setReloadVersion] = useState(0)

  function updateSearch(changes: Record<string, string | number | null>, replace = true) {
    setSearchParams((current) => {
      const next = new URLSearchParams(current)
      Object.entries(changes).forEach(([key, value]) => {
        if (value === null) next.delete(key)
        else next.set(key, String(value))
      })
      return next
    }, { replace })
  }

  function openCreate() {
    setActionError(null)
    setEditingEntry(null)
    setModalOpen(true)
  }

  function openEdit(entry: FoodDiaryEntry) {
    setActionError(null)
    setEditingEntry(entry)
    setModalOpen(true)
  }

  async function handleSubmit(payload: FoodDiaryEntryRequest) {
    const isEditing = editingEntry !== null
    setSaving(true)
    setActionError(null)
    try {
      if (editingEntry) await updateFoodDiaryEntry(editingEntry.id, payload)
      else await createFoodDiaryEntry(payload)

      setModalOpen(false)
      setEditingEntry(null)
      setReloadVersion((version) => version + 1)
      updateSearch({
        tab: null,
        view: null,
        ...(isEditing ? {} : { date: null, keyword: null, keywordDays: null }),
      })
    } catch (err) {
      setActionError(err instanceof Error ? err.message : 'Không thể lưu nhật ký ăn uống.')
    } finally {
      setSaving(false)
    }
  }

  if (requestedTab === 'trend') {
    const days = parseOption(searchParams.get('trendDays'), [7, 30, 90] as const, 30)
    return <Navigate to={`/food-diary-history?trendDays=${days}`} replace />
  }

  if (requestedTab === 'sensitivity') {
    const days = parseOption(searchParams.get('sensitivityDays'), [30, 90, 180] as const, 90)
    return <Navigate to={`/food-sensitivity?sensitivityDays=${days}`} replace />
  }

  return (
    <AppShell>
      <div className="w-full">
        <div className="mb-6 flex flex-col justify-between gap-4 lg:flex-row lg:items-end">
          <div>
            <Text type="secondary">Theo dõi sức khỏe</Text>
            <Title level={2} className="mb-1! mt-1!">Nhật ký ăn uống</Title>
            <Text type="secondary">
              Ghi lại bữa ăn và các triệu chứng đi kèm để theo dõi sức khỏe hằng ngày.
            </Text>
          </div>
          <JournalActions createLabel="Thêm bữa ăn" onCreate={openCreate} />
        </div>

        {actionError && (
          <Alert
            type="error"
            message={actionError}
            showIcon
            closable
            className="mb-4"
            onClose={() => setActionError(null)}
          />
        )}

        <FoodDiaryJournalTab
          view={journalView}
          date={date}
          keyword={keyword}
          keywordDays={keywordDays}
          reloadVersion={reloadVersion}
          onViewChange={(view) => updateSearch({ view: view === 'list' ? null : view })}
          onClearFilters={() => updateSearch({ date: null, keyword: null, keywordDays: null })}
          onKeywordChange={(nextKeyword) => updateSearch({
            date: null,
            keyword: nextKeyword || null,
            keywordDays: nextKeyword ? keywordDays : null,
          })}
          onKeywordDaysChange={(days) => updateSearch({ keywordDays: days })}
          onEdit={openEdit}
          onMutated={() => setReloadVersion((version) => version + 1)}
        />
      </div>

      <FoodDiaryEntryModal
        open={modalOpen}
        entry={editingEntry}
        saving={saving}
        onCancel={() => {
          setModalOpen(false)
          setEditingEntry(null)
        }}
        onSubmit={handleSubmit}
      />
    </AppShell>
  )
}
