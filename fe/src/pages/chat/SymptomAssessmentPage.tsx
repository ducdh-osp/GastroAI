import { ArrowLeftOutlined, CheckCircleOutlined, ExclamationCircleOutlined, HeartOutlined } from '@ant-design/icons'
import { Alert, Button, Card, Checkbox, Input, Result, Steps, Tag, Typography } from 'antd'
import { useState, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  assessSymptoms,
  type CareRecommendation,
  type PatientGroup,
  type PrimarySymptom,
  type Progression,
  type SeverityLevel,
  type SymptomAssessmentRequest,
  type SymptomDuration,
  type SymptomAssessmentResponse,
  type WarningSign,
} from '../../api/symptomAssessment'
import { AppShell } from '../../components/layout/AppShell'

const { Text, Title } = Typography

const DEFAULT_ANSWERS: Partial<SymptomAssessmentRequest> = { warningSigns: [] }

const symptomOptions: { value: PrimarySymptom; label: string; description: string }[] = [
  { value: 'ABDOMINAL_PAIN', label: 'Đau bụng', description: 'Đau hoặc co thắt ở vùng bụng' },
  { value: 'DIARRHEA', label: 'Tiêu chảy', description: 'Đi ngoài phân lỏng nhiều lần' },
  { value: 'CONSTIPATION', label: 'Táo bón', description: 'Khó đi ngoài hoặc đi ít hơn thường lệ' },
  { value: 'NAUSEA', label: 'Buồn nôn', description: 'Cảm giác muốn nôn' },
  { value: 'VOMITING', label: 'Nôn', description: 'Nôn hoặc không giữ được thức ăn/nước' },
  { value: 'HEARTBURN', label: 'Ợ nóng / trào ngược', description: 'Nóng rát hoặc dịch trào lên họng' },
  { value: 'BLOATING', label: 'Đầy bụng', description: 'Chướng bụng hoặc nhiều hơi' },
  { value: 'OTHER', label: 'Triệu chứng khác', description: 'Triệu chứng tiêu hóa chưa có trong danh sách' },
]

const durationOptions: { value: SymptomDuration; label: string }[] = [
  { value: 'LESS_THAN_24_HOURS', label: 'Dưới 24 giờ' },
  { value: 'ONE_TO_THREE_DAYS', label: '1–3 ngày' },
  { value: 'MORE_THAN_THREE_DAYS', label: 'Trên 3 ngày' },
  { value: 'RECURRING', label: 'Tái diễn nhiều lần' },
  { value: 'UNSURE', label: 'Không chắc' },
]

const patientGroupOptions: { value: PatientGroup; label: string }[] = [
  { value: 'ADULT', label: 'Người trưởng thành' },
  { value: 'UNDER_18', label: 'Người dưới 18 tuổi' },
  { value: 'PREGNANT_OR_RECENTLY_POSTPARTUM', label: 'Đang mang thai hoặc mới sinh' },
  { value: 'UNSURE', label: 'Không chắc' },
]

const severityOptions: { value: Exclude<SeverityLevel, 'UNDETERMINED'>; label: string; description: string }[] = [
  { value: 'MILD', label: 'Nhẹ', description: 'Cảm giác triệu chứng ở mức nhẹ' },
  { value: 'MODERATE', label: 'Trung bình', description: 'Cảm giác triệu chứng rõ rệt, mức vừa' },
  { value: 'SEVERE', label: 'Nặng', description: 'Cảm giác triệu chứng rất mạnh' },
]

const activityImpactOptions = [
  { value: 'NONE', label: 'Không ảnh hưởng', description: 'Sinh hoạt như thường ngày' },
  { value: 'SOME_LIMITATION', label: 'Ảnh hưởng một phần', description: 'Phải giảm hoặc điều chỉnh một số hoạt động' },
  { value: 'PREVENTS_NORMAL_ACTIVITY', label: 'Không thể sinh hoạt bình thường', description: 'Khó thực hiện các hoạt động thường ngày' },
] as const

const progressionOptions: { value: Progression; label: string }[] = [
  { value: 'IMPROVING', label: 'Đang giảm' },
  { value: 'STABLE', label: 'Không thay đổi' },
  { value: 'WORSENING', label: 'Đang nặng hơn' },
]

const emergencyWarningOptions: { value: WarningSign; label: string }[] = [
  { value: 'BLOOD_IN_VOMIT', label: 'Nôn ra máu hoặc chất nôn như bã cà phê' },
  { value: 'BLACK_OR_BLOODY_STOOL', label: 'Đi ngoài ra máu hoặc phân đen như hắc ín' },
  { value: 'SUDDEN_SEVERE_ABDOMINAL_PAIN', label: 'Đau bụng xuất hiện đột ngột hoặc dữ dội' },
  { value: 'RIGID_OR_TENDER_ABDOMEN', label: 'Bụng cứng hoặc đau nhiều khi chạm vào' },
  { value: 'UNABLE_TO_PASS_STOOL_OR_GAS', label: 'Không thể đi ngoài hoặc trung tiện' },
  { value: 'UNABLE_TO_URINATE', label: 'Không thể đi tiểu' },
  { value: 'BREATHING_DIFFICULTY_OR_CHEST_PAIN', label: 'Khó thở hoặc đau/tức ngực' },
  { value: 'FAINTING_OR_CONFUSION', label: 'Ngất, lú lẫn hoặc khó đánh thức' },
  { value: 'HIGH_FEVER_WITH_ABDOMINAL_PAIN', label: 'Sốt cao kèm đau bụng' },
  { value: 'PAIN_RADIATING_TO_BACK_OR_SHOULDER', label: 'Đau bụng lan ra lưng hoặc vai' },
  { value: 'DIABETES_WITH_VOMITING', label: 'Đang mắc tiểu đường và bị nôn' },
  { value: 'JAUNDICE_WITH_ABDOMINAL_PAIN', label: 'Vàng da/vàng mắt kèm đau bụng' },
  { value: 'SEVERE_DEHYDRATION', label: 'Tiểu rất ít kèm lả, lơ mơ hoặc khó đánh thức' },
]

const clinicianReviewWarningOptions: { value: WarningSign; label: string }[] = [
  { value: 'UNEXPLAINED_WEIGHT_LOSS', label: 'Sụt cân không rõ nguyên nhân' },
  { value: 'DIFFICULT_OR_PAINFUL_SWALLOWING', label: 'Khó nuốt hoặc đau khi nuốt' },
]

const severityLabels: Record<SeverityLevel, string> = {
  MILD: 'Nhẹ',
  MODERATE: 'Trung bình',
  SEVERE: 'Nặng',
  UNDETERMINED: 'Chưa thể đánh giá',
}

const severityColors: Record<SeverityLevel, string> = {
  MILD: 'green',
  MODERATE: 'orange',
  SEVERE: 'red',
  UNDETERMINED: 'default',
}

const symptomLabel = (value?: PrimarySymptom) => symptomOptions.find((option) => option.value === value)?.label ?? 'Chưa chọn'
const durationLabel = (value?: SymptomDuration) => durationOptions.find((option) => option.value === value)?.label ?? 'Chưa chọn'
const groupLabel = (value?: PatientGroup) => patientGroupOptions.find((option) => option.value === value)?.label ?? 'Chưa chọn'
const progressionLabel = (value?: Progression) => progressionOptions.find((option) => option.value === value)?.label ?? 'Chưa chọn'
const warningLabel = (value: WarningSign) => [...emergencyWarningOptions, ...clinicianReviewWarningOptions].find((option) => option.value === value)?.label ?? value

const activityImpactLabel = (value?: SymptomAssessmentRequest['activityImpact']) => activityImpactOptions.find((option) => option.value === value)?.label ?? 'Chưa chọn'

const reasonExplanations: Record<string, string> = {
  WARNING_SIGNS_PRESENT: 'Có dấu hiệu cần trợ giúp y tế khẩn cấp.',
  UNSUPPORTED_PATIENT_GROUP: 'Công cụ chưa có quy tắc đánh giá cho nhóm đối tượng này.',
  NON_EMERGENCY_WARNING_SIGNS_PRESENT: 'Có dấu hiệu cần được nhân viên y tế xem xét.',
  SELF_REPORTED_SEVERITY: 'Kết quả dựa trên mức độ bạn tự đánh giá.',
  IMPACT_OR_WORSENING_ESCALATION: 'Mức đánh giá được nâng lên vì sinh hoạt bị ảnh hưởng hoặc triệu chứng đang nặng hơn.',
  SEVERE_SELF_REPORTED_SYMPTOMS: 'Bạn tự đánh giá triệu chứng ở mức nặng.',
  NEEDS_MORE_INFORMATION: 'Thông tin hiện có chưa đủ để xác định mức độ.',
  DURATION_UNCLEAR: 'Thời gian kéo dài chưa rõ; nên trao đổi thêm với nhân viên y tế.',
}

function getCareAdvice(careRecommendation?: CareRecommendation) {
  if (careRecommendation === 'EMERGENCY_CARE') {
    return {
      type: 'error' as const,
      title: 'Cần cấp cứu ngay',
      description: 'Hãy gọi 115 hoặc đến cơ sở y tế gần nhất ngay; đừng chờ chatbot đánh giá thêm.',
    }
  }
  if (careRecommendation === 'SEE_DOCTOR') {
    return {
      type: 'warning' as const,
      title: 'Nên gặp bác sĩ',
      description: 'Hãy liên hệ cơ sở y tế để được tư vấn hoặc khám trực tiếp. Công cụ không thể xác định nguyên nhân hay thay thế bác sĩ.',
    }
  }
  if (careRecommendation === 'SELF_CARE_AT_HOME') {
    return {
      type: 'success' as const,
      title: 'Tự chăm sóc tại nhà',
      description: 'Nghỉ ngơi, uống đủ nước và theo dõi diễn tiến. Hãy gặp bác sĩ nếu triệu chứng kéo dài, nặng lên, ảnh hưởng sinh hoạt hoặc khiến bạn lo lắng.',
    }
  }
  return null
}

export default function SymptomAssessmentPage() {
  const navigate = useNavigate()
  const containerRef = useRef<HTMLDivElement>(null)
  const [step, setStep] = useState(0)
  const [answers, setAnswers] = useState<Partial<SymptomAssessmentRequest>>(DEFAULT_ANSWERS)
  const [result, setResult] = useState<SymptomAssessmentResponse | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  function update<K extends keyof SymptomAssessmentRequest>(key: K, value: SymptomAssessmentRequest[K]) {
    setAnswers((current) => ({ ...current, [key]: value }))
    setError(null)
  }

  function showError(msg: string) {
    setError(msg)
    containerRef.current?.scrollTo({ top: 0, behavior: 'smooth' })
  }

  function updateWarningGroup(values: WarningSign[], groupOptions: { value: WarningSign; label: string }[]) {
    const groupSigns = new Set(groupOptions.map((option) => option.value))
    const otherGroupSigns = (answers.warningSigns ?? []).filter((sign) => !groupSigns.has(sign))
    update('warningSigns', [...otherGroupSigns, ...values])
  }

  function next() {
    if (step === 0) {
      if (!answers.primarySymptom) {
        showError('Vui lòng chọn triệu chứng chính ở mục 1.')
        return
      }
      if (answers.primarySymptom === 'OTHER' && !answers.primarySymptomDetail?.trim()) {
        showError('Vui lòng nhập mô tả chi tiết cho triệu chứng khác ở mục 1.')
        return
      }
      if (!answers.duration) {
        showError('Vui lòng chọn thời gian triệu chứng kéo dài ở mục 2.')
        return
      }
      if (!answers.patientGroup) {
        showError('Vui lòng chọn đối tượng được đánh giá ở mục 3.')
        return
      }
    }
    if (step === 1) {
      if (!answers.reportedSeverity) {
        showError('Vui lòng chọn mức độ ảnh hưởng của triệu chứng ở mục 1.')
        return
      }
      if (!answers.progression) {
        showError('Vui lòng chọn diễn tiến triệu chứng gần đây ở mục 2.')
        return
      }
      if (!answers.activityImpact) {
        showError('Vui lòng chọn mức ảnh hưởng sinh hoạt ở mục 3.')
        return
      }
    }
    setError(null)
    setStep((current) => current + 1)
    containerRef.current?.scrollTo({ top: 0, behavior: 'smooth' })
  }

  async function submit() {
    if (!answers.primarySymptom || !answers.duration || !answers.reportedSeverity || !answers.activityImpact || !answers.progression || !answers.patientGroup) {
      showError('Thiếu thông tin bắt buộc. Hãy quay lại kiểm tra các câu trả lời.')
      return
    }

    setSubmitting(true)
    setError(null)
    try {
      const assessment = await assessSymptoms({
        primarySymptom: answers.primarySymptom,
        primarySymptomDetail: answers.primarySymptom === 'OTHER' ? answers.primarySymptomDetail?.trim() : undefined,
        duration: answers.duration,
        reportedSeverity: answers.reportedSeverity,
        activityImpact: answers.activityImpact,
        progression: answers.progression,
        patientGroup: answers.patientGroup,
        warningSigns: answers.warningSigns ?? [],
      })
      setResult(assessment)
      containerRef.current?.scrollTo({ top: 0, behavior: 'smooth' })
    } catch {
      showError('Không thể đánh giá lúc này. Vui lòng thử lại hoặc liên hệ nhân viên y tế nếu bạn lo lắng.')
    } finally {
      setSubmitting(false)
    }
  }

  function restart() {
    setStep(0)
    setAnswers(DEFAULT_ANSWERS)
    setResult(null)
    setError(null)
    containerRef.current?.scrollTo({ top: 0, behavior: 'smooth' })
  }

  const warningSet = new Set(answers.warningSigns ?? [])
  const emergencySelected = emergencyWarningOptions.some((option) => warningSet.has(option.value))
  const careAdvice = result
    ? (getCareAdvice(result.careRecommendation) ?? {
        type: 'info' as const,
        title: 'Kết quả đánh giá',
        description: 'Hãy liên hệ cơ sở y tế để được tư vấn chi tiết hơn nếu triệu chứng tiếp diễn.',
      })
    : null
  const titleForSeverity = answers.reportedSeverity ? severityLabels[answers.reportedSeverity] : 'Chưa chọn'

  return (
    <AppShell fixedViewport>
      <div ref={containerRef} className="mx-auto flex h-full min-h-0 max-w-4xl flex-col overflow-y-auto overscroll-contain pr-1">
        <div className="mb-6 flex shrink-0 items-center justify-between gap-4">
          <div>
            <Text type="secondary">Sàng lọc triệu chứng</Text>
            <Title level={2} className="mb-1! mt-1!">Đánh giá triệu chứng</Title>
            <Text type="secondary">Trả lời các câu hỏi để nhận hướng dẫn tham khảo.</Text>
          </div>
          <Button icon={<ArrowLeftOutlined />} size="middle" className="!rounded-lg" onClick={() => navigate(-1)}>Quay lại tư vấn</Button>
        </div>

        <Alert
          className="mb-6 shrink-0 rounded-xl"
          type="info"
          showIcon
          message="Công cụ sàng lọc tham khảo, không chẩn đoán bệnh và không thay thế bác sĩ."
        />

        {!result && (
          <div className="my-6 shrink-0 px-1 py-1">
            <Steps
              current={step}
              responsive
              items={[{ title: 'Triệu chứng' }, { title: 'Mức độ' }, { title: 'Dấu hiệu cảnh báo' }]}
            />
          </div>
        )}

        {error && (
          <Alert
            type="error"
            showIcon
            message={error}
            className="mb-6 mt-2 shrink-0 rounded-xl shadow-xs"
          />
        )}

        {result && careAdvice ? (
          <div className="space-y-5 pb-6">
            <Card className="rounded-2xl border-black/5 shadow-sm">
              <Result
                status={result.careRecommendation === 'EMERGENCY_CARE' ? 'error' : result.careRecommendation === 'SEE_DOCTOR' ? 'warning' : 'success'}
                icon={result.emergency ? <ExclamationCircleOutlined /> : <CheckCircleOutlined />}
                title={<span className="flex flex-wrap items-center justify-center gap-2">Mức đánh giá: <Tag color={severityColors[result.severityLevel]}>{severityLabels[result.severityLevel]}</Tag></span>}
                subTitle="Kết quả dựa trên câu trả lời bạn cung cấp, không phải chẩn đoán."
              />
              <Alert type={careAdvice.type} showIcon message={careAdvice.title} description={careAdvice.description} />
              {result.emergency && <Alert className="mt-3" type="error" showIcon message="Nếu dấu hiệu đang xảy ra, hãy gọi 115 hoặc đến cơ sở y tế gần nhất ngay." />}
            </Card>

            <Card title="Tóm tắt câu trả lời" className="rounded-2xl border-black/5 shadow-sm">
              <dl className="grid gap-4 sm:grid-cols-2">
                <div><dt className="text-xs text-slate-500 dark:text-white">Triệu chứng chính</dt><dd className="mb-0 mt-1 font-medium text-slate-800 dark:text-white">{symptomLabel(answers.primarySymptom)}{answers.primarySymptomDetail ? `: ${answers.primarySymptomDetail}` : ''}</dd></div>
                <div><dt className="text-xs text-slate-500 dark:text-white">Thời gian</dt><dd className="mb-0 mt-1 font-medium text-slate-800 dark:text-white">{durationLabel(answers.duration)}</dd></div>
                <div><dt className="text-xs text-slate-500 dark:text-white">Đối tượng</dt><dd className="mb-0 mt-1 font-medium text-slate-800 dark:text-white">{groupLabel(answers.patientGroup)}</dd></div>
                <div><dt className="text-xs text-slate-500 dark:text-white">Mức độ tự đánh giá</dt><dd className="mb-0 mt-1 font-medium text-slate-800 dark:text-white">{titleForSeverity}</dd></div>
                <div><dt className="text-xs text-slate-500 dark:text-white">Ảnh hưởng sinh hoạt</dt><dd className="mb-0 mt-1 font-medium text-slate-800 dark:text-white">{activityImpactLabel(answers.activityImpact)}</dd></div>
                <div><dt className="text-xs text-slate-500 dark:text-white">Diễn tiến</dt><dd className="mb-0 mt-1 font-medium text-slate-800 dark:text-white">{progressionLabel(answers.progression)}</dd></div>
                <div className="sm:col-span-2">
                  <dt className="text-xs text-slate-500 dark:text-white">Dấu hiệu đã chọn</dt>
                  <dd className="mb-0 mt-1 text-slate-800 dark:text-white">
                    {warningSet.size ? [...warningSet].map(warningLabel).join('; ') : 'Không chọn dấu hiệu cảnh báo nào'}
                  </dd>
                </div>
              </dl>
              {result.reasonCodes.length > 0 && (
                <div className="mt-4 border-t border-slate-100 dark:border-white/10 pt-3">
                  <Text strong>Vì sao có kết quả này</Text>
                  <ul className="mb-0 mt-2 list-disc space-y-1 pl-5 text-sm text-slate-700 dark:text-white">
                    {result.reasonCodes.map((code) => <li key={code}>{reasonExplanations[code] ?? 'Một số câu trả lời đã ảnh hưởng đến kết quả đánh giá.'}</li>)}
                  </ul>
                </div>
              )}
              <p className="mb-0 mt-4 border-t border-slate-100 dark:border-white/10 pt-3 text-xs leading-5 text-slate-500 dark:text-white">
                Mức đánh giá lấy mức độ bạn tự khai làm cơ sở; dấu hiệu cảnh báo, mức ảnh hưởng sinh hoạt và diễn tiến có thể làm thay đổi kết quả. Thời gian và triệu chứng chính được ghi nhận để cung cấp ngữ cảnh.
              </p>
            </Card>

            <div className="flex flex-wrap justify-end gap-3.5 pt-2">
              <Button size="middle" className="!h-10 !rounded-lg !px-5 font-medium" onClick={() => navigate(-1)}>Quay lại</Button>
              <Button size="middle" className="!h-10 !rounded-lg !px-5 font-medium" icon={<HeartOutlined />} onClick={restart}>Đánh giá mới</Button>
              <Button type="primary" size="middle" className="!h-10 !rounded-lg !px-6 font-medium shadow-xs" onClick={() => navigate('/chat')}>Tư vấn thêm với Trợ lý AI</Button>
            </div>
          </div>
        ) : (
          <Card className="rounded-2xl border-black/5 shadow-sm">
            {step === 0 && (
              <div className="space-y-7">
                <section>
                  <div className="flex items-center gap-2.5">
                    <Title level={4} className="mb-1!">1. Triệu chứng chính</Title>
                    {answers.primarySymptom ? <Tag color="success">Đã chọn</Tag> : <Tag color="default">Chưa chọn</Tag>}
                  </div>
                  <Text type="secondary">Chọn triệu chứng khiến bạn muốn được đánh giá nhất.</Text>
                  <div className="mt-4 grid gap-3 sm:grid-cols-2">
                    {symptomOptions.map((option) => {
                      const selected = answers.primarySymptom === option.value
                      return (
                        <button
                          key={option.value}
                          type="button"
                          aria-pressed={selected}
                          onClick={() => update('primarySymptom', option.value)}
                          className={`rounded-xl border p-4 text-left transition-colors ${selected ? 'border-teal-600 bg-teal-50 ring-1 ring-teal-600 dark:bg-teal-900/30' : 'border-slate-200 bg-white hover:border-teal-300 hover:bg-slate-50 dark:border-white/10 dark:bg-neutral-800 dark:hover:bg-neutral-700'}`}
                        >
                          <span className="block font-medium text-slate-800 dark:text-white">{option.label}</span>
                          <span className="mt-1 block text-sm text-slate-500 dark:text-white">{option.description}</span>
                        </button>
                      )
                    })}
                  </div>
                  {answers.primarySymptom === 'OTHER' && (
                    <div className="mt-3">
                      <Text strong>Mô tả triệu chứng khác</Text>
                      <Input.TextArea
                        className="mt-2"
                        value={answers.primarySymptomDetail}
                        maxLength={500}
                        showCount
                        autoSize={{ minRows: 2, maxRows: 4 }}
                        placeholder="Mô tả ngắn triệu chứng bạn đang gặp"
                        onChange={(event) => update('primarySymptomDetail', event.target.value)}
                      />
                      <Text type="secondary">Thông tin này sẽ được lưu cùng kết quả để nhân viên y tế có thể tham khảo.</Text>
                    </div>
                  )}
                </section>

                <section>
                  <div className="flex items-center gap-2.5">
                    <Title level={4} className="mb-1!">2. Triệu chứng kéo dài bao lâu?</Title>
                    {answers.duration ? <Tag color="success">Đã chọn</Tag> : <Tag color="default">Chưa chọn</Tag>}
                  </div>
                  <Text type="secondary">Chọn khoảng thời gian gần đúng; đây là thông tin bổ sung cho kết quả.</Text>
                  <div className="mt-3.5 flex flex-wrap gap-3">
                    {durationOptions.map((option) => (
                      <Button
                        key={option.value}
                        size="middle"
                        className="!h-10 !rounded-lg !px-4.5 font-medium transition-all"
                        type={answers.duration === option.value ? 'primary' : 'default'}
                        onClick={() => update('duration', option.value)}
                        aria-pressed={answers.duration === option.value}
                      >
                        {option.label}
                      </Button>
                    ))}
                  </div>
                </section>

                <section>
                  <div className="flex items-center gap-2.5">
                    <Title level={4} className="mb-1!">3. Bạn đang đánh giá cho ai?</Title>
                    {answers.patientGroup ? <Tag color="success">Đã chọn</Tag> : <Tag color="default">Chưa chọn</Tag>}
                  </div>
                  <Text type="secondary">Chọn nhóm phù hợp nhất với người đang có triệu chứng.</Text>
                  <div className="mt-3.5 grid gap-3 sm:grid-cols-2">
                    {patientGroupOptions.map((option) => (
                      <Button
                        key={option.value}
                        block
                        size="middle"
                        className="!h-11 !rounded-xl !px-4 text-center font-medium transition-all"
                        type={answers.patientGroup === option.value ? 'primary' : 'default'}
                        onClick={() => update('patientGroup', option.value)}
                        aria-pressed={answers.patientGroup === option.value}
                      >
                        {option.label}
                      </Button>
                    ))}
                  </div>
                  <p className="mb-0 mt-3 text-sm leading-5 text-slate-500 dark:text-white">
                    “Không chắc” dùng khi bạn chưa xác định được người có triệu chứng thuộc nhóm nào trong ba nhóm còn lại.
                  </p>
                  {answers.patientGroup === 'UNSURE' && (
                    <Alert
                      className="mt-3.5 rounded-xl"
                      type="info"
                      showIcon
                      message="Chưa thể đánh giá theo nhóm đối tượng"
                      description="Công cụ hiện chưa áp dụng cách phân loại cho trường hợp này và sẽ khuyến nghị bạn trao đổi với nhân viên y tế."
                    />
                  )}
                </section>
              </div>
            )}

            {step === 1 && (
              <div className="space-y-7">
                <section>
                  <div className="flex items-center gap-2.5">
                    <Title level={4} className="mb-1!">1. Mức độ ảnh hưởng hiện tại</Title>
                    {answers.reportedSeverity ? <Tag color="success">Đã chọn</Tag> : <Tag color="default">Chưa chọn</Tag>}
                  </div>
                  <Text type="secondary">Chọn một mức gần nhất với cảm nhận của bạn. Mô tả bên dưới giúp phân biệt các mức.</Text>
                  <div className="mt-4 grid gap-3.5 sm:grid-cols-3">
                    {severityOptions.map((option) => {
                      const selected = answers.reportedSeverity === option.value
                      return (
                        <button
                          key={option.value}
                          type="button"
                          aria-pressed={selected}
                          onClick={() => update('reportedSeverity', option.value)}
                          className={`min-h-28 rounded-xl border p-4 text-left transition-colors ${selected ? 'border-teal-600 bg-teal-50 ring-1 ring-teal-600 dark:bg-teal-900/30' : 'border-slate-200 bg-white hover:border-teal-300 hover:bg-slate-50 dark:border-white/10 dark:bg-neutral-800 dark:hover:bg-neutral-700'}`}
                        >
                          <span className="block font-semibold text-slate-800 dark:text-white">{option.label}</span>
                          <span className="mt-2 block text-sm leading-5 text-slate-500 dark:text-white">{option.description}</span>
                        </button>
                      )
                    })}
                  </div>
                </section>

                <section>
                  <div className="flex items-center gap-2.5">
                    <Title level={4} className="mb-1!">2. Diễn tiến trong thời gian gần đây</Title>
                    {answers.progression ? <Tag color="success">Đã chọn</Tag> : <Tag color="default">Chưa chọn</Tag>}
                  </div>
                  <Text type="secondary">Chọn tình trạng hiện đang thay đổi theo hướng nào.</Text>
                  <div className="mt-3.5 flex flex-wrap gap-3">
                    {progressionOptions.map((option) => (
                      <Button
                        key={option.value}
                        size="middle"
                        className="!h-10 !rounded-lg !px-4.5 font-medium transition-all"
                        type={answers.progression === option.value ? 'primary' : 'default'}
                        onClick={() => update('progression', option.value)}
                        aria-pressed={answers.progression === option.value}
                      >
                        {option.label}
                      </Button>
                    ))}
                  </div>
                </section>
                <section>
                  <div className="flex items-center gap-2.5">
                    <Title level={4} className="mb-1!">3. Mức ảnh hưởng đến sinh hoạt</Title>
                    {answers.activityImpact ? <Tag color="success">Đã chọn</Tag> : <Tag color="default">Chưa chọn</Tag>}
                  </div>
                  <Text type="secondary">Trả lời riêng theo những gì bạn đang trải qua; mức này không tự suy ra từ mức độ triệu chứng.</Text>
                  <div className="mt-3.5 grid gap-3.5 sm:grid-cols-3">
                    {activityImpactOptions.map((option) => (
                      <button key={option.value} type="button" aria-pressed={answers.activityImpact === option.value}
                        onClick={() => update('activityImpact', option.value)}
                        className={`rounded-xl border p-4 text-left transition-colors ${answers.activityImpact === option.value ? 'border-teal-600 bg-teal-50 ring-1 ring-teal-600 dark:bg-teal-900/30' : 'border-slate-200 bg-white hover:border-teal-300 hover:bg-slate-50 dark:border-white/10 dark:bg-neutral-800 dark:hover:bg-neutral-700'}`}>
                        <span className="block font-semibold text-slate-800 dark:text-white">{option.label}</span>
                        <span className="mt-2 block text-sm leading-5 text-slate-500 dark:text-white">{option.description}</span>
                      </button>
                    ))}
                  </div>
                </section>
              </div>
            )}

            {step === 2 && (
              <div className="space-y-6">
                <div>
                  <Title level={4} className="mb-1!">Bạn có đang gặp dấu hiệu nào dưới đây không?</Title>
                  <Text type="secondary">Chọn tất cả dấu hiệu đang có. Nếu không có, cứ để trống và tiếp tục.</Text>
                </div>

                <section className="rounded-xl border border-red-200 bg-red-50/60 p-4.5">
                  <div className="mb-3.5 flex items-start gap-2 text-red-900">
                    <ExclamationCircleOutlined className="mt-1" />
                    <div>
                      <h3 className="m-0 font-semibold">Dấu hiệu cần trợ giúp khẩn cấp</h3>
                      <p className="mb-0 mt-1 text-sm leading-5 text-red-800">Nếu đang gặp một trong các dấu hiệu này, hãy gọi 115 hoặc đến cơ sở y tế gần nhất ngay.</p>
                    </div>
                  </div>
                  <Checkbox.Group
                    value={answers.warningSigns}
                    onChange={(values) => updateWarningGroup(values as WarningSign[], emergencyWarningOptions)}
                    className="grid w-full gap-3 sm:grid-cols-2"
                  >
                    {emergencyWarningOptions.map((option) => (
                      <Checkbox key={option.value} value={option.value} className="!m-0 rounded-lg border border-red-100 bg-white px-3.5 py-3 leading-5 dark:border-red-900/50 dark:bg-neutral-800">
                        {option.label}
                      </Checkbox>
                    ))}
                  </Checkbox.Group>
                </section>

                <section className="rounded-xl border border-amber-200 bg-amber-50/60 p-4.5">
                  <div className="mb-3.5 text-amber-950">
                    <h3 className="m-0 font-semibold">Dấu hiệu nên được nhân viên y tế xem xét</h3>
                    <p className="mb-0 mt-1 text-sm leading-5 text-amber-900">Các dấu hiệu này sẽ được ghi nhận trong kết quả để bạn trao đổi với nhân viên y tế.</p>
                  </div>
                  <Checkbox.Group
                    value={answers.warningSigns}
                    onChange={(values) => updateWarningGroup(values as WarningSign[], clinicianReviewWarningOptions)}
                    className="grid w-full gap-3 sm:grid-cols-2"
                  >
                    {clinicianReviewWarningOptions.map((option) => (
                      <Checkbox key={option.value} value={option.value} className="!m-0 rounded-lg border border-amber-100 bg-white px-3.5 py-3 leading-5 dark:border-amber-900/50 dark:bg-neutral-800">
                        {option.label}
                      </Checkbox>
                    ))}
                  </Checkbox.Group>
                </section>

                {emergencySelected && <Alert className="rounded-xl" type="error" showIcon message="Bạn đã chọn dấu hiệu cần trợ giúp khẩn cấp. Hãy ưu tiên gọi 115 hoặc đến cơ sở y tế gần nhất." />}
              </div>
            )}

            {error && (
              <div className="mt-5 rounded-xl border border-red-200 bg-red-50/80 p-3 text-sm font-medium text-red-600 dark:border-red-900/60 dark:bg-red-950/40 dark:text-red-300">
                <ExclamationCircleOutlined className="mr-2" />
                {error}
              </div>
            )}

            <div className="mt-6 flex flex-wrap items-center justify-between gap-4 border-t border-slate-100 dark:border-white/10 pt-5">
              <Button size="middle" className="!h-10 !rounded-lg !px-5 font-medium" disabled={step === 0 || submitting} onClick={() => { setError(null); setStep((current) => current - 1) }}>Quay lại</Button>
              <Text type="secondary" className="font-medium">Bước {step + 1} / 3</Text>
              {step < 2 ? (
                <Button type="primary" size="middle" className="!h-10 !rounded-lg !px-6 font-medium shadow-xs" onClick={next}>Tiếp tục</Button>
              ) : (
                <Button type="primary" size="middle" className="!h-10 !rounded-lg !px-6 font-medium shadow-xs" loading={submitting} onClick={() => void submit()}>Xem kết quả</Button>
              )}
            </div>
          </Card>
        )}
      </div>
    </AppShell>
  )
}
