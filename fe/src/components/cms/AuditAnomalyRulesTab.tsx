import { SaveOutlined, UndoOutlined } from '@ant-design/icons'
import { Alert, Button, InputNumber, Space, Switch, Table, Tag, Tooltip, Typography, message } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { useEffect, useState } from 'react'
import {
  listAnomalyRules,
  updateAnomalyRule,
  type AnomalyMetric,
  type AuditAction,
  type AuditAnomalyRule,
  type UpdateAuditAnomalyRulePayload,
} from '../../api/cmsAuditLogs'
import { formatDateTime } from '../../lib/format'

const { Text } = Typography

const METRIC_LABEL: Record<AnomalyMetric, string> = {
  ACTIONS: 'Đếm số lượt thao tác',
  DISTINCT_TARGETS: 'Đếm số đối tượng khác nhau',
  DISTINCT_IPS: 'Đếm số địa chỉ IP khác nhau',
}

interface AuditAnomalyRulesTabProps {
  actionLabel: Record<AuditAction, string>
}

function toDraft(rule: AuditAnomalyRule): UpdateAuditAnomalyRulePayload {
  return { threshold: rule.threshold, windowMinutes: rule.windowMinutes, enabled: rule.enabled }
}

function isChanged(rule: AuditAnomalyRule, draft: UpdateAuditAnomalyRulePayload) {
  return draft.threshold !== rule.threshold
    || draft.windowMinutes !== rule.windowMinutes
    || draft.enabled !== rule.enabled
}

export function AuditAnomalyRulesTab({ actionLabel }: AuditAnomalyRulesTabProps) {
  const [rules, setRules] = useState<AuditAnomalyRule[]>([])
  const [drafts, setDrafts] = useState<Record<string, UpdateAuditAnomalyRulePayload>>({})
  const [loading, setLoading] = useState(true)
  const [savingCode, setSavingCode] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    async function load() {
      setLoading(true)
      setError(null)
      try {
        const result = await listAnomalyRules()
        if (cancelled) return
        setRules(result)
        setDrafts(Object.fromEntries(result.map((rule) => [rule.code, toDraft(rule)])))
      } catch (err) {
        if (cancelled) return
        setError(err instanceof Error ? err.message : 'Không thể tải danh sách quy tắc cảnh báo.')
      } finally {
        if (!cancelled) setLoading(false)
      }
    }
    void load()
    return () => { cancelled = true }
  }, [])

  function changeDraft(code: string, patch: Partial<UpdateAuditAnomalyRulePayload>) {
    setDrafts((current) => ({ ...current, [code]: { ...current[code], ...patch } }))
  }

  function resetDraft(rule: AuditAnomalyRule) {
    setDrafts((current) => ({ ...current, [rule.code]: toDraft(rule) }))
  }

  async function save(rule: AuditAnomalyRule) {
    const draft = drafts[rule.code]
    setSavingCode(rule.code)
    setError(null)
    try {
      const updated = await updateAnomalyRule(rule.code, draft)
      setRules((current) => current.map((item) => item.code === updated.code ? updated : item))
      setDrafts((current) => ({ ...current, [updated.code]: toDraft(updated) }))
      message.success(`Đã lưu quy tắc "${updated.name}"`)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không thể lưu quy tắc cảnh báo.')
    } finally {
      setSavingCode(null)
    }
  }

  const columns: ColumnsType<AuditAnomalyRule> = [
    {
      title: 'Quy tắc',
      key: 'rule',
      render: (_, rule) => (
        <div>
          <div className="font-medium">{rule.name}</div>
          <Text type="secondary" className="text-xs">{rule.code}</Text>
        </div>
      ),
    },
    {
      title: 'Theo dõi',
      key: 'actions',
      render: (_, rule) => (
        <div>
          {rule.actions.length === 0
            ? <Tag>Mọi thao tác</Tag>
            : (
              <Space wrap size={4}>
                {rule.actions.map((action) => <Tag key={action}>{actionLabel[action] ?? action}</Tag>)}
              </Space>
            )}
          <div><Text type="secondary" className="text-xs">{METRIC_LABEL[rule.metric] ?? rule.metric}</Text></div>
        </div>
      ),
    },
    {
      title: 'Ngưỡng cảnh báo',
      key: 'threshold',
      render: (_, rule) => (
        <Space size={6} align="center" wrap>
          <InputNumber
            min={1}
            max={1000}
            precision={0}
            value={drafts[rule.code]?.threshold}
            onChange={(value) => changeDraft(rule.code, { threshold: value ?? 1 })}
            style={{ width: 70 }}
          />
          <Text type="secondary">/</Text>
          <InputNumber
            min={1}
            max={1440}
            precision={0}
            value={drafts[rule.code]?.windowMinutes}
            onChange={(value) => changeDraft(rule.code, { windowMinutes: value ?? 1 })}
            style={{ width: 70 }}
          />
          <Text type="secondary">phút</Text>
        </Space>
      ),
    },
    {
      title: 'Bật',
      key: 'enabled',
      width: 70,
      render: (_, rule) => (
        <Switch
          checked={drafts[rule.code]?.enabled}
          onChange={(checked) => changeDraft(rule.code, { enabled: checked })}
        />
      ),
    },
    {
      title: 'Sửa lần cuối',
      key: 'updated',
      render: (_, rule) => (
        <div className="max-w-[180px]">
          <div className="truncate">{rule.updatedBy ?? 'Mặc định'}</div>
          <Text type="secondary" className="text-xs">{formatDateTime(rule.updatedAt)}</Text>
        </div>
      ),
    },
    {
      title: '',
      key: 'save',
      width: 80,
      render: (_, rule) => {
        const draft = drafts[rule.code]
        const changed = draft ? isChanged(rule, draft) : false
        return (
          <Space size={4}>
            <Tooltip title="Lưu">
              <Button
                type="primary"
                size="small"
                icon={<SaveOutlined />}
                disabled={!changed}
                loading={savingCode === rule.code}
                onClick={() => void save(rule)}
              />
            </Tooltip>
            <Tooltip title="Hoàn tác">
              <Button
                size="small"
                icon={<UndoOutlined />}
                disabled={!changed || savingCode === rule.code}
                onClick={() => resetDraft(rule)}
              />
            </Tooltip>
          </Space>
        )
      },
    },
  ]

  return (
    <>
      {error && <Alert type="error" title={error} showIcon className="mb-4" closable onClose={() => setError(null)} />}
      <Table<AuditAnomalyRule>
        rowKey="code"
        loading={loading}
        columns={columns}
        dataSource={rules}
        pagination={false}
      />
    </>
  )
}