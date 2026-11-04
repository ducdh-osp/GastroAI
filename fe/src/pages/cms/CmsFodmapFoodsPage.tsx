import { DeleteOutlined, EditOutlined, PlusOutlined, ReloadOutlined, SearchOutlined } from '@ant-design/icons'
import {
  Alert,
  Badge,
  Button,
  Card,
  Form,
  Input,
  Modal,
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
  message,
} from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { useCallback, useEffect, useMemo, useState } from 'react'
import {
  FODMAP_GROUPS,
  createCmsFodmapFood,
  deleteCmsFodmapFood,
  dismissCmsFodmapUnknownName,
  listCmsFodmapFoods,
  listCmsFodmapUnknownNames,
  toggleCmsFodmapFoodActive,
  updateCmsFodmapFood,
  type CmsFodmapFood,
  type CmsFodmapFoodRequest,
  type CmsFodmapUnknownName,
  type FodmapGroup,
  type FodmapLevel,
} from '../../api/cmsFodmapFoods'
import { CmsAppShell } from '../../components/cms/CmsAppShell'
import { useInFlightGuard } from '../../hooks/useInFlightGuard'
import { formatDateTime } from '../../lib/format'

const { Title } = Typography

const LEVEL_CONFIG: Record<FodmapLevel, { label: string; color: string }> = {
  LOW: { label: 'Thấp', color: 'green' },
  MEDIUM: { label: 'Trung bình', color: 'orange' },
  HIGH: { label: 'Cao', color: 'red' },
}

const GROUP_LABEL: Record<FodmapGroup, string> = {
  FRUCTAN: 'Fructan',
  GOS: 'GOS',
  LACTOSE: 'Lactose',
  FRUCTOSE: 'Fructose',
  POLYOL: 'Polyol',
}

type ActiveFilter = 'ALL' | 'ACTIVE' | 'INACTIVE'

interface FoodFormValues {
  name: string
  aliases?: string[]
  level: FodmapLevel
  groups?: FodmapGroup[]
  servingNote?: string
  note?: string
  source: string
}

function normalize(text: string): string {
  return text
    .toLowerCase()
    .replace(/đ/g, 'd')
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .replace(/[^a-z0-9]+/g, ' ')
    .trim()
}

function errorText(err: unknown, fallback: string): string {
  return err instanceof Error ? err.message : fallback
}

export default function CmsFodmapFoodsPage() {
  const [activeTab, setActiveTab] = useState<'foods' | 'unknown'>('foods')

  const [foods, setFoods] = useState<CmsFodmapFood[]>([])
  const [foodsLoading, setFoodsLoading] = useState(false)
  const [unknownNames, setUnknownNames] = useState<CmsFodmapUnknownName[]>([])
  const [unknownLoading, setUnknownLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const [searchText, setSearchText] = useState('')
  const [levelFilter, setLevelFilter] = useState<FodmapLevel | 'ALL'>('ALL')
  const [activeFilter, setActiveFilter] = useState<ActiveFilter>('ALL')

  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState<CmsFodmapFood | null>(null)
  const [saving, setSaving] = useState(false)
  const [modalError, setModalError] = useState<string | null>(null)
  const [form] = Form.useForm<FoodFormValues>()

  const toggleGuard = useInFlightGuard<number>()
  const deleteGuard = useInFlightGuard<number>()
  const dismissGuard = useInFlightGuard<number>()

  const loadFoods = useCallback(async () => {
    setFoodsLoading(true)
    setError(null)
    try {
      setFoods(await listCmsFodmapFoods())
    } catch (err) {
      setError(errorText(err, 'Không thể tải danh mục FODMAP.'))
    } finally {
      setFoodsLoading(false)
    }
  }, [])

  const loadUnknown = useCallback(async () => {
    setUnknownLoading(true)
    try {
      setUnknownNames(await listCmsFodmapUnknownNames())
    } catch (err) {
      setError(errorText(err, 'Không thể tải danh sách tên chưa có dữ liệu.'))
    } finally {
      setUnknownLoading(false)
    }
  }, [])

  useEffect(() => {
    loadFoods()
    loadUnknown()
  }, [loadFoods, loadUnknown])

  const stats = useMemo(
    () => ({
      total: foods.length,
      LOW: foods.filter((f) => f.level === 'LOW').length,
      MEDIUM: foods.filter((f) => f.level === 'MEDIUM').length,
      HIGH: foods.filter((f) => f.level === 'HIGH').length,
    }),
    [foods],
  )

  const filteredFoods = useMemo(() => {
    const keyword = normalize(searchText)
    return foods.filter((food) => {
      if (levelFilter !== 'ALL' && food.level !== levelFilter) return false
      if (activeFilter === 'ACTIVE' && !food.active) return false
      if (activeFilter === 'INACTIVE' && food.active) return false
      if (keyword) {
        const keys = [food.name, ...food.aliases].map(normalize)
        if (!keys.some((key) => key.includes(keyword))) return false
      }
      return true
    })
  }, [foods, searchText, levelFilter, activeFilter])

  function openCreate(prefillName?: string) {
    setEditing(null)
    setModalError(null)
    form.resetFields()
    form.setFieldsValue({ name: prefillName ?? '', aliases: [], level: 'LOW', groups: [] })
    setModalOpen(true)
  }

  function openEdit(food: CmsFodmapFood) {
    setEditing(food)
    setModalError(null)
    form.resetFields()
    form.setFieldsValue({
      name: food.name,
      aliases: food.aliases,
      level: food.level,
      groups: food.groups,
      servingNote: food.servingNote ?? undefined,
      note: food.note ?? undefined,
      source: food.source,
    })
    setModalOpen(true)
  }

  async function handleSubmit() {
    let values: FoodFormValues
    try {
      values = await form.validateFields()
    } catch {
      return
    }

    const payload: CmsFodmapFoodRequest = {
      name: values.name.trim(),
      aliases: (values.aliases ?? []).map((a) => a.trim()).filter(Boolean),
      level: values.level,
      groups: values.groups ?? [],
      servingNote: values.servingNote?.trim() || null,
      note: values.note?.trim() || null,
      source: values.source.trim(),
    }

    setSaving(true)
    setModalError(null)
    try {
      if (editing) {
        const updated = await updateCmsFodmapFood(editing.id, payload)
        setFoods((prev) => prev.map((f) => (f.id === updated.id ? updated : f)))
        message.success(`Đã lưu món "${updated.name}"`)
      } else {
        const created = await createCmsFodmapFood(payload)
        message.success(`Đã thêm món "${created.name}"`)
        loadFoods()
      }
      setModalOpen(false)
      loadUnknown()
    } catch (err) {
      setModalError(errorText(err, 'Không thể lưu món FODMAP.'))
    } finally {
      setSaving(false)
    }
  }

  async function handleToggle(food: CmsFodmapFood) {
    await toggleGuard.run(food.id, async () => {
      try {
        const updated = await toggleCmsFodmapFoodActive(food.id)
        setFoods((prev) => prev.map((f) => (f.id === updated.id ? updated : f)))
        message.success(updated.active ? `Đã bật món "${updated.name}"` : `Đã tắt món "${updated.name}"`)
      } catch (err) {
        message.error(errorText(err, 'Không thể thay đổi trạng thái món FODMAP.'))
      }
    })
  }

  async function handleDelete(food: CmsFodmapFood) {
    await deleteGuard.run(food.id, async () => {
      try {
        await deleteCmsFodmapFood(food.id)
        setFoods((prev) => prev.filter((f) => f.id !== food.id))
        message.success(`Đã xoá món "${food.name}"`)
      } catch (err) {
        message.error(errorText(err, 'Không thể xoá món FODMAP.'))
      }
    })
  }

  async function handleDismiss(unknown: CmsFodmapUnknownName) {
    await dismissGuard.run(unknown.id, async () => {
      try {
        await dismissCmsFodmapUnknownName(unknown.id)
        setUnknownNames((prev) => prev.filter((u) => u.id !== unknown.id))
        message.success(`Đã bỏ qua "${unknown.name}"`)
      } catch (err) {
        message.error(errorText(err, 'Không thể bỏ qua tên này.'))
      }
    })
  }

  const foodColumns: ColumnsType<CmsFodmapFood> = [
    {
      title: 'Tên món',
      dataIndex: 'name',
      key: 'name',
      render: (name: string) => <span className="font-semibold text-slate-800 dark:text-slate-100">{name}</span>,
    },
    {
      title: 'Tên gọi khác',
      dataIndex: 'aliases',
      key: 'aliases',
      render: (aliases: string[]) =>
        aliases.length === 0 ? (
          <span className="text-slate-300 dark:text-slate-600">—</span>
        ) : (
          <div className="flex flex-wrap gap-1">
            {aliases.map((alias) => (
              <Tag key={alias} className="!m-0">
                {alias}
              </Tag>
            ))}
          </div>
        ),
    },
    {
      title: 'Mức FODMAP',
      dataIndex: 'level',
      key: 'level',
      width: 130,
      render: (level: FodmapLevel) => <Tag color={LEVEL_CONFIG[level].color}>{LEVEL_CONFIG[level].label}</Tag>,
    },
    {
      title: 'Nhóm',
      dataIndex: 'groups',
      key: 'groups',
      width: 180,
      render: (groups: FodmapGroup[]) =>
        groups.length === 0 ? (
          <span className="text-slate-300 dark:text-slate-600">—</span>
        ) : (
          <div className="flex flex-wrap gap-1">
            {groups.map((group) => (
              <Tag key={group} color="purple" className="!m-0">
                {GROUP_LABEL[group]}
              </Tag>
            ))}
          </div>
        ),
    },
    {
      title: 'Khẩu phần',
      dataIndex: 'servingNote',
      key: 'servingNote',
      width: 150,
      render: (servingNote: string | null) => servingNote ?? <span className="text-slate-300 dark:text-slate-600">—</span>,
    },
    {
      title: 'Trạng thái',
      dataIndex: 'active',
      key: 'active',
      width: 110,
      align: 'center',
      render: (active: boolean, food) => (
        <Switch
          checked={active}
          size="small"
          checkedChildren="Bật"
          unCheckedChildren="Tắt"
          loading={toggleGuard.inFlightId === food.id}
          onChange={() => handleToggle(food)}
        />
      ),
    },
    {
      title: 'Cập nhật lúc',
      dataIndex: 'updatedAt',
      key: 'updatedAt',
      width: 160,
      render: formatDateTime,
    },
    {
      title: 'Thao tác',
      key: 'actions',
      width: 100,
      align: 'center',
      render: (_value, food) => (
        <Space size="small">
          <Tooltip title="Sửa">
            <Button size="small" type="text" icon={<EditOutlined className="text-blue-500" />} onClick={() => openEdit(food)} />
          </Tooltip>
          <Popconfirm
            title={`Xoá món "${food.name}"?`}
            okText="Xoá"
            cancelText="Huỷ"
            okButtonProps={{ danger: true, loading: deleteGuard.inFlightId === food.id }}
            onConfirm={() => handleDelete(food)}
          >
            <Tooltip title="Xoá">
              <Button size="small" type="text" danger icon={<DeleteOutlined />} loading={deleteGuard.inFlightId === food.id} />
            </Tooltip>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  const unknownColumns: ColumnsType<CmsFodmapUnknownName> = [
    {
      title: 'Tên',
      dataIndex: 'name',
      key: 'name',
      render: (name: string) => <span className="font-semibold text-slate-800 dark:text-slate-100">{name}</span>,
    },
    {
      title: 'Số lần gặp',
      dataIndex: 'hitCount',
      key: 'hitCount',
      width: 120,
      align: 'center',
    },
    {
      title: 'Gặp lần đầu',
      dataIndex: 'firstSeenAt',
      key: 'firstSeenAt',
      width: 170,
      render: formatDateTime,
    },
    {
      title: 'Gặp gần nhất',
      dataIndex: 'lastSeenAt',
      key: 'lastSeenAt',
      width: 170,
      render: formatDateTime,
    },
    {
      title: 'Thao tác',
      key: 'actions',
      width: 240,
      align: 'center',
      render: (_value, unknown) => (
        <Space size="small">
          <Button size="small" type="primary" icon={<PlusOutlined />} onClick={() => openCreate(unknown.name)}>
            Thêm vào danh mục
          </Button>
          <Popconfirm
            title={`Bỏ qua "${unknown.name}"?`}
            okText="Bỏ qua"
            cancelText="Huỷ"
            okButtonProps={{ loading: dismissGuard.inFlightId === unknown.id }}
            onConfirm={() => handleDismiss(unknown)}
          >
            <Button size="small" loading={dismissGuard.inFlightId === unknown.id}>
              Bỏ qua
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <CmsAppShell>
      <div className="mx-auto max-w-7xl px-4 py-6">
        <div className="mb-6 flex flex-wrap items-center justify-between gap-4">
          <Title level={2} className="!mb-0 tracking-tight">
            Danh mục FODMAP
          </Title>
          <Space>
            <Button
              icon={<ReloadOutlined />}
              loading={foodsLoading || unknownLoading}
              onClick={() => {
                loadFoods()
                loadUnknown()
              }}
            >
              Làm mới
            </Button>
            <Button type="primary" icon={<PlusOutlined />} onClick={() => openCreate()}>
              Thêm món
            </Button>
          </Space>
        </div>

        {error && <Alert type="error" title={error} showIcon closable className="mb-4" onClose={() => setError(null)} />}

        <div className="mb-6 grid grid-cols-2 gap-4 md:grid-cols-4">
          <Card styles={{ body: { padding: 16 } }}>
            <div className="text-xs font-medium uppercase text-slate-500">Tổng số món</div>
            <div className="mt-1 text-2xl font-bold text-slate-800 dark:text-slate-100">{stats.total}</div>
          </Card>
          {(Object.keys(LEVEL_CONFIG) as FodmapLevel[]).map((level) => (
            <Card key={level} styles={{ body: { padding: 16 } }}>
              <div className="text-xs font-medium uppercase text-slate-500">{LEVEL_CONFIG[level].label}</div>
              <div className="mt-1">
                <Tag color={LEVEL_CONFIG[level].color} className="!m-0 !text-xl !font-bold !px-3 !py-0.5">
                  {stats[level]}
                </Tag>
              </div>
            </Card>
          ))}
        </div>

        <Card styles={{ body: { padding: '8px 20px 20px' } }}>
          <Tabs
            activeKey={activeTab}
            onChange={(key) => setActiveTab(key as 'foods' | 'unknown')}
            items={[
              {
                key: 'foods',
                label: 'Danh mục',
                children: (
                  <>
                    <div className="mb-4 flex flex-wrap items-center gap-2.5">
                      <Input
                        placeholder="Tìm tên món hoặc tên gọi khác"
                        prefix={<SearchOutlined className="text-slate-400" />}
                        value={searchText}
                        onChange={(e) => setSearchText(e.target.value)}
                        allowClear
                        className="w-72"
                      />
                      <Select
                        value={levelFilter}
                        onChange={setLevelFilter}
                        className="w-40"
                        options={[
                          { value: 'ALL', label: 'Tất cả mức' },
                          { value: 'LOW', label: 'Thấp' },
                          { value: 'MEDIUM', label: 'Trung bình' },
                          { value: 'HIGH', label: 'Cao' },
                        ]}
                      />
                      <Select
                        value={activeFilter}
                        onChange={setActiveFilter}
                        className="w-40"
                        options={[
                          { value: 'ALL', label: 'Tất cả trạng thái' },
                          { value: 'ACTIVE', label: 'Đang bật' },
                          { value: 'INACTIVE', label: 'Đã tắt' },
                        ]}
                      />
                    </div>
                    <Table
                      columns={foodColumns}
                      dataSource={filteredFoods}
                      rowKey="id"
                      loading={foodsLoading}
                      pagination={{
                        pageSize: 20,
                        showSizeChanger: true,
                        pageSizeOptions: ['20', '50', '100'],
                        showTotal: (total, range) => `${range[0]}-${range[1]} của ${total} món`,
                      }}
                      locale={{ emptyText: 'Chưa có món nào' }}
                    />
                  </>
                ),
              },
              {
                key: 'unknown',
                label: (
                  <span className="flex items-center gap-1.5">
                    Chưa có dữ liệu <Badge count={unknownNames.length} overflowCount={999} />
                  </span>
                ),
                children: (
                  <Table
                    columns={unknownColumns}
                    dataSource={unknownNames}
                    rowKey="id"
                    loading={unknownLoading}
                    pagination={{ pageSize: 20, showSizeChanger: false }}
                    locale={{ emptyText: 'Không có tên nào chưa có dữ liệu' }}
                  />
                ),
              },
            ]}
          />
        </Card>

        <Modal
          title={editing ? `Sửa món "${editing.name}"` : 'Thêm món FODMAP'}
          open={modalOpen}
          onOk={handleSubmit}
          onCancel={() => setModalOpen(false)}
          confirmLoading={saving}
          okText={editing ? 'Lưu' : 'Thêm'}
          cancelText="Huỷ"
          destroyOnHidden
          width={640}
        >
          {modalError && <Alert type="error" title={modalError} showIcon className="mb-4" />}

          <Form form={form} layout="vertical" className="mt-4">
            <Form.Item
              name="name"
              label="Tên món"
              rules={[
                { required: true, whitespace: true, message: 'Vui lòng nhập tên món' },
                { min: 2, max: 120, message: 'Tên món từ 2 đến 120 ký tự' },
              ]}
            >
              <Input placeholder="vd: Hành tây" />
            </Form.Item>

            <Form.Item
              name="aliases"
              label="Tên gọi khác"
              rules={[
                {
                  validator: (_rule, value?: string[]) => {
                    const list = value ?? []
                    if (list.length > 10) return Promise.reject(new Error('Tối đa 10 tên gọi khác'))
                    if (list.some((a) => a.length > 60)) return Promise.reject(new Error('Mỗi tên gọi khác tối đa 60 ký tự'))
                    return Promise.resolve()
                  },
                },
              ]}
            >
              <Select mode="tags" tokenSeparators={[',']} open={false} placeholder="vd: hành, onion" />
            </Form.Item>

            <Form.Item name="level" label="Mức FODMAP" rules={[{ required: true, message: 'Vui lòng chọn mức FODMAP' }]}>
              <Radio.Group buttonStyle="solid">
                <Radio.Button value="LOW">Thấp</Radio.Button>
                <Radio.Button value="MEDIUM">Trung bình</Radio.Button>
                <Radio.Button value="HIGH">Cao</Radio.Button>
              </Radio.Group>
            </Form.Item>

            <Form.Item name="groups" label="Nhóm FODMAP">
              <Select
                mode="multiple"
                allowClear
                placeholder="Chọn nhóm"
                options={FODMAP_GROUPS.map((group) => ({ value: group, label: GROUP_LABEL[group] }))}
              />
            </Form.Item>

            <Form.Item
              name="servingNote"
              label="Khẩu phần tham khảo"
              dependencies={['level']}
              rules={[
                ({ getFieldValue }) => ({
                  validator: (_rule, value?: string) => {
                    if (getFieldValue('level') === 'MEDIUM' && !value?.trim()) {
                      return Promise.reject(new Error('Món mức Trung bình phải có khẩu phần tham khảo'))
                    }
                    return Promise.resolve()
                  },
                }),
                { max: 100, message: 'Khẩu phần tham khảo tối đa 100 ký tự' },
              ]}
            >
              <Input placeholder="vd: Khoảng 60 g" />
            </Form.Item>

            <Form.Item name="note" label="Ghi chú" rules={[{ max: 500, message: 'Ghi chú tối đa 500 ký tự' }]}>
              <Input.TextArea rows={2} />
            </Form.Item>

            <Form.Item
              name="source"
              label="Nguồn tham khảo"
              rules={[
                { required: true, whitespace: true, message: 'Vui lòng nhập nguồn tham khảo' },
                { max: 255, message: 'Nguồn tham khảo tối đa 255 ký tự' },
              ]}
            >
              <Input placeholder="vd: Monash University FODMAP Diet App" />
            </Form.Item>
          </Form>
        </Modal>
      </div>
    </CmsAppShell>
  )
}
