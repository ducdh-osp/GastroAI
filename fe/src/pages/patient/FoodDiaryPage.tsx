import { BarChartOutlined, BulbOutlined, CoffeeOutlined, PlusOutlined } from '@ant-design/icons'
import { Alert, Button, Tabs, Typography } from 'antd'
import { useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { createFoodDiaryEntry, updateFoodDiaryEntry } from '../../api/foodDiary'
import type { FoodDiaryEntry, FoodDiaryEntryRequest } from '../../api/foodDiary'
import { FoodDiaryEntryModal } from '../../components/foodDiary/FoodDiaryEntryModal'
import { FoodDiaryJournalTab } from '../../components/foodDiary/FoodDiaryJournalTab'
import { FoodDiaryTrendTab } from '../../components/foodDiary/FoodDiaryTrendTab'
import { FoodSensitivityTab } from '../../components/foodDiary/FoodSensitivityTab'
import { AppShell } from '../../components/layout/AppShell'

const { Title, Text } = Typography
type MainTab = 'journal' | 'trend' | 'sensitivity'

function parseOption<T extends number>(value: string | null, options: readonly T[], fallback: T): T {
  const parsed = Number(value)
  return options.includes(parsed as T) ? parsed as T : fallback
}

export default function FoodDiaryPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const requestedTab = searchParams.get('tab')
  const activeTab: MainTab = requestedTab === 'trend' || requestedTab === 'sensitivity' ? requestedTab : 'journal'
  const journalView = searchParams.get('view') === 'trash' ? 'trash' : 'list'
  const date = searchParams.get('date') || undefined
  const keyword = searchParams.get('keyword') || undefined
  const keywordDays = parseOption(searchParams.get('keywordDays'), [30, 90, 180] as const, 90)
  const trendDays = parseOption(searchParams.get('trendDays'), [7, 30, 90] as const, 30)
  const sensitivityDays = parseOption(searchParams.get('sensitivityDays'), [30, 90, 180] as const, 90)

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

  function selectTab(tab: MainTab) {
    updateSearch({ tab: tab === 'journal' ? null : tab })
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

  function showJournalDate(selectedDate: string) {
    updateSearch({
      tab: null,
      view: null,
      date: selectedDate,
      keyword: null,
      keywordDays: null,
    })
  }

  function showSensitivityEvidence(selectedKeyword: string, days: number) {
    updateSearch({
      tab: null,
      view: null,
      date: null,
      keyword: selectedKeyword,
      keywordDays: days,
    })
  }

  return (
    <AppShell>
      <div className="mx-auto max-w-5xl">
        <div className="mb-6 flex flex-col justify-between gap-4 sm:flex-row sm:items-end">
          <div>
            <Text type="secondary">Theo dõi sức khỏe</Text>
            <Title level={2} className="mb-1! mt-1!">Nhật ký ăn uống</Title>
            <Text type="secondary">
              Ghi lại bữa ăn, theo dõi xu hướng và đối chiếu thực phẩm với triệu chứng tại một nơi.
            </Text>
          </div>
          <Button type="primary" size="large" icon={<PlusOutlined />} onClick={openCreate}>
            Ghi bữa ăn
          </Button>
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

        <Tabs
          activeKey={activeTab}
          destroyOnHidden
          size="large"
          onChange={(key) => selectTab(key as MainTab)}
          items={[
            {
              key: 'journal',
              label: <span><CoffeeOutlined /> Nhật ký</span>,
              children: (
                <FoodDiaryJournalTab
                  view={journalView}
                  date={date}
                  keyword={keyword}
                  keywordDays={keywordDays}
                  reloadVersion={reloadVersion}
                  onViewChange={(view) => updateSearch({ view: view === 'list' ? null : view })}
                  onClearFilters={() => updateSearch({ date: null, keyword: null, keywordDays: null })}
                  onEdit={openEdit}
                  onMutated={() => setReloadVersion((version) => version + 1)}
                />
              ),
            },
            {
              key: 'trend',
              label: <span><BarChartOutlined /> Xu hướng</span>,
              children: (
                <FoodDiaryTrendTab
                  days={trendDays}
                  onDaysChange={(days) => updateSearch({ trendDays: days })}
                  onDaySelect={showJournalDate}
                />
              ),
            },
            {
              key: 'sensitivity',
              label: <span><BulbOutlined /> Nhạy cảm thực phẩm</span>,
              children: (
                <FoodSensitivityTab
                  days={sensitivityDays}
                  onDaysChange={(days) => updateSearch({ sensitivityDays: days })}
                  onViewEvidence={showSensitivityEvidence}
                />
              ),
            },
          ]}
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
