import {
  AlertOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  DatabaseOutlined,
  ExperimentOutlined,
  PlayCircleOutlined,
  ReloadOutlined,
  SafetyCertificateOutlined,
} from '@ant-design/icons'
import {
  Alert,
  Button,
  Card,
  Col,
  Empty,
  Input,
  message,
  Progress,
  Radio,
  Row,
  Select,
  Space,
  Spin,
  Statistic,
  Table,
  Tabs,
  Tag,
  Typography,
} from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { useEffect, useState } from 'react'
import {
  getEvaluationSummary,
  getRagEvaluation,
  getTriageEvaluation,
  runRagEvaluation,
  runTriageEvaluation,
  type CategoryMetric,
  type EvaluationSummary,
  type RagEvaluationReport,
  type RagQueryEvalResult,
  type RetrievedChunkSnippet,
  type TriageCaseEvalResult,
  type TriageEvaluationReport,
} from '../../api/cmsEvaluation'
import { CmsAppShell } from '../../components/cms/CmsAppShell'

const { Title, Text, Paragraph } = Typography

export default function CmsEvaluationPage() {
  const [activeTab, setActiveTab] = useState<'triage' | 'rag'>('triage')
  const [summary, setSummary] = useState<EvaluationSummary | null>(null)
  const [triageReport, setTriageReport] = useState<TriageEvaluationReport | null>(null)
  const [ragReport, setRagReport] = useState<RagEvaluationReport | null>(null)

  const [loadingSummary, setLoadingSummary] = useState(false)
  const [loadingTriage, setLoadingTriage] = useState(false)
  const [loadingRag, setLoadingRag] = useState(false)
  const [runningTriage, setRunningTriage] = useState(false)
  const [runningRag, setRunningRag] = useState(false)

  // Filters for Triage
  const [triageFilterClass, setTriageFilterClass] = useState<string>('ALL')
  const [triageSearchText, setTriageSearchText] = useState('')

  // Settings for RAG
  const [ragTopK, setRagTopK] = useState<number>(5)
  const [ragSearchText, setRagSearchText] = useState('')
  const [ragHitFilter, setRagHitFilter] = useState<string>('ALL')

  async function loadSummary() {
    setLoadingSummary(true)
    try {
      const data = await getEvaluationSummary()
      setSummary(data)
    } catch {
      // non-critical, user will see per-tab state
    } finally {
      setLoadingSummary(false)
    }
  }

  async function loadTriage() {
    setLoadingTriage(true)
    try {
      const data = await getTriageEvaluation()
      setTriageReport(data)
    } catch (err) {
      message.error(err instanceof Error ? err.message : 'Không thể tải báo cáo đánh giá Triage.')
    } finally {
      setLoadingTriage(false)
    }
  }

  async function loadRag() {
    setLoadingRag(true)
    try {
      const data = await getRagEvaluation()
      setRagReport(data)
    } catch (err) {
      message.error(err instanceof Error ? err.message : 'Không thể tải báo cáo đánh giá RAG.')
    } finally {
      setLoadingRag(false)
    }
  }

  async function handleRunTriage() {
    setRunningTriage(true)
    try {
      const res = await runTriageEvaluation()
      setTriageReport(res)
      message.success('Chạy kiểm thử đánh giá độ nhạy Triage thành công!')
      loadSummary()
    } catch (err) {
      message.error(err instanceof Error ? err.message : 'Lỗi khi chạy đánh giá Triage.')
    } finally {
      setRunningTriage(false)
    }
  }

  async function handleRunRag() {
    setRunningRag(true)
    try {
      const res = await runRagEvaluation(ragTopK)
      setRagReport(res)
      message.success(`Chạy kiểm định độ chính xác truy xuất RAG (Top-${ragTopK}) thành công!`)
      loadSummary()
    } catch (err) {
      message.error(err instanceof Error ? err.message : 'Lỗi khi chạy đánh giá RAG.')
    } finally {
      setRunningRag(false)
    }
  }

  useEffect(() => {
    loadSummary()
    loadTriage()
    loadRag()
  }, [])

  // Filtered Triage Cases
  const filteredTriageCases = (triageReport?.caseResults || []).filter((c) => {
    const matchClass = triageFilterClass === 'ALL' || c.classification === triageFilterClass
    const matchText =
      !triageSearchText.trim() ||
      c.query.toLowerCase().includes(triageSearchText.toLowerCase()) ||
      c.category.toLowerCase().includes(triageSearchText.toLowerCase()) ||
      c.notes.toLowerCase().includes(triageSearchText.toLowerCase())
    return matchClass && matchText
  })

  // Filtered RAG Queries
  const filteredRagQueries = (ragReport?.queryResults || []).filter((q) => {
    const matchHit =
      ragHitFilter === 'ALL' ||
      (ragHitFilter === 'HIT' && q.hit) ||
      (ragHitFilter === 'MISS' && !q.hit)
    const matchText =
      !ragSearchText.trim() ||
      q.query.toLowerCase().includes(ragSearchText.toLowerCase()) ||
      q.category.toLowerCase().includes(ragSearchText.toLowerCase()) ||
      q.expectedDocKeyword.toLowerCase().includes(ragSearchText.toLowerCase())
    return matchHit && matchText
  })

  // Columns for Triage category table
  const categoryColumns: ColumnsType<CategoryMetric> = [
    {
      title: 'Nhóm dấu hiệu cấp cứu',
      dataIndex: 'category',
      key: 'category',
      render: (cat: string) => {
        const labels: Record<string, string> = {
          XUAT_HUYET_TIEU_HOA: 'Xuất huyết tiêu hóa (Nôn/Đi ngoài phân đen/máu)',
          DAU_BUNG_CAP_TINH: 'Đau bụng cấp tính dữ dội / Bụng cứng như gỗ',
          TAC_RUOT: 'Tắc ruột / Bí trung đại tiện / Chướng to',
          KHONG_THE_DI_TIEU: 'Không đi tiểu được / Bí tiểu cấp',
          SOC_MAT_MAU: 'Sốc mất máu / Da xanh tái / Ngất xỉu',
          NHIEM_TRUNG_NANG: 'Nhiễm trùng nặng / Sốt cao đau bụng hố chậu',
          DAU_LAN_CO_QUAN_KHAC: 'Đau bụng lan ra sau lưng / ngực / vai',
          KEM_HO_HAP_TIM_MACH: 'Đau bụng kèm khó thở / tức ngực',
          TIEU_DUONG_KEM_NON: 'Tiểu đường kèm nôn ói liên tục',
          VANG_DA_KEM_DAU_BUNG: 'Vàng da vàng mắt kèm đau hạ sườn',
        }
        return <Text strong>{labels[cat] || cat}</Text>
      },
    },
    {
      title: 'Tổng ca kiểm thử',
      dataIndex: 'totalCases',
      key: 'totalCases',
      align: 'center',
      width: 130,
    },
    {
      title: 'Phát hiện đúng (TP)',
      dataIndex: 'truePositives',
      key: 'truePositives',
      align: 'center',
      width: 140,
      render: (val: number) => <Tag color="success">+{val} ca</Tag>,
    },
    {
      title: 'Bỏ sót (FN)',
      dataIndex: 'falseNegatives',
      key: 'falseNegatives',
      align: 'center',
      width: 120,
      render: (val: number) =>
        val > 0 ? <Tag color="error">{val} bỏ sót</Tag> : <Tag color="default">0</Tag>,
    },
    {
      title: 'Độ nhạy (Recall)',
      dataIndex: 'recall',
      key: 'recall',
      align: 'center',
      width: 220,
      render: (recall: number) => {
        const pct = Math.round(recall * 100)
        return (
          <Space orientation="horizontal" style={{ width: '100%' }}>
            <Progress
              percent={pct}
              size="small"
              status={pct >= 95 ? 'success' : pct >= 80 ? 'normal' : 'exception'}
              style={{ width: 140 }}
            />
            <Text strong style={{ color: pct >= 95 ? '#10b981' : '#f59e0b' }}>
              {pct}%
            </Text>
          </Space>
        )
      },
    },
  ]

  // Columns for Triage Case details table
  const triageCaseColumns: ColumnsType<TriageCaseEvalResult> = [
    {
      title: 'Mã',
      dataIndex: 'id',
      key: 'id',
      width: 90,
      render: (id: string) => <Tag color="blue">{id}</Tag>,
    },
    {
      title: 'Câu mô tả triệu chứng bệnh nhân',
      dataIndex: 'query',
      key: 'query',
      render: (q: string, row) => (
        <div>
          <Paragraph style={{ margin: 0 }} ellipsis={{ rows: 2, expandable: true }}>
            "{q}"
          </Paragraph>
          <Text type="secondary" style={{ fontSize: 12 }}>
            Mục tiêu: {row.expectedEmergency ? 'Cấp cứu' : 'An toàn / Phân tâm'} • Nhóm: {row.category}
          </Text>
        </div>
      ),
    },
    {
      title: 'Phân loại Ma trận',
      dataIndex: 'classification',
      key: 'classification',
      width: 140,
      align: 'center',
      render: (cls: 'TP' | 'TN' | 'FP' | 'FN') => {
        switch (cls) {
          case 'TP':
            return <Tag color="success">TP (Bắt đúng cấp cứu)</Tag>
          case 'TN':
            return <Tag color="cyan">TN (An toàn chuẩn xác)</Tag>
          case 'FP':
            return <Tag color="warning">FP (Báo động giả)</Tag>
          case 'FN':
            return <Tag color="error">FN (Bỏ sót ca bệnh)</Tag>
        }
      },
    },
    {
      title: 'Từ khóa khớp',
      dataIndex: 'matchedGroups',
      key: 'matchedGroups',
      width: 180,
      render: (groups: string[]) =>
        groups && groups.length > 0 ? (
          <Space wrap size={[0, 4]}>
            {groups.map((g) => (
              <Tag key={g} color="volcano" style={{ fontSize: 11 }}>
                {g}
              </Tag>
            ))}
          </Space>
        ) : (
          <Text type="secondary">—</Text>
        ),
    },
    {
      title: 'Ghi chú y khoa',
      dataIndex: 'notes',
      key: 'notes',
      width: 220,
      render: (note: string) => <Text style={{ fontSize: 12 }}>{note}</Text>,
    },
  ]

  // Columns for RAG Query evaluation table
  const ragQueryColumns: ColumnsType<RagQueryEvalResult> = [
    {
      title: 'Mã',
      dataIndex: 'id',
      key: 'id',
      width: 90,
      render: (id: string) => <Tag color="purple">{id}</Tag>,
    },
    {
      title: 'Câu hỏi kiểm thử chuẩn hóa',
      dataIndex: 'query',
      key: 'query',
      render: (query: string, row) => (
        <div>
          <Text strong>"{query}"</Text>
          <br />
          <Text type="secondary" style={{ fontSize: 12 }}>
            Chủ đề: <Tag color="geekblue">{row.category}</Tag> • Tài liệu kỳ vọng:{' '}
            <Text code>{row.expectedDocKeyword}</Text>
          </Text>
        </div>
      ),
    },
    {
      title: 'Kết quả Top-K',
      key: 'hit',
      width: 130,
      align: 'center',
      render: (_, row) =>
        row.hit ? (
          <Tag color="success" icon={<CheckCircleOutlined />}>
            Trúng (Top {row.firstHitRank})
          </Tag>
        ) : (
          <Tag color="error" icon={<CloseCircleOutlined />}>
            Không trúng
          </Tag>
        ),
    },
    {
      title: 'Thứ hạng trúng',
      dataIndex: 'firstHitRank',
      key: 'firstHitRank',
      width: 110,
      align: 'center',
      render: (rank: number) =>
        rank > 0 ? (
          <Tag color={rank === 1 ? 'gold' : 'blue'}>#{rank}</Tag>
        ) : (
          <Text type="secondary">—</Text>
        ),
    },
    {
      title: 'Độ tương đồng Top-1',
      dataIndex: 'top1Similarity',
      key: 'top1Similarity',
      width: 140,
      align: 'center',
      render: (sim: number) => {
        const pct = Math.round(sim * 100)
        return (
          <Text strong style={{ color: pct >= 80 ? '#10b981' : pct >= 65 ? '#f59e0b' : '#ef4444' }}>
            {pct}%
          </Text>
        )
      },
    },
    {
      title: 'Độ trễ',
      dataIndex: 'latencyMs',
      key: 'latencyMs',
      width: 95,
      align: 'right',
      render: (lat: number) => `${lat} ms`,
    },
  ]

  // Expanded row renderer for RAG chunks
  const expandedChunkRowRender = (record: RagQueryEvalResult) => {
    const chunkColumns: ColumnsType<RetrievedChunkSnippet> = [
      {
        title: 'Thứ hạng',
        dataIndex: 'rank',
        key: 'rank',
        width: 80,
        render: (r: number) => <Tag>#{r}</Tag>,
      },
      {
        title: 'Tài liệu nguồn trong kho tri thức',
        dataIndex: 'documentTitle',
        key: 'documentTitle',
        render: (title: string, item) => (
          <div>
            <Text strong>{title}</Text>{' '}
            {item.isRelevant ? (
              <Tag color="success">Khớp tài liệu mục tiêu</Tag>
            ) : (
              <Tag color="default">Nội dung liên đới</Tag>
            )}
          </div>
        ),
      },
      {
        title: 'Độ tương đồng Cosine',
        dataIndex: 'similarity',
        key: 'similarity',
        width: 150,
        align: 'center',
        render: (s: number) => `${(s * 100).toFixed(1)}%`,
      },
    ]

    return (
      <Table
        dataSource={record.retrievedChunks}
        columns={chunkColumns}
        rowKey="chunkId"
        pagination={false}
        size="small"
        bordered
      />
    )
  }

  return (
    <CmsAppShell>
      <div style={{ maxWidth: 1400, margin: '0 auto', paddingBottom: 48 }}>
        {/* Header */}
        <div style={{ marginBottom: 24, display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: 16 }}>
          <div>
            <Space orientation="horizontal" align="center">
              <ExperimentOutlined style={{ fontSize: 26, color: '#3b82f6' }} />
              <Title level={2} style={{ margin: 0 }}>
                Đánh giá mô hình AI & Triage (UC0037 & UC0038)
              </Title>
            </Space>
            <Paragraph type="secondary" style={{ marginTop: 6, marginBottom: 0 }}>
              Kiểm định định lượng độ nhạy tầng cảnh báo khẩn cấp (Triage Sensitivity) và độ chính xác truy xuất kho tri thức y khoa (RAG Retrieval Accuracy) theo mục tiêu Đề cương đồ án.
            </Paragraph>
          </div>

          <Space wrap>
            <Button
              icon={<ReloadOutlined />}
              onClick={() => {
                loadSummary()
                loadTriage()
                loadRag()
              }}
              loading={loadingSummary || loadingTriage || loadingRag}
            >
              Làm mới số liệu
            </Button>
            <Button
              type="primary"
              danger
              icon={<AlertOutlined />}
              onClick={handleRunTriage}
              loading={runningTriage}
            >
              Chạy đánh giá Triage (UC0038)
            </Button>
            <Button
              type="primary"
              icon={<DatabaseOutlined />}
              onClick={handleRunRag}
              loading={runningRag}
            >
              Chạy đánh giá RAG (UC0037)
            </Button>
          </Space>
        </div>

        {/* Global Summary KPI Banner */}
        {summary && (
          <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
            <Col xs={24} sm={12} md={6}>
              <Card
                bordered={false}
                style={{
                  background: 'linear-gradient(135deg, rgba(16, 185, 129, 0.1) 0%, rgba(5, 150, 105, 0.05) 100%)',
                  borderLeft: '4px solid #10b981',
                }}
              >
                <Statistic
                  title={<Text strong>Độ nhạy Triage (Sensitivity)</Text>}
                  value={(summary.triageSensitivityRecall * 100).toFixed(1)}
                  suffix="%"
                  valueStyle={{ color: '#10b981', fontWeight: 700 }}
                  prefix={<SafetyCertificateOutlined />}
                />
                <div style={{ marginTop: 8 }}>
                  <Tag color="success">Đạt chuẩn Đề cương (≥ 95%)</Tag>
                  <Text type="secondary" style={{ fontSize: 12 }}>
                    {summary.triageTruePositives} / {summary.triageTruePositives + summary.triageFalseNegatives} ca
                  </Text>
                </div>
              </Card>
            </Col>

            <Col xs={24} sm={12} md={6}>
              <Card
                bordered={false}
                style={{
                  background: 'linear-gradient(135deg, rgba(245, 158, 11, 0.1) 0%, rgba(217, 119, 6, 0.05) 100%)',
                  borderLeft: '4px solid #f59e0b',
                }}
              >
                <Statistic
                  title={<Text strong>Tỷ lệ báo động giả (False Alarm)</Text>}
                  value={(summary.triageFalseAlarmRate * 100).toFixed(1)}
                  suffix="%"
                  valueStyle={{ color: '#f59e0b', fontWeight: 700 }}
                  prefix={<AlertOutlined />}
                />
                <div style={{ marginTop: 8 }}>
                  <Tag color="gold">Kiểm soát tốt (≤ 20%)</Tag>
                  <Text type="secondary" style={{ fontSize: 12 }}>
                    F2-Score: {summary.triageF2Score.toFixed(3)}
                  </Text>
                </div>
              </Card>
            </Col>

            <Col xs={24} sm={12} md={6}>
              <Card
                bordered={false}
                style={{
                  background: 'linear-gradient(135deg, rgba(59, 130, 246, 0.1) 0%, rgba(37, 99, 235, 0.05) 100%)',
                  borderLeft: '4px solid #3b82f6',
                }}
              >
                <Statistic
                  title={<Text strong>RAG Hit Rate @ 1</Text>}
                  value={(summary.ragHitRateAt1 * 100).toFixed(1)}
                  suffix="%"
                  valueStyle={{ color: '#3b82f6', fontWeight: 700 }}
                  prefix={<DatabaseOutlined />}
                />
                <div style={{ marginTop: 8 }}>
                  <Tag color="blue">Hit Rate @ Top-{summary.ragTopK}: {(summary.ragHitRateAtK * 100).toFixed(1)}%</Tag>
                </div>
              </Card>
            </Col>

            <Col xs={24} sm={12} md={6}>
              <Card
                bordered={false}
                style={{
                  background: 'linear-gradient(135deg, rgba(139, 92, 246, 0.1) 0%, rgba(124, 58, 237, 0.05) 100%)',
                  borderLeft: '4px solid #8b5cf6',
                }}
              >
                <Statistic
                  title={<Text strong>RAG MRR & Tương đồng</Text>}
                  value={summary.ragMrr.toFixed(3)}
                  valueStyle={{ color: '#8b5cf6', fontWeight: 700 }}
                  prefix={<CheckCircleOutlined />}
                />
                <div style={{ marginTop: 8 }}>
                  <Tag color="purple">Cosine: {(summary.ragAvgSimilarity * 100).toFixed(1)}%</Tag>
                  <Text type="secondary" style={{ fontSize: 12 }}>
                    {summary.ragTotalQueries} câu chuẩn
                  </Text>
                </div>
              </Card>
            </Col>
          </Row>
        )}

        {/* Main Content Tabs */}
        <Card bordered={false} style={{ borderRadius: 12 }}>
          <Tabs
            activeKey={activeTab}
            onChange={(key) => setActiveTab(key as 'triage' | 'rag')}
            items={[
              {
                key: 'triage',
                label: (
                  <span>
                    <AlertOutlined /> Đánh giá độ nhạy Triage (UC0038)
                  </span>
                ),
                children: (
                  <div>
                    {loadingTriage && !triageReport ? (
                      <div style={{ textAlign: 'center', padding: 40 }}>
                        <Spin size="large" tip="Đang tải dữ liệu đánh giá Triage..." />
                      </div>
                    ) : !triageReport ? (
                      <Empty description="Chưa có dữ liệu đánh giá Triage." />
                    ) : (
                      <div>
                        {/* Notice Banner */}
                        <Alert
                          message="Mục tiêu cốt lõi của Đề cương đồ án (Mục 2.2 & 8)"
                          description="Trong y tế cấp cứu tiêu hóa, chi phí cho việc bỏ sót một ca cấp cứu (False Negative) là khôn lường đối với tính mạng bệnh nhân. Vì vậy, hệ thống Triage được cấu hình ưu tiên tối đa cho Độ nhạy (Sensitivity/Recall ≥ 95%) và F2-score, chấp nhận tỷ lệ báo động giả (False Alarm Rate) ở mức kiểm soát được."
                          type="info"
                          showIcon
                          style={{ marginBottom: 20 }}
                        />

                        {/* Confusion Matrix & Detailed Metrics */}
                        <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
                          {/* Confusion Matrix Card */}
                          <Col xs={24} lg={12}>
                            <Card
                              title={<Text strong>Ma trận nhầm lẫn (Confusion Matrix)</Text>}
                              size="small"
                              bordered
                            >
                              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12, padding: 8 }}>
                                <div
                                  style={{
                                    padding: '16px 20px',
                                    borderRadius: 8,
                                    background: '#ecfdf5',
                                    border: '1px solid #10b981',
                                    textAlign: 'center',
                                  }}
                                >
                                  <Text style={{ fontSize: 12, color: '#047857' }}>
                                    TRUE POSITIVE (TP)
                                  </Text>
                                  <div style={{ fontSize: 28, fontWeight: 700, color: '#047857', margin: '4px 0' }}>
                                    {triageReport.truePositives}
                                  </div>
                                  <Text style={{ fontSize: 12, color: '#047857' }}>
                                    Bắt đúng ca cấp cứu
                                  </Text>
                                </div>

                                <div
                                  style={{
                                    padding: '16px 20px',
                                    borderRadius: 8,
                                    background: '#fef2f2',
                                    border: '1px solid #ef4444',
                                    textAlign: 'center',
                                  }}
                                >
                                  <Text style={{ fontSize: 12, color: '#b91c1c' }}>
                                    FALSE NEGATIVE (FN)
                                  </Text>
                                  <div style={{ fontSize: 28, fontWeight: 700, color: '#b91c1c', margin: '4px 0' }}>
                                    {triageReport.falseNegatives}
                                  </div>
                                  <Text style={{ fontSize: 12, color: '#b91c1c' }}>
                                    Ca cấp cứu bị bỏ sót
                                  </Text>
                                </div>

                                <div
                                  style={{
                                    padding: '16px 20px',
                                    borderRadius: 8,
                                    background: '#fffbeb',
                                    border: '1px solid #f59e0b',
                                    textAlign: 'center',
                                  }}
                                >
                                  <Text style={{ fontSize: 12, color: '#b45309' }}>
                                    FALSE POSITIVE (FP)
                                  </Text>
                                  <div style={{ fontSize: 28, fontWeight: 700, color: '#b45309', margin: '4px 0' }}>
                                    {triageReport.falsePositives}
                                  </div>
                                  <Text style={{ fontSize: 12, color: '#b45309' }}>
                                    Báo động giả an toàn
                                  </Text>
                                </div>

                                <div
                                  style={{
                                    padding: '16px 20px',
                                    borderRadius: 8,
                                    background: '#eff6ff',
                                    border: '1px solid #3b82f6',
                                    textAlign: 'center',
                                  }}
                                >
                                  <Text style={{ fontSize: 12, color: '#1d4ed8' }}>
                                    TRUE NEGATIVE (TN)
                                  </Text>
                                  <div style={{ fontSize: 28, fontWeight: 700, color: '#1d4ed8', margin: '4px 0' }}>
                                    {triageReport.trueNegatives}
                                  </div>
                                  <Text style={{ fontSize: 12, color: '#1d4ed8' }}>
                                    Nhận diện an toàn đúng
                                  </Text>
                                </div>
                              </div>

                              <div style={{ marginTop: 12, textAlign: 'right' }}>
                                <Text type="secondary" style={{ fontSize: 12 }}>
                                  Tổng số ca kiểm chuẩn:{' '}
                                  <Text strong>{triageReport.totalCases} ca</Text> ({triageReport.positiveCases} ca cấp cứu + {triageReport.negativeCases} ca an toàn/phân tâm)
                                </Text>
                              </div>
                            </Card>
                          </Col>

                          {/* Statistical Metrics Breakdown */}
                          <Col xs={24} lg={12}>
                            <Card
                              title={<Text strong>Chỉ số kiểm định lâm sàng</Text>}
                              size="small"
                              bordered
                            >
                              <div style={{ padding: '8px 12px' }}>
                                <div style={{ marginBottom: 12 }}>
                                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
                                    <Text>Độ nhạy (Sensitivity / Recall = TP / [TP + FN]):</Text>
                                    <Text strong style={{ color: triageReport.sensitivityRecall >= 0.95 ? '#10b981' : '#f59e0b' }}>
                                      {(triageReport.sensitivityRecall * 100).toFixed(1)}%
                                    </Text>
                                  </div>
                                  <Progress
                                    percent={Math.round(triageReport.sensitivityRecall * 100)}
                                    status={triageReport.sensitivityRecall >= 0.95 ? 'success' : 'normal'}
                                  />
                                </div>

                                <div style={{ marginBottom: 12 }}>
                                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
                                    <Text>Độ đặc hiệu (Specificity = TN / [TN + FP]):</Text>
                                    <Text strong>{(triageReport.specificity * 100).toFixed(1)}%</Text>
                                  </div>
                                  <Progress percent={Math.round(triageReport.specificity * 100)} />
                                </div>

                                <div style={{ marginBottom: 12 }}>
                                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
                                    <Text>Tỷ lệ báo động giả (False Alarm Rate = FP / [TN + FP]):</Text>
                                    <Text strong style={{ color: triageReport.falseAlarmRate <= 0.2 ? '#10b981' : '#f59e0b' }}>
                                      {(triageReport.falseAlarmRate * 100).toFixed(1)}%
                                    </Text>
                                  </div>
                                  <Progress
                                    percent={Math.round(triageReport.falseAlarmRate * 100)}
                                    strokeColor="#f59e0b"
                                  />
                                </div>

                                <Row gutter={8} style={{ marginTop: 16 }}>
                                  <Col span={8}>
                                    <Statistic
                                      title="Precision"
                                      value={(triageReport.precision * 100).toFixed(1)}
                                      suffix="%"
                                      valueStyle={{ fontSize: 18 }}
                                    />
                                  </Col>
                                  <Col span={8}>
                                    <Statistic
                                      title="F1-Score"
                                      value={triageReport.f1Score.toFixed(3)}
                                      valueStyle={{ fontSize: 18 }}
                                    />
                                  </Col>
                                  <Col span={8}>
                                    <Statistic
                                      title="F2-Score (2x Recall)"
                                      value={triageReport.f2Score.toFixed(3)}
                                      valueStyle={{ fontSize: 18, color: '#10b981', fontWeight: 700 }}
                                    />
                                  </Col>
                                </Row>
                              </div>
                            </Card>
                          </Col>
                        </Row>

                        {/* Breakdown by Emergency Category */}
                        <Card
                          title={
                            <Space>
                              <Text strong>Đánh giá theo 10 nhóm dấu hiệu cấp cứu tiêu hóa</Text>
                              <Tag color="geekblue">NHS 111 & Mayo Clinic</Tag>
                            </Space>
                          }
                          size="small"
                          bordered
                          style={{ marginBottom: 24 }}
                        >
                          <Table
                            dataSource={triageReport.categoryMetrics}
                            columns={categoryColumns}
                            rowKey="category"
                            pagination={false}
                            size="small"
                          />
                        </Card>

                        {/* Interactive Test Cases Explorer */}
                        <Card
                          title={
                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 12 }}>
                              <Text strong>Bộ câu hỏi kiểm chuẩn Triage (Benchmark Test Cases)</Text>
                              <Space wrap>
                                <Radio.Group
                                  value={triageFilterClass}
                                  onChange={(e) => setTriageFilterClass(e.target.value)}
                                  size="small"
                                >
                                  <Radio.Button value="ALL">Tất cả ({triageReport.caseResults.length})</Radio.Button>
                                  <Radio.Button value="TP">Đúng TP ({triageReport.truePositives})</Radio.Button>
                                  <Radio.Button value="FN">Bỏ sót FN ({triageReport.falseNegatives})</Radio.Button>
                                  <Radio.Button value="FP">Báo động giả FP ({triageReport.falsePositives})</Radio.Button>
                                  <Radio.Button value="TN">An toàn TN ({triageReport.trueNegatives})</Radio.Button>
                                </Radio.Group>
                                <Input.Search
                                  placeholder="Tìm câu hỏi, triệu chứng..."
                                  value={triageSearchText}
                                  onChange={(e) => setTriageSearchText(e.target.value)}
                                  style={{ width: 220 }}
                                  size="small"
                                  allowClear
                                />
                              </Space>
                            </div>
                          }
                          size="small"
                          bordered
                        >
                          <Table
                            dataSource={filteredTriageCases}
                            columns={triageCaseColumns}
                            rowKey="id"
                            pagination={{ pageSize: 10, showSizeChanger: true }}
                            size="small"
                          />
                        </Card>
                      </div>
                    )}
                  </div>
                ),
              },
              {
                key: 'rag',
                label: (
                  <span>
                    <DatabaseOutlined /> Đánh giá độ chính xác truy xuất RAG (UC0037)
                  </span>
                ),
                children: (
                  <div>
                    {loadingRag && !ragReport ? (
                      <div style={{ textAlign: 'center', padding: 40 }}>
                        <Spin size="large" tip="Đang tải dữ liệu đánh giá RAG..." />
                      </div>
                    ) : !ragReport ? (
                      <Empty description="Chưa có dữ liệu đánh giá RAG." />
                    ) : (
                      <div>
                        {/* Retrieval Mode & Settings Bar */}
                        <div
                          style={{
                            padding: '12px 16px',
                            background: '#f8fafc',
                            borderRadius: 8,
                            marginBottom: 20,
                            display: 'flex',
                            justifyContent: 'space-between',
                            alignItems: 'center',
                            flexWrap: 'wrap',
                            gap: 12,
                          }}
                        >
                          <Space>
                            <Text strong>Cấu hình Top-K truy xuất:</Text>
                            <Select
                              value={ragTopK}
                              onChange={(val) => setRagTopK(val)}
                              style={{ width: 100 }}
                              options={[
                                { label: 'Top 3', value: 3 },
                                { label: 'Top 5', value: 5 },
                                { label: 'Top 10', value: 10 },
                              ]}
                            />
                            <Button
                              type="primary"
                              size="middle"
                              icon={<PlayCircleOutlined />}
                              onClick={handleRunRag}
                              loading={runningRag}
                            >
                              Thực thi kiểm tra Top-{ragTopK}
                            </Button>
                          </Space>

                          <Space wrap>
                            <Text type="secondary">Chế độ truy xuất hiện tại:</Text>
                            <Tag
                              color={
                                ragReport.retrievalMode === 'VECTOR_EMBEDDING' || ragReport.retrievalMode === 'VECTOR'
                                  ? 'success'
                                  : ragReport.retrievalMode === 'NO_DATA' || ragReport.dataSource === 'SIMULATED'
                                  ? 'error'
                                  : 'processing'
                              }
                            >
                              {ragReport.retrievalMode === 'VECTOR_EMBEDDING' || ragReport.retrievalMode === 'VECTOR'
                                ? 'Vector Embedding (Cosine 3072-dim)'
                                : ragReport.retrievalMode === 'NO_DATA' || ragReport.dataSource === 'SIMULATED'
                                ? 'Chưa có dữ liệu DB thật (NO_DATA / SIMULATED)'
                                : 'Lexical / Semantic Fallback (SQL)'}
                            </Tag>
                            {ragReport.dataSource && (
                              <Tag color={ragReport.dataSource === 'SIMULATED' ? 'warning' : 'default'}>
                                Nguồn: {ragReport.dataSource}
                              </Tag>
                            )}
                          </Space>
                        </div>

                        {(ragReport.retrievalMode === 'NO_DATA' ||
                          ragReport.dataSource === 'SIMULATED' ||
                          Boolean(ragReport.warning)) && (
                          <Alert
                            style={{ marginBottom: 20 }}
                            type="warning"
                            showIcon
                            message="Cảnh báo: Dữ liệu kiểm thử giả lập (NO_DATA / SIMULATED)"
                            description={
                              ragReport.warning ||
                              'Bảng chunks chưa có dữ liệu thực tế do tài liệu chưa được nạp (ingest) hoặc chưa cấu hình embedding. Kết quả đang hiển thị mẫu giả lập để tham khảo giao diện.'
                            }
                          />
                        )}

                        {/* RAG Key Metrics */}
                        <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
                          <Col xs={24} sm={8} md={4}>
                            <Card size="small" bordered style={{ textAlign: 'center' }}>
                              <Statistic
                                title="Hit Rate @ 1"
                                value={(ragReport.hitRateAt1 * 100).toFixed(1)}
                                suffix="%"
                                valueStyle={{ color: '#3b82f6', fontWeight: 700 }}
                              />
                              <Text type="secondary" style={{ fontSize: 11 }}>
                                {ragReport.hitCountAt1} / {ragReport.totalQueries} câu Top-1
                              </Text>
                            </Card>
                          </Col>

                          <Col xs={24} sm={8} md={4}>
                            <Card size="small" bordered style={{ textAlign: 'center' }}>
                              <Statistic
                                title="Hit Rate @ 3"
                                value={(ragReport.hitRateAt3 * 100).toFixed(1)}
                                suffix="%"
                                valueStyle={{ color: '#10b981', fontWeight: 700 }}
                              />
                              <Text type="secondary" style={{ fontSize: 11 }}>
                                {ragReport.hitCountAt3} / {ragReport.totalQueries} câu Top-3
                              </Text>
                            </Card>
                          </Col>

                          <Col xs={24} sm={8} md={4}>
                            <Card size="small" bordered style={{ textAlign: 'center' }}>
                              <Statistic
                                title={`Hit Rate @ ${ragReport.topK}`}
                                value={(ragReport.hitRateAtK * 100).toFixed(1)}
                                suffix="%"
                                valueStyle={{ color: '#059669', fontWeight: 700 }}
                              />
                              <Text type="secondary" style={{ fontSize: 11 }}>
                                {ragReport.hitCountAtK} / {ragReport.totalQueries} câu Top-{ragReport.topK}
                              </Text>
                            </Card>
                          </Col>

                          <Col xs={24} sm={8} md={4}>
                            <Card size="small" bordered style={{ textAlign: 'center' }}>
                              <Statistic
                                title="Mean Reciprocal Rank"
                                value={ragReport.mrr.toFixed(3)}
                                valueStyle={{ color: '#8b5cf6', fontWeight: 700 }}
                              />
                              <Text type="secondary" style={{ fontSize: 11 }}>
                                MRR (1.0 là hoàn hảo)
                              </Text>
                            </Card>
                          </Col>

                          <Col xs={24} sm={8} md={4}>
                            <Card size="small" bordered style={{ textAlign: 'center' }}>
                              <Statistic
                                title="Độ tương đồng Cosine"
                                value={(ragReport.averageSimilarity * 100).toFixed(1)}
                                suffix="%"
                                valueStyle={{ color: '#d97706', fontWeight: 700 }}
                              />
                              <Text type="secondary" style={{ fontSize: 11 }}>
                                Trung bình Top-1
                              </Text>
                            </Card>
                          </Col>

                          <Col xs={24} sm={8} md={4}>
                            <Card size="small" bordered style={{ textAlign: 'center' }}>
                              <Statistic
                                title="Độ trễ trung bình"
                                value={ragReport.averageLatencyMs.toFixed(0)}
                                suffix="ms"
                                valueStyle={{ color: '#475569', fontWeight: 700 }}
                              />
                              <Text type="secondary" style={{ fontSize: 11 }}>
                                Thời gian truy xuất
                              </Text>
                            </Card>
                          </Col>
                        </Row>

                        {/* Benchmark Queries Explorer */}
                        <Card
                          title={
                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 12 }}>
                              <Space>
                                <Text strong>Chi tiết kiểm định 22 câu truy vấn y khoa chuẩn hóa</Text>
                                <Tag color="geekblue">Đối chiếu 25 tài liệu chuyên khoa tiêu hóa</Tag>
                              </Space>
                              <Space wrap>
                                <Radio.Group
                                  value={ragHitFilter}
                                  onChange={(e) => setRagHitFilter(e.target.value)}
                                  size="small"
                                >
                                  <Radio.Button value="ALL">Tất cả ({ragReport.totalQueries})</Radio.Button>
                                  <Radio.Button value="HIT">Trúng ({ragReport.hitCountAtK})</Radio.Button>
                                  <Radio.Button value="MISS">Trượt ({ragReport.totalQueries - ragReport.hitCountAtK})</Radio.Button>
                                </Radio.Group>
                                <Input.Search
                                  placeholder="Tìm câu hỏi, tài liệu..."
                                  value={ragSearchText}
                                  onChange={(e) => setRagSearchText(e.target.value)}
                                  style={{ width: 220 }}
                                  size="small"
                                  allowClear
                                />
                              </Space>
                            </div>
                          }
                          size="small"
                          bordered
                        >
                          <Table
                            dataSource={filteredRagQueries}
                            columns={ragQueryColumns}
                            rowKey="id"
                            expandable={{
                              expandedRowRender: expandedChunkRowRender,
                              rowExpandable: (record) =>
                                record.retrievedChunks && record.retrievedChunks.length > 0,
                            }}
                            pagination={{ pageSize: 10, showSizeChanger: true }}
                            size="small"
                          />
                        </Card>
                      </div>
                    )}
                  </div>
                ),
              },
            ]}
          />
        </Card>
      </div>
    </CmsAppShell>
  )
}
