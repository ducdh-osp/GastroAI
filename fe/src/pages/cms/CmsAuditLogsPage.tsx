import { AuditOutlined, EyeOutlined, ReloadOutlined } from '@ant-design/icons'
import {
    Alert,
    Button,
    Card,
    DatePicker,
    Descriptions,
    Drawer,
    Input,
    Select,
    Space,
    Table,
    Tabs,
    Tag,
    Tooltip,
    Typography,
} from 'antd'
import type { ColumnsType } from 'antd/es/table'
import type { Dayjs } from 'dayjs'
import { useEffect, useState } from 'react'
import {
    listAuditLogs,
    type AuditAction,
    type AuditLog,
    type AuditLogFilter,
    type AuditTargetType,
} from '../../api/cmsAuditLogs'
import { CmsAppShell } from '../../components/cms/CmsAppShell'
import { AuditAnomalyRulesTab } from '../../components/cms/AuditAnomalyRulesTab'
import { formatDateTime } from '../../lib/format'

const { Title, Text } = Typography
const { RangePicker } = DatePicker
const PAGE_SIZE = 20

const ACTION_LABEL: Record<AuditAction, string> = {
    DOCUMENT_UPLOAD: 'Tải lên tài liệu',
    DOCUMENT_NEW_VERSION: 'Tải phiên bản mới',
    DOCUMENT_ACTIVATE_VERSION: 'Kích hoạt phiên bản',
    DOCUMENT_UPDATE_INFO: 'Đổi thông tin tài liệu',
    DOCUMENT_PUBLISH: 'Đăng tài liệu',
    DOCUMENT_UNPUBLISH: 'Gỡ tài liệu',
    DOCUMENT_DELETE_VERSION: 'Xoá phiên bản',
    DOCUMENT_DELETE: 'Xoá tài liệu',
    RBAC_ROLE_CHANGE: 'Đổi vai trò',
    PATIENT_LOCK: 'Khoá bệnh nhân',
    PATIENT_UNLOCK: 'Mở khoá bệnh nhân',
    PATIENT_DATA_VIEW: 'Xem dữ liệu bệnh nhân',
    PASSWORD_CHANGE: 'Đổi mật khẩu',
    SYSTEM_RESTORE: 'Khôi phục dữ liệu',
    ANOMALY_DETECTED: 'Cảnh báo bất thường',
    ANOMALY_RULE_UPDATE: 'Sửa quy tắc cảnh báo',
}

const ACTION_COLOR: Partial<Record<AuditAction, string>> = {
    DOCUMENT_DELETE: 'red',
    DOCUMENT_DELETE_VERSION: 'red',
    PATIENT_LOCK: 'orange',
    PATIENT_UNLOCK: 'green',
    PATIENT_DATA_VIEW: 'blue',
    RBAC_ROLE_CHANGE: 'purple',
    PASSWORD_CHANGE: 'gold',
    SYSTEM_RESTORE: 'magenta',
    ANOMALY_DETECTED: 'volcano',
    ANOMALY_RULE_UPDATE: 'cyan',
}

const TARGET_LABEL: Record<AuditTargetType, string> = {
    DOCUMENT: 'Tài liệu',
    PATIENT: 'Bệnh nhân',
    ADMIN: 'Tài khoản Admin',
    DOCTOR: 'Tài khoản bác sĩ',
    SYSTEM: 'Hệ thống',
    RULE: 'Quy tắc cảnh báo',
}

const ACTOR_LABEL: Record<string, string> = {
    ADMIN: 'Admin',
    DOCTOR: 'Bác sĩ',
    SYSTEM: 'Hệ thống',
}

const actionOptions = (Object.keys(ACTION_LABEL) as AuditAction[]).map((value) => ({
    value,
    label: ACTION_LABEL[value],
}))

const targetOptions = (Object.keys(TARGET_LABEL) as AuditTargetType[]).map((value) => ({
    value,
    label: TARGET_LABEL[value],
}))

function targetText(log: AuditLog) {
    if (!log.targetType) return '—'
    const label = TARGET_LABEL[log.targetType as AuditTargetType] ?? log.targetType
    return log.targetId ? `${label} #${log.targetId}` : label
}

export default function CmsAuditLogsPage() {
    const [data, setData] = useState<AuditLog[]>([])
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState<string | null>(null)
    const [page, setPage] = useState(1)
    const [total, setTotal] = useState(0)
    const [range, setRange] = useState<[Dayjs, Dayjs] | null>(null)
    const [keyword, setKeyword] = useState('')
    const [filter, setFilter] = useState<AuditLogFilter>({})
    const [selected, setSelected] = useState<AuditLog | null>(null)

    useEffect(() => {
        let cancelled = false
        async function load() {
            setLoading(true)
            setError(null)
            try {
                const result = await listAuditLogs({ ...filter, page: page - 1, size: PAGE_SIZE })
                if (cancelled) return
                setData(result.items)
                setTotal(result.totalElements)
            } catch (err) {
                if (cancelled) return
                setError(err instanceof Error ? err.message : 'Không thể tải nhật ký hoạt động.')
            } finally {
                if (!cancelled) setLoading(false)
            }
        }
        void load()
        return () => { cancelled = true }
    }, [filter, page])

    function applyFilter(patch: Partial<AuditLogFilter>) {
        setFilter((current) => ({ ...current, ...patch }))
        setPage(1)
    }

    function changeRange(values: [Dayjs, Dayjs] | null) {
        setRange(values)
        applyFilter({
            from: values ? values[0].format('YYYY-MM-DD') : undefined,
            to: values ? values[1].format('YYYY-MM-DD') : undefined,
        })
    }

    function resetFilter() {
        setRange(null)
        setKeyword('')
        setFilter({})
        setPage(1)
    }

    const columns: ColumnsType<AuditLog> = [
        {
            title: 'Thời gian',
            dataIndex: 'occurredAt',
            key: 'occurredAt',
            width: 170,
            render: formatDateTime,
        },
        {
            title: 'Người thực hiện',
            key: 'actor',
            width: 240,
            render: (_, log) => (
                <div>
                    <div className="font-medium">{log.actorEmail ?? '—'}</div>
                    <Text type="secondary" className="text-xs">{ACTOR_LABEL[log.actorType] ?? log.actorType}</Text>
                </div>
            ),
        },
        {
            title: 'Hành động',
            dataIndex: 'action',
            key: 'action',
            width: 190,
            render: (action: AuditAction) => (
                <Tag color={ACTION_COLOR[action]}>{ACTION_LABEL[action] ?? action}</Tag>
            ),
        },
        {
            title: 'Đối tượng',
            key: 'target',
            width: 170,
            render: (_, log) => targetText(log),
        },
        {
            title: 'Chi tiết',
            dataIndex: 'detail',
            key: 'detail',
            ellipsis: { showTitle: false },
            render: (detail: string | null) => detail
                ? <Tooltip title={detail} placement="topLeft">{detail}</Tooltip>
                : '—',
        },
        {
            title: 'IP',
            dataIndex: 'ipAddress',
            key: 'ipAddress',
            width: 140,
            render: (ip: string, log) => (
                <Tooltip title={log.userAgent} placement="topLeft">
                    <span>{ip}</span>
                </Tooltip>
            ),
        },
        {
            title: '',
            key: 'view',
            width: 60,
            fixed: 'right',
            render: (_, log) => (
                <Tooltip title="Xem chi tiết">
                    <Button
                        type="text"
                        icon={<EyeOutlined />}
                        onClick={(event) => {
                            event.stopPropagation()
                            setSelected(log)
                        }}
                    />
                </Tooltip>
            ),
        },
    ]

    return <CmsAppShell><div className="mx-auto max-w-7xl">
        <div className="mb-6">
            <Text type="secondary">Hệ thống</Text>
            <Title level={2} className="mb-1! mt-1!"><AuditOutlined className="mr-2" />Nhật ký hoạt động</Title>
        </div>

        {error && <Alert type="error" title={error} showIcon className="mb-4" closable onClose={() => setError(null)} />}

        <Card className="rounded-2xl border-black/5 shadow-sm">
            <Tabs
                items={[
                    {
                        key: 'logs',
                        label: 'Nhật ký',
                        children: (
                            <>
                                <Space className="mb-4" wrap>
                                    <RangePicker
                                        value={range}
                                        onChange={(values) => changeRange(values as [Dayjs, Dayjs] | null)}
                                        format="DD/MM/YYYY"
                                        placeholder={['Từ ngày', 'Đến ngày']}
                                    />
                                    <Select
                                        allowClear
                                        placeholder="Tất cả hành động"
                                        value={filter.action}
                                        onChange={(value?: AuditAction) => applyFilter({ action: value })}
                                        options={actionOptions}
                                        style={{ width: 210 }}
                                    />
                                    <Select
                                        allowClear
                                        placeholder="Tất cả đối tượng"
                                        value={filter.targetType}
                                        onChange={(value?: AuditTargetType) => applyFilter({ targetType: value })}
                                        options={targetOptions}
                                        style={{ width: 180 }}
                                    />
                                    <Input.Search
                                        allowClear
                                        placeholder="Email, chi tiết hoặc mã đối tượng"
                                        value={keyword}
                                        onChange={(e) => setKeyword(e.target.value)}
                                        onSearch={(value) => applyFilter({ q: value.trim() || undefined })}
                                        style={{ width: 280 }}
                                    />
                                    <Button icon={<ReloadOutlined />} onClick={resetFilter}>Đặt lại</Button>
                                </Space>

                                <Table<AuditLog>
                                    rowKey="id"
                                    loading={loading}
                                    columns={columns}
                                    dataSource={data}
                                    scroll={{ x: 1100 }}
                                    onRow={(log) => ({ onClick: () => setSelected(log), className: 'cursor-pointer' })}
                                    pagination={{
                                        current: page,
                                        pageSize: PAGE_SIZE,
                                        total,
                                        showSizeChanger: false,
                                        showTotal: (count) => `${count} bản ghi`,
                                        onChange: (next) => setPage(next),
                                    }}
                                />
                            </>
                        ),
                    },
                    {
                        key: 'rules',
                        label: 'Quy tắc cảnh báo',
                        children: <AuditAnomalyRulesTab actionLabel={ACTION_LABEL} />,
                    },
                ]}
            />
        </Card>

        <Drawer
            title="Chi tiết hoạt động"
            open={selected !== null}
            onClose={() => setSelected(null)}
            size="large"
            destroyOnHidden
        >
            {selected && (
                <Descriptions column={1} bordered size="small" styles={{ label: { width: 150, whiteSpace: 'nowrap' } }}>
                    <Descriptions.Item label="Mã log">#{selected.id}</Descriptions.Item>
                    <Descriptions.Item label="Thời gian">{formatDateTime(selected.occurredAt)}</Descriptions.Item>
                    <Descriptions.Item label="Người thực hiện">
                        <Space wrap size={4}>
                            <span>{selected.actorEmail ?? '—'}</span>
                            <Tag>{ACTOR_LABEL[selected.actorType] ?? selected.actorType}</Tag>
                            {selected.actorId !== null && <Text type="secondary">#{selected.actorId}</Text>}
                        </Space>
                    </Descriptions.Item>
                    <Descriptions.Item label="Hành động">
                        <Space wrap size={4}>
                            <Tag color={ACTION_COLOR[selected.action]}>{ACTION_LABEL[selected.action] ?? selected.action}</Tag>
                            <Text type="secondary" className="text-xs">{selected.action}</Text>
                        </Space>
                    </Descriptions.Item>
                    <Descriptions.Item label="Đối tượng">{targetText(selected)}</Descriptions.Item>
                    <Descriptions.Item label="Chi tiết">
                        <div className="whitespace-pre-wrap break-words">{selected.detail ?? '—'}</div>
                    </Descriptions.Item>
                    <Descriptions.Item label="Địa chỉ IP">{selected.ipAddress}</Descriptions.Item>
                    <Descriptions.Item label="Thiết bị">
                        <div className="break-words text-xs">{selected.userAgent}</div>
                    </Descriptions.Item>
                </Descriptions>
            )}
        </Drawer>
    </div></CmsAppShell>
}