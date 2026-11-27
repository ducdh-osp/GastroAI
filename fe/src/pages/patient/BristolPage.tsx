import { Alert, Typography } from 'antd'
import { useState } from 'react'
import { Navigate, useSearchParams } from 'react-router-dom'
import { createBristolLog, updateBristolLog } from '../../api/bristol'
import type { BristolLog, BristolLogRequest } from '../../api/bristol'
import { BristolJournalTab } from '../../components/bristol/BristolJournalTab'
import { BristolLogModal } from '../../components/bristol/BristolLogModal'
import { JournalActions } from '../../components/healthReport/JournalActions'
import { AppShell } from '../../components/layout/AppShell'

const { Title, Text } = Typography
function parseTrendDays(value: string | null): 7 | 30 | 90 {
  const parsed = Number(value)
  return parsed === 7 || parsed === 90 ? parsed : 30
}

export default function BristolPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const requestedTab = searchParams.get('tab')
  const journalView = searchParams.get('view') === 'trash' ? 'trash' : 'list'
  const date = searchParams.get('date') || undefined

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

  if (requestedTab === 'trend') {
    return <Navigate to={`/bristol-history?trendDays=${parseTrendDays(searchParams.get('trendDays'))}`} replace />
  }

  return (
    <AppShell>
      <div className="w-full">
        <div className="mb-6 flex flex-col justify-between gap-4 lg:flex-row lg:items-end">
          <div>
            <Text type="secondary">Theo dõi sức khỏe</Text>
            <Title level={2} className="mb-1! mt-1!">Nhật ký Bristol</Title>
            <Text type="secondary">
              Ghi nhận tình trạng tiêu hóa hằng ngày theo thang Bristol.
            </Text>
          </div>
          <JournalActions createLabel="Ghi nhận mới" onCreate={openCreate} />
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

        <BristolJournalTab
          view={journalView}
          date={date}
          reloadVersion={reloadVersion}
          onViewChange={(view) => updateSearch({ view: view === 'list' ? null : view })}
          onClearFilter={() => updateSearch({ date: null })}
          onEdit={openEdit}
          onMutated={() => setReloadVersion((version) => version + 1)}
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
