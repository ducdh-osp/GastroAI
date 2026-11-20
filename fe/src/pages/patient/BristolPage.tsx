import { ExperimentOutlined, LineChartOutlined, PlusOutlined } from '@ant-design/icons'
import { Alert, Button, Tabs, Typography } from 'antd'
import { useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { createBristolLog, updateBristolLog } from '../../api/bristol'
import type { BristolLog, BristolLogRequest } from '../../api/bristol'
import { BristolJournalTab } from '../../components/bristol/BristolJournalTab'
import { BristolLogModal } from '../../components/bristol/BristolLogModal'
import { BristolTrendTab } from '../../components/bristol/BristolTrendTab'
import { HealthReportButton } from '../../components/healthReport/HealthReportButton'
import { AppShell } from '../../components/layout/AppShell'

const { Title, Text } = Typography
type MainTab = 'journal' | 'trend'

function parseTrendDays(value: string | null): 7 | 30 | 90 {
  const parsed = Number(value)
  return parsed === 7 || parsed === 90 ? parsed : 30
}

function vietnamDateKey(iso: string): string {
  const parts = new Intl.DateTimeFormat('en-CA', {
    timeZone: 'Asia/Ho_Chi_Minh',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).formatToParts(new Date(iso))
  const get = (type: Intl.DateTimeFormatPartTypes) => parts.find((part) => part.type === type)?.value ?? ''
  return `${get('year')}-${get('month')}-${get('day')}`
}

export default function BristolPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const activeTab: MainTab = searchParams.get('tab') === 'trend' ? 'trend' : 'journal'
  const journalView = searchParams.get('view') === 'trash' ? 'trash' : 'list'
  const date = searchParams.get('date') || undefined
  const trendDays = parseTrendDays(searchParams.get('trendDays'))

  const [modalOpen, setModalOpen] = useState(false)
  const [editingLog, setEditingLog] = useState<BristolLog | null>(null)
  const [saving, setSaving] = useState(false)
  const [actionError, setActionError] = useState<string | null>(null)
  const [reloadVersion, setReloadVersion] = useState(0)

  function updateSearch(changes: Record<string, string | number | null>) {
    setSearchParams((current) => {
      const next = new URLSearchParams(current)
      Object.entries(changes).forEach(([key, value]) => {
        if (value === null) next.delete(key)
        else next.set(key, String(value))
      })
      return next
    }, { replace: true })
  }

  function openCreate() {
    setActionError(null)
    setEditingLog(null)
    setModalOpen(true)
  }

  function openEdit(log: BristolLog) {
    setActionError(null)
    setEditingLog(log)
    setModalOpen(true)
  }

  async function handleSubmit(payload: BristolLogRequest) {
    const isEditing = editingLog !== null
    setSaving(true)
    setActionError(null)
    try {
      if (editingLog) await updateBristolLog(editingLog.id, payload)
      else await createBristolLog(payload)

      setModalOpen(false)
      setEditingLog(null)
      setReloadVersion((version) => version + 1)
      updateSearch({
        tab: null,
        view: null,
        ...(isEditing ? {} : { date: null }),
      })
    } catch (err) {
      setActionError(err instanceof Error ? err.message : 'Không thể lưu ghi nhận Bristol.')
    } finally {
      setSaving(false)
    }
  }

  function showJournalDay(loggedAt: string) {
    updateSearch({ tab: null, view: null, date: vietnamDateKey(loggedAt) })
  }

  return (
    <AppShell>
      <div className="mx-auto max-w-5xl">
        <div className="mb-6 flex flex-col justify-between gap-4 sm:flex-row sm:items-end">
          <div>
            <Text type="secondary">Theo dõi sức khỏe</Text>
            <Title level={2} className="mb-1! mt-1!">Nhật ký Bristol</Title>
            <Text type="secondary">
              Ghi nhận tình trạng tiêu hóa và theo dõi diễn biến theo thang Bristol tại một nơi.
            </Text>
          </div>
          <div className="flex flex-wrap gap-2">
            <HealthReportButton />
            <Button type="primary" size="large" icon={<PlusOutlined />} onClick={openCreate}>
              Ghi nhận mới
            </Button>
          </div>
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
          onChange={(key) => updateSearch({ tab: key === 'journal' ? null : key })}
          items={[
            {
              key: 'journal',
              label: <span><ExperimentOutlined /> Nhật ký</span>,
              children: (
                <BristolJournalTab
                  view={journalView}
                  date={date}
                  reloadVersion={reloadVersion}
                  onViewChange={(view) => updateSearch({ view: view === 'list' ? null : view })}
                  onClearFilter={() => updateSearch({ date: null })}
                  onEdit={openEdit}
                  onMutated={() => setReloadVersion((version) => version + 1)}
                />
              ),
            },
            {
              key: 'trend',
              label: <span><LineChartOutlined /> Xu hướng</span>,
              children: (
                <BristolTrendTab
                  days={trendDays}
                  onDaysChange={(days) => updateSearch({ trendDays: days })}
                  onPointSelect={showJournalDay}
                />
              ),
            },
          ]}
        />
      </div>

      <BristolLogModal
        open={modalOpen}
        log={editingLog}
        saving={saving}
        onCancel={() => {
          setModalOpen(false)
          setEditingLog(null)
        }}
        onSubmit={handleSubmit}
      />
    </AppShell>
  )
}
