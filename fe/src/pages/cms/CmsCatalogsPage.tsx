import {
  AppstoreOutlined,
  CheckCircleOutlined,
  DeleteOutlined,
  EditOutlined,
  ExperimentOutlined,
  FilterOutlined,
  MedicineBoxOutlined,
  PlusOutlined,
  ReloadOutlined,
  SearchOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons'
import {
  Alert,
  AutoComplete,
  Badge,
  Button,
  Card,
  Form,
  Input,
  InputNumber,
  Modal,
  notification,
  Popconfirm,
  Radio,
  Select,
  Space,
  Switch,
  Table,
  Tabs,
  Tag,
  Tooltip,
  Typography,
} from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { useCallback, useEffect, useMemo, useState } from 'react'
import {
  createCmsCatalog,
  deleteCmsCatalog,
  listCmsCatalogs,
  toggleCmsCatalogActive,
  updateCmsCatalog,
  type CatalogType,
  type CmsCatalogItem,
  type CmsCatalogItemRequest,
} from '../../api/cmsCatalogs'
import { CmsAppShell } from '../../components/cms/CmsAppShell'
import { useInFlightGuard } from '../../hooks/useInFlightGuard'

const { Title, Text } = Typography

// Gợi ý các mã Triage cấp cứu thường dùng để khớp từ khóa tầng 1 Triage
const TRIAGE_SUGGESTIONS = [
  { value: 'BLACK_OR_BLOODY_STOOL', label: 'BLACK_OR_BLOODY_STOOL - Phân đen/xuất huyết tiêu hóa dưới' },
  { value: 'BLOOD_IN_VOMIT', label: 'BLOOD_IN_VOMIT - Nôn ra máu/chất như bã cà phê' },
  { value: 'ABDOMINAL_PAIN', label: 'ABDOMINAL_PAIN - Đau bụng cấp tính dữ dội' },
  { value: 'BLOATING', label: 'BLOATING - Đầy hơi chướng bụng nặng' },
  { value: 'CONSTIPATION', label: 'CONSTIPATION - Táo bón kéo dài/bí đại tiện' },
  { value: 'DIARRHEA', label: 'DIARRHEA - Tiêu chảy cấp tính mất nước' },
  { value: 'HEARTBURN', label: 'HEARTBURN - Ợ nóng, trào ngược nghiêm trọng' },
  { value: 'NAUSEA', label: 'NAUSEA - Buồn nôn kéo dài' },
  { value: 'VOMITING', label: 'VOMITING - Nôn mửa liên tục' },
]

const TYPE_CONFIG: Record<
  CatalogType,
  { label: string; color: string; badgeColor: string; icon: React.ReactNode }
> = {
  SYMPTOM: {
    label: 'Triệu chứng',
    color: 'cyan',
    badgeColor: '#13c2c2',
    icon: <ExperimentOutlined />,
  },
  CONDITION: {
    label: 'Bệnh lý',
    color: 'orange',
    badgeColor: '#fa8c16',
    icon: <ThunderboltOutlined />,
  },
  MEDICATION: {
    label: 'Thuốc',
    color: 'green',
    badgeColor: '#52c41a',
    icon: <MedicineBoxOutlined />,
  },
}

export default function CmsCatalogsPage() {
  const [items, setItems] = useState<CmsCatalogItem[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  // Filters
  const [selectedTypeTab, setSelectedTypeTab] = useState<string>('ALL')
  const [searchText, setSearchText] = useState('')
  const [activeFilter, setActiveFilter] = useState<'ALL' | 'ACTIVE' | 'INACTIVE'>('ALL')

  // Modal create/edit
  const [modalOpen, setModalOpen] = useState(false)
  const [editingItem, setEditingItem] = useState<CmsCatalogItem | null>(null)
  const [modalSubmitting, setModalSubmitting] = useState(false)
  const [modalError, setModalError] = useState<string | null>(null)
  const [form] = Form.useForm<CmsCatalogItemRequest>()

  const deleteGuard = useInFlightGuard<number>()
  const toggleGuard = useInFlightGuard<number>()

  const loadData = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const data = await listCmsCatalogs()
      setItems(data)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không thể tải danh sách danh mục.')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    loadData()
  }, [loadData])

  // Statistics calculation
  const stats = useMemo(() => {
    const total = items.length
    const symptoms = items.filter((i) => i.type === 'SYMPTOM').length
    const conditions = items.filter((i) => i.type === 'CONDITION').length
    const medications = items.filter((i) => i.type === 'MEDICATION').length
    const activeCount = items.filter((i) => i.active).length
    return { total, symptoms, conditions, medications, activeCount }
  }, [items])

  // Filtered items
  const filteredItems = useMemo(() => {
    return items.filter((item) => {
      // Type tab filter
      if (selectedTypeTab !== 'ALL' && item.type !== selectedTypeTab) {
        return false
      }
      // Active filter
      if (activeFilter === 'ACTIVE' && !item.active) return false
      if (activeFilter === 'INACTIVE' && item.active) return false

      // Search text
      if (searchText.trim()) {
        const q = searchText.trim().toLowerCase()
        const matchName = item.name.toLowerCase().includes(q)
        const matchCode = item.code.toLowerCase().includes(q)
        const matchTriage = item.triageCode ? item.triageCode.toLowerCase().includes(q) : false
        if (!matchName && !matchCode && !matchTriage) {
          return false
        }
      }

      return true
    })
  }, [items, selectedTypeTab, activeFilter, searchText])

  // Handlers for modal
  function handleOpenCreate() {
    setEditingItem(null)
    setModalError(null)
    const currentTabType = selectedTypeTab !== 'ALL' ? (selectedTypeTab as CatalogType) : 'SYMPTOM'
    // Default next sortOrder based on selected type
    const maxSort = items
      .filter((i) => i.type === currentTabType)
      .reduce((max, i) => Math.max(max, i.sortOrder || 0), 0)

    form.resetFields()
    form.setFieldsValue({
      type: currentTabType,
      code: '',
      name: '',
      triageCode: null,
      sortOrder: maxSort + 10,
      active: true,
    })
    setModalOpen(true)
  }

  function handleOpenEdit(record: CmsCatalogItem) {
    setEditingItem(record)
    setModalError(null)
    form.resetFields()
    form.setFieldsValue({
      type: record.type,
      code: record.code,
      name: record.name,
      triageCode: record.triageCode,
      sortOrder: record.sortOrder,
      active: record.active,
    })
    setModalOpen(true)
  }

  async function handleModalSubmit() {
    try {
      const values = await form.validateFields()
      setModalSubmitting(true)
      setModalError(null)

      const payload: CmsCatalogItemRequest = {
        ...values,
        code: values.code.trim().toUpperCase().replace(/\s+/g, '_'),
        name: values.name.trim(),
        triageCode: values.triageCode ? values.triageCode.trim().toUpperCase().replace(/\s+/g, '_') : null,
      }

      if (editingItem) {
        await updateCmsCatalog(editingItem.id, payload)
        notification.success({
          message: 'Cập nhật thành công',
          description: `Đã lưu thay đổi cho danh mục "${payload.name}".`,
        })
      } else {
        await createCmsCatalog(payload)
        notification.success({
          message: 'Thêm mới thành công',
          description: `Đã thêm danh mục "${payload.name}" (${payload.code}).`,
        })
      }

      setModalOpen(false)
      loadData()
    } catch (err) {
      if (err && typeof err === 'object' && 'errorFields' in err) {
        return // Ant Design form validation error
      }
      setModalError(err instanceof Error ? err.message : 'Đã có lỗi xảy ra khi lưu danh mục.')
    } finally {
      setModalSubmitting(false)
    }
  }

  async function handleToggle(record: CmsCatalogItem) {
    await toggleGuard.run(record.id, async () => {
      try {
        const updated = await toggleCmsCatalogActive(record.id)
        setItems((prev) => prev.map((item) => (item.id === updated.id ? updated : item)))
        notification.success({
          message: updated.active ? 'Đã kích hoạt' : 'Đã tắt kích hoạt',
          description: `Danh mục "${updated.name}" hiện ${updated.active ? 'sẽ xuất hiện' : 'sẽ ẩn'} trên hệ thống.`,
        })
      } catch (err) {
        notification.error({
          message: 'Thao tác thất bại',
          description: err instanceof Error ? err.message : 'Không thể thay đổi trạng thái danh mục.',
        })
      }
    })
  }

  async function handleDelete(record: CmsCatalogItem) {
    await deleteGuard.run(record.id, async () => {
      try {
        await deleteCmsCatalog(record.id)
        setItems((prev) => prev.filter((item) => item.id !== record.id))
        notification.success({
          message: 'Xóa thành công',
          description: `Đã xóa danh mục "${record.name}".`,
        })
      } catch (err) {
        notification.error({
          message: 'Xóa thất bại',
          description: err instanceof Error ? err.message : 'Không thể xóa danh mục này.',
        })
      }
    })
  }

  const columns: ColumnsType<CmsCatalogItem> = [
    {
      title: 'STT',
      key: 'stt',
      width: 60,
      align: 'center',
      render: (_val, _record, index) => <span className="text-gray-400 font-mono text-xs">{index + 1}</span>,
    },
    {
      title: 'Phân loại',
      dataIndex: 'type',
      key: 'type',
      width: 140,
      render: (type: CatalogType) => {
        const cfg = TYPE_CONFIG[type]
        return (
          <Tag color={cfg.color} className="inline-flex items-center gap-1 font-medium px-2 py-0.5">
            {cfg.icon}
            <span>{cfg.label}</span>
          </Tag>
        )
      },
    },
    {
      title: 'Tên hiển thị',
      dataIndex: 'name',
      key: 'name',
      width: 260,
      render: (name: string, record) => (
        <div className="flex flex-col">
          <span className="font-semibold text-slate-800 dark:text-slate-100">{name}</span>
          <span className="text-xs text-slate-400 font-mono mt-0.5">{record.code}</span>
        </div>
      ),
    },
    {
      title: 'Mã Triage liên kết',
      dataIndex: 'triageCode',
      key: 'triageCode',
      width: 200,
      render: (triageCode: string | null) => {
        if (!triageCode) {
          return <span className="text-slate-300 dark:text-slate-600 italic text-xs">Không có</span>
        }
        return (
          <Tooltip title="Mã nhóm khớp dấu hiệu cảnh báo khẩn cấp trong Triage tầng 1">
            <Tag color="volcano" className="font-mono text-xs font-semibold px-2">
              <ThunderboltOutlined className="mr-1" />
              {triageCode}
            </Tag>
          </Tooltip>
        )
      },
    },
    {
      title: 'Thứ tự',
      dataIndex: 'sortOrder',
      key: 'sortOrder',
      width: 90,
      align: 'center',
      sorter: (a, b) => a.sortOrder - b.sortOrder,
      render: (sortOrder: number) => (
        <span className="inline-block px-2 py-0.5 rounded text-xs font-mono font-medium bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300 border border-slate-200 dark:border-slate-700">
          {sortOrder}
        </span>
      ),
    },
    {
      title: 'Trạng thái',
      dataIndex: 'active',
      key: 'active',
      width: 120,
      align: 'center',
      render: (active: boolean, record) => (
        <div className="flex items-center justify-center">
          <Switch
            checked={active}
            size="small"
            checkedChildren="Bật"
            unCheckedChildren="Tắt"
            loading={toggleGuard.inFlightId === record.id}
            onChange={() => handleToggle(record)}
          />
        </div>
      ),
    },
    {
      title: 'Thao tác',
      key: 'actions',
      width: 110,
      align: 'center',
      render: (_val, record) => (
        <Space size="small">
          <Tooltip title="Chỉnh sửa">
            <Button
              size="small"
              type="text"
              icon={<EditOutlined className="text-blue-500" />}
              onClick={() => handleOpenEdit(record)}
            />
          </Tooltip>
          <Popconfirm
            title="Xác nhận xóa danh mục?"
            description={
              <div className="max-w-xs text-xs">
                Mục <strong className="font-semibold">{record.name}</strong> ({record.code}) sẽ bị xóa vĩnh viễn khỏi
                hệ thống. Nếu chỉ muốn tạm ẩn trong dropdown bệnh nhân, hãy chọn Tắt trạng thái.
              </div>
            }
            okText="Xóa vĩnh viễn"
            cancelText="Hủy"
            okButtonProps={{ danger: true, loading: deleteGuard.inFlightId === record.id }}
            onConfirm={() => handleDelete(record)}
          >
            <Tooltip title="Xóa">
              <Button
                size="small"
                type="text"
                danger
                icon={<DeleteOutlined />}
                loading={deleteGuard.inFlightId === record.id}
              />
            </Tooltip>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <CmsAppShell>
      <div className="mx-auto max-w-7xl px-4 py-6">
        {/* Header section */}
        <div className="mb-6 flex flex-wrap items-center justify-between gap-4">
          <div>
            <Title level={2} className="!mb-1 tracking-tight flex items-center gap-2">
              <MedicineBoxOutlined className="text-emerald-600" />
              Quản lý danh mục chuẩn hóa
            </Title>
            <Text type="secondary" className="text-sm">
              Chuẩn hóa danh mục triệu chứng, bệnh lý tiêu hóa và thuốc dùng làm nguồn dữ liệu dropdown cho hồ sơ bệnh
              án, nhật ký dùng thuốc và đối soát quy tắc cảnh báo Triage.
            </Text>
          </div>
          <Space>
            <Button icon={<ReloadOutlined />} onClick={loadData} loading={loading}>
              Làm mới
            </Button>
            <Button type="primary" icon={<PlusOutlined />} onClick={handleOpenCreate} className="bg-emerald-600 hover:bg-emerald-500">
              Thêm danh mục mới
            </Button>
          </Space>
        </div>

        {/* Global error banner */}
        {error && (
          <Alert
            message="Không thể kết nối đến máy chủ"
            description={error}
            type="error"
            showIcon
            closable
            className="mb-6"
          />
        )}

        {/* Metric / Stat cards */}
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
          <Card className="shadow-xs border-slate-200 dark:border-slate-800" styles={{ body: { padding: '16px' } }}>
            <div className="flex items-center justify-between">
              <div>
                <Text type="secondary" className="text-xs uppercase font-medium">
                  Tổng danh mục
                </Text>
                <div className="text-2xl font-bold text-slate-800 dark:text-slate-100 mt-1">{stats.total}</div>
                <div className="text-xs text-slate-400 mt-0.5">
                  <CheckCircleOutlined className="text-emerald-500 mr-1" />
                  {stats.activeCount} đang kích hoạt
                </div>
              </div>
              <div className="w-10 h-10 rounded-lg bg-blue-50 dark:bg-blue-950/40 text-blue-600 flex items-center justify-center text-lg">
                <AppstoreOutlined />
              </div>
            </div>
          </Card>

          <Card className="shadow-xs border-slate-200 dark:border-slate-800" styles={{ body: { padding: '16px' } }}>
            <div className="flex items-center justify-between">
              <div>
                <Text type="secondary" className="text-xs uppercase font-medium">
                  Triệu chứng tiêu hóa
                </Text>
                <div className="text-2xl font-bold text-cyan-600 dark:text-cyan-400 mt-1">{stats.symptoms}</div>
                <div className="text-xs text-slate-400 mt-0.5">Định danh SYMPTOM</div>
              </div>
              <div className="w-10 h-10 rounded-lg bg-cyan-50 dark:bg-cyan-950/40 text-cyan-600 flex items-center justify-center text-lg">
                <ExperimentOutlined />
              </div>
            </div>
          </Card>

          <Card className="shadow-xs border-slate-200 dark:border-slate-800" styles={{ body: { padding: '16px' } }}>
            <div className="flex items-center justify-between">
              <div>
                <Text type="secondary" className="text-xs uppercase font-medium">
                  Bệnh lý tiêu hóa
                </Text>
                <div className="text-2xl font-bold text-orange-600 dark:text-orange-400 mt-1">{stats.conditions}</div>
                <div className="text-xs text-slate-400 mt-0.5">Định danh CONDITION</div>
              </div>
              <div className="w-10 h-10 rounded-lg bg-orange-50 dark:bg-orange-950/40 text-orange-600 flex items-center justify-center text-lg">
                <ThunderboltOutlined />
              </div>
            </div>
          </Card>

          <Card className="shadow-xs border-slate-200 dark:border-slate-800" styles={{ body: { padding: '16px' } }}>
            <div className="flex items-center justify-between">
              <div>
                <Text type="secondary" className="text-xs uppercase font-medium">
                  Thuốc thường dùng
                </Text>
                <div className="text-2xl font-bold text-emerald-600 dark:text-emerald-400 mt-1">{stats.medications}</div>
                <div className="text-xs text-slate-400 mt-0.5">Định danh MEDICATION</div>
              </div>
              <div className="w-10 h-10 rounded-lg bg-emerald-50 dark:bg-emerald-950/40 text-emerald-600 flex items-center justify-center text-lg">
                <MedicineBoxOutlined />
              </div>
            </div>
          </Card>
        </div>

        {/* Filter and Tab Section */}
        <Card className="shadow-xs border-slate-200 dark:border-slate-800 mb-6" styles={{ body: { padding: '16px 20px' } }}>
          <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
            <Tabs
              activeKey={selectedTypeTab}
              onChange={setSelectedTypeTab}
              className="!mb-0"
              items={[
                {
                  key: 'ALL',
                  label: (
                    <span className="flex items-center gap-1.5 font-medium">
                      Tất cả <Badge count={stats.total} overflowCount={999} color="#64748b" />
                    </span>
                  ),
                },
                {
                  key: 'SYMPTOM',
                  label: (
                    <span className="flex items-center gap-1.5 font-medium">
                      <ExperimentOutlined /> Triệu chứng <Badge count={stats.symptoms} color="#13c2c2" />
                    </span>
                  ),
                },
                {
                  key: 'CONDITION',
                  label: (
                    <span className="flex items-center gap-1.5 font-medium">
                      <ThunderboltOutlined /> Bệnh lý <Badge count={stats.conditions} color="#fa8c16" />
                    </span>
                  ),
                },
                {
                  key: 'MEDICATION',
                  label: (
                    <span className="flex items-center gap-1.5 font-medium">
                      <MedicineBoxOutlined /> Thuốc <Badge count={stats.medications} color="#52c41a" />
                    </span>
                  ),
                },
              ]}
            />

            <div className="flex flex-wrap items-center gap-2.5">
              <Input
                placeholder="Tìm tên, mã chuẩn hóa, triage..."
                prefix={<SearchOutlined className="text-slate-400" />}
                value={searchText}
                onChange={(e) => setSearchText(e.target.value)}
                allowClear
                className="w-64"
              />
              <Select
                value={activeFilter}
                onChange={setActiveFilter}
                className="w-36"
                options={[
                  { value: 'ALL', label: 'Tất cả trạng thái' },
                  { value: 'ACTIVE', label: 'Đang hoạt động' },
                  { value: 'INACTIVE', label: 'Đang ẩn' },
                ]}
              />
            </div>
          </div>
        </Card>

        {/* Data Table */}
        <Card className="shadow-xs border-slate-200 dark:border-slate-800" styles={{ body: { padding: 0 } }}>
          <Table
            columns={columns}
            dataSource={filteredItems}
            rowKey="id"
            loading={loading}
            pagination={{
              pageSize: 15,
              showSizeChanger: true,
              pageSizeOptions: ['15', '30', '50', '100'],
              showTotal: (total, range) => `${range[0]}-${range[1]} của ${total} mục`,
            }}
            locale={{
              emptyText: (
                <div className="py-12 text-center text-slate-400">
                  <FilterOutlined className="text-3xl mb-2" />
                  <div>Không tìm thấy danh mục nào phù hợp</div>
                </div>
              ),
            }}
          />
        </Card>

        {/* Modal Create / Edit */}
        <Modal
          title={
            <div className="flex items-center gap-2 text-base font-semibold">
              <MedicineBoxOutlined className="text-emerald-500" />
              {editingItem ? 'Chỉnh sửa danh mục chuẩn hóa' : 'Thêm danh mục chuẩn hóa mới'}
            </div>
          }
          open={modalOpen}
          onOk={handleModalSubmit}
          confirmLoading={modalSubmitting}
          onCancel={() => setModalOpen(false)}
          okText={editingItem ? 'Lưu thay đổi' : 'Thêm mới'}
          cancelText="Hủy bỏ"
          destroyOnClose
          width={600}
        >
          {modalError && (
            <Alert message="Không thể lưu danh mục" description={modalError} type="error" showIcon className="mb-4" />
          )}

          <Form form={form} layout="vertical" className="mt-4" initialValues={{ sortOrder: 10, active: true }}>
            <Form.Item
              name="type"
              label="Loại danh mục chuẩn hóa"
              rules={[{ required: true, message: 'Vui lòng chọn loại danh mục' }]}
            >
              <Radio.Group buttonStyle="solid" className="w-full flex">
                <Radio.Button value="SYMPTOM" className="flex-1 text-center">
                  <ExperimentOutlined className="mr-1" /> Triệu chứng
                </Radio.Button>
                <Radio.Button value="CONDITION" className="flex-1 text-center">
                  <ThunderboltOutlined className="mr-1" /> Bệnh lý
                </Radio.Button>
                <Radio.Button value="MEDICATION" className="flex-1 text-center">
                  <MedicineBoxOutlined className="mr-1" /> Thuốc
                </Radio.Button>
              </Radio.Group>
            </Form.Item>

            <Form.Item
              name="name"
              label="Tên hiển thị tiếng Việt"
              rules={[
                { required: true, message: 'Vui lòng nhập tên hiển thị tiếng Việt' },
                { max: 200, message: 'Tên hiển thị tối đa 200 ký tự' },
              ]}
              extra="Tên hiển thị cho bệnh nhân và bác sĩ trong các dropdown chọn lựa."
            >
              <Input placeholder="vd: Đau thắt vùng thượng vị, Omeprazole, Loét dạ dày..." />
            </Form.Item>

            <Form.Item
              name="code"
              label="Mã chuẩn hóa (Code định danh)"
              rules={[
                { required: true, message: 'Vui lòng nhập mã chuẩn hóa' },
                { max: 80, message: 'Mã tối đa 80 ký tự' },
                {
                  pattern: /^[a-zA-Z0-9_]+$/,
                  message: 'Mã chỉ được chứa chữ cái tiếng Anh không dấu, số và dấu gạch dưới (_)',
                },
              ]}
              extra="Mã duy nhất trong cùng phân loại, tự động viết hoa (vd: EPIGASTRIC_PAIN, OMEPRAZOLE, GERD)."
            >
              <Input
                placeholder="vd: EPIGASTRIC_PAIN"
                className="font-mono uppercase"
                onChange={(e) => {
                  const val = e.target.value.toUpperCase().replace(/\s+/g, '_')
                  form.setFieldValue('code', val)
                }}
              />
            </Form.Item>

            <Form.Item
              noStyle
              shouldUpdate={(prev, cur) => prev.type !== cur.type}
            >
              {({ getFieldValue }) => {
                const currentType = getFieldValue('type')
                return (
                  <Form.Item
                    name="triageCode"
                    label={
                      <span className="flex items-center gap-1.5">
                        Mã Triage liên kết
                        <Tooltip title="Liên kết triệu chứng với nhóm dấu hiệu khẩn cấp cấp cứu trong Triage tầng 1. Có thể bỏ trống nếu là triệu chứng thông thường hoặc phân loại khác.">
                          <Text type="secondary" className="text-xs font-normal">
                            (Khuyên dùng cho Triệu chứng)
                          </Text>
                        </Tooltip>
                      </span>
                    }
                    extra={
                      currentType === 'SYMPTOM'
                        ? 'Chọn mã nhóm cấp cứu gợi ý hoặc gõ mã tùy chỉnh để kích hoạt cờ cảnh báo Triage.'
                        : 'Không bắt buộc cho bệnh lý và thuốc.'
                    }
                  >
                    <AutoComplete
                      options={TRIAGE_SUGGESTIONS}
                      placeholder="vd: BLACK_OR_BLOODY_STOOL, ABDOMINAL_PAIN..."
                      filterOption={(inputValue, option) =>
                        (option?.value?.toUpperCase().indexOf(inputValue.toUpperCase()) ?? -1) !== -1 ||
                        (option?.label?.toUpperCase().indexOf(inputValue.toUpperCase()) ?? -1) !== -1
                      }
                      allowClear
                      className="font-mono"
                    />
                  </Form.Item>
                )
              }}
            </Form.Item>

            <div className="grid grid-cols-2 gap-4">
              <Form.Item
                name="sortOrder"
                label="Thứ tự hiển thị (Sort Order)"
                rules={[{ required: true, message: 'Vui lòng nhập thứ tự' }]}
                extra="Số nhỏ hơn sẽ đứng trước trong danh sách dropdown."
              >
                <InputNumber min={0} step={10} className="w-full" />
              </Form.Item>

              <Form.Item
                name="active"
                label="Trạng thái kích hoạt"
                valuePropName="checked"
                extra="Nếu tắt, mục này sẽ tạm ẩn trong các dropdown người dùng."
              >
                <Switch checkedChildren="Đang dùng" unCheckedChildren="Đang ẩn" />
              </Form.Item>
            </div>
          </Form>
        </Modal>
      </div>
    </CmsAppShell>
  )
}
