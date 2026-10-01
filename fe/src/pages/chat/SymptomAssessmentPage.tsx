import { ArrowLeftOutlined, CheckCircleOutlined, ExclamationCircleOutlined, HeartOutlined } from '@ant-design/icons'
import { Alert, Button, Card, Checkbox, Result, Steps, Tag, Typography } from 'antd'
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  assessSymptoms,
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
  { value: 'MILD', label: 'Nhẹ', description: 'Khó chịu nhưng vẫn sinh hoạt gần như bình thường' },
  { value: 'MODERATE', label: 'Trung bình', description: 'Ảnh hưởng một phần đến công việc hoặc sinh hoạt' },
  { value: 'SEVERE', label: 'Nặng', description: 'Khó thực hiện các hoạt động thường ngày' },
]

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
  { value: 'DIABETES_WITH_VOMITING', label: 'Đang mắc tiểu đường và bị nôn' },
  { value: 'SEVERE_DEHYDRATION', label: 'Tiểu rất ít kèm lả, lơ mơ hoặc khó đánh thức' },
]

const clinicianReviewWarningOptions: { value: WarningSign; label: string }[] = [
  { value: 'HIGH_FEVER_WITH_ABDOMINAL_PAIN', label: 'Sốt cao kèm đau bụng' },
  { value: 'PAIN_RADIATING_TO_BACK_OR_SHOULDER', label: 'Đau bụng lan ra lưng hoặc vai' },
  { value: 'JAUNDICE_WITH_ABDOMINAL_PAIN', label: 'Vàng da/vàng mắt kèm đau bụng' },
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

function getActivityImpact(severity: Exclude<SeverityLevel, 'UNDETERMINED'>) {
  if (severity === 'MILD') return 'NONE' as const
  if (severity === 'MODERATE') return 'SOME_LIMITATION' as const
  return 'PREVENTS_NORMAL_ACTIVITY' as const
}

function getAdvice(result: SymptomAssessmentResponse) {
  if (result.emergency) {
    return {
      type: 'error' as const,
      title: 'Cần được trợ giúp y tế khẩn cấp',
      description: 'Bạn đã chọn ít nhất một dấu hiệu khẩn cấp. Hãy gọi 115 hoặc đến cơ sở y tế gần nhất ngay; đừng chờ chatbot đánh giá thêm.',
    }
  }
  if (result.requiresClinicianReview) {
    return {
      type: 'warning' as const,
      title: 'Nên trao đổi với nhân viên y tế',
      description: 'Một số dấu hiệu hoặc thông tin bạn cung cấp cần được xem xét trực tiếp. Công cụ không thể xác định nguyên nhân hay thay thế bác sĩ.',
    }
  }
  if (result.severityLevel === 'SEVERE') {
    return {
      type: 'warning' as const,
      title: 'Triệu chứng đang ảnh hưởng nhiều đến sinh hoạt',
      description: 'Bạn tự đánh giá triệu chứng ở mức nặng. Hãy liên hệ nhân viên y tế để được tư vấn trực tiếp, đặc biệt nếu tình trạng tiếp tục nặng lên.',
    }
  }
  if (result.severityLevel === 'MODERATE') {
    return {
      type: 'info' as const,
      title: 'Theo dõi sát diễn tiến',
      description: 'Nếu triệu chứng kéo dài, nặng lên hoặc tiếp tục ảnh hưởng sinh hoạt, hãy liên hệ nhân viên y tế để được tư vấn.',
    }
  }
  if (result.severityLevel === 'UNDETERMINED') {
    return {
      type: 'info' as const,
      title: 'Chưa đủ thông tin để đánh giá',
      description: 'Hãy trao đổi với nhân viên y tế để được hướng dẫn phù hợp với tình trạng của bạn.',
    }
  }
  return {
    type: 'success' as const,
    title: 'Tiếp tục theo dõi triệu chứng',
    description: 'Ghi nhận nếu triệu chứng thay đổi. Nếu kéo dài, nặng lên hoặc khiến bạn lo lắng, hãy liên hệ nhân viên y tế.',
  }
}

export default function SymptomAssessmentPage() {
  const navigate = useNavigate()
  const [step, setStep] = useState(0)
  const [answers, setAnswers] = useState<Partial<SymptomAssessmentRequest>>(DEFAULT_ANSWERS)
  const [result, setResult] = useState<SymptomAssessmentResponse | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  function update<K extends keyof SymptomAssessmentRequest>(key: K, value: SymptomAssessmentRequest[K]) {
    setAnswers((current) => ({ ...current, [key]: value }))
    setError(null)
  }

  function updateWarningGroup(values: WarningSign[], groupOptions: { value: WarningSign; label: string }[]) {
    const groupSigns = new Set(groupOptions.map((option) => option.value))
    const otherGroupSigns = (answers.warningSigns ?? []).filter((sign) => !groupSigns.has(sign))
    update('warningSigns', [...otherGroupSigns, ...values])
  }

  function next() {
    if (step === 0 && (!answers.primarySymptom || !answers.duration || !answers.patientGroup)) {
      setError('Vui lòng chọn triệu chứng, thời gian kéo dài và đối tượng được đánh giá.')
      return
    }
    if (step === 1 && (!answers.reportedSeverity || !answers.progression)) {
      setError('Vui lòng chọn mức độ ảnh hưởng và diễn tiến của triệu chứng.')
      return
    }
    setError(null)
    setStep((current) => current + 1)
  }

  async function submit() {
    if (!answers.primarySymptom || !answers.duration || !answers.reportedSeverity || !answers.progression || !answers.patientGroup) {
      setError('Thiếu thông tin bắt buộc. Hãy quay lại kiểm tra các câu trả lời.')
      return
    }

    setSubmitting(true)
    setError(null)
    try {
      const assessment = await assessSymptoms({
        primarySymptom: answers.primarySymptom,
        duration: answers.duration,
        reportedSeverity: answers.reportedSeverity,
        activityImpact: getActivityImpact(answers.reportedSeverity),
        progression: answers.progression,
        patientGroup: answers.patientGroup,
        warningSigns: answers.warningSigns ?? [],
      })
      setResult(assessment)
    } catch {
      setError('Không thể đánh giá lúc này. Vui lòng thử lại hoặc liên hệ nhân viên y tế nếu bạn lo lắng.')
    } finally {
      setSubmitting(false)
    }
  }

  function restart() {
    setStep(0)
    setAnswers(DEFAULT_ANSWERS)
    setResult(null)
    setError(null)
  }

  const warningSet = new Set(answers.warningSigns ?? [])
  const emergencySelected = emergencyWarningOptions.some((option) => warningSet.has(option.value))
  const advice = result ? getAdvice(result) : null
  const titleForSeverity = answers.reportedSeverity ? severityLabels[answers.reportedSeverity] : 'Chưa chọn'

  return (
    <AppShell fixedViewport>
      <div className="mx-auto flex h-full min-h-0 max-w-4xl flex-col overflow-y-auto overscroll-contain pr-1">
        <div className="mb-5 flex shrink-0 items-center justify-between gap-4">
          <div>
            <Text type="secondary">Sàng lọc triệu chứng</Text>
            <Title level={2} className="mb-1! mt-1!">Đánh giá triệu chứng</Title>
            <Text type="secondary">Trả lời các câu hỏi để nhận hướng dẫn tham khảo.</Text>
          </div>
          <Button icon={<ArrowLeftOutlined />} onClick={() => navigate(-1)}>Quay lại tư vấn</Button>
        </div>

        <Alert
          className="mb-5 shrink-0"
          type="info"
          showIcon
          message="Công cụ sàng lọc tham khảo, không chẩn đoán bệnh và không thay thế bác sĩ."
        />

        {!result && (
          <Steps
            className="mb-5 shrink-0"
            current={step}
            responsive
            items={[{ title: 'Triệu chứng' }, { title: 'Mức độ' }, { title: 'Dấu hiệu cảnh báo' }]}
          />
        )}

        {error && <Alert type="error" showIcon message={error} className="mb-4 shrink-0" />}

        {result && advice ? (
          <div className="space-y-5 pb-6">
            <Card className="rounded-2xl border-black/5 shadow-sm">
              <Result
                status={result.emergency ? 'error' : result.severityLevel === 'UNDETERMINED' ? 'info' : result.requiresClinicianReview || result.severityLevel === 'SEVERE' ? 'warning' : 'success'}
                icon={result.emergency ? <ExclamationCircleOutlined /> : <CheckCircleOutlined />}
                title={<span className="flex flex-wrap items-center justify-center gap-2">Mức đánh giá: <Tag color={severityColors[result.severityLevel]}>{severityLabels[result.severityLevel]}</Tag></span>}
                subTitle="Kết quả dựa trên câu trả lời bạn cung cấp, không phải chẩn đoán."
              />
              <Alert type={advice.type} showIcon message={advice.title} description={advice.description} />
              {result.emergency && <Alert className="mt-3" type="error" showIcon message="Nếu dấu hiệu đang xảy ra, hãy gọi 115 hoặc đến cơ sở y tế gần nhất ngay." />}
            </Card>

            <Card title="Tóm tắt câu trả lời" className="rounded-2xl border-black/5 shadow-sm">
              <dl className="grid gap-4 sm:grid-cols-2">
                <div><dt className="text-xs text-slate-500">Triệu chứng chính</dt><dd className="mb-0 mt-1 font-medium text-slate-800">{symptomLabel(answers.primarySymptom)}</dd></div>
                <div><dt className="text-xs text-slate-500">Thời gian</dt><dd className="mb-0 mt-1 font-medium text-slate-800">{durationLabel(answers.duration)}</dd></div>
                <div><dt className="text-xs text-slate-500">Đối tượng</dt><dd className="mb-0 mt-1 font-medium text-slate-800">{groupLabel(answers.patientGroup)}</dd></div>
                <div><dt className="text-xs text-slate-500">Mức độ tự đánh giá</dt><dd className="mb-0 mt-1 font-medium text-slate-800">{titleForSeverity}</dd></div>
                <div><dt className="text-xs text-slate-500">Diễn tiến</dt><dd className="mb-0 mt-1 font-medium text-slate-800">{progressionLabel(answers.progression)}</dd></div>
                <div className="sm:col-span-2">
                  <dt className="text-xs text-slate-500">Dấu hiệu đã chọn</dt>
                  <dd className="mb-0 mt-1 text-slate-800">
                    {warningSet.size ? [...warningSet].map(warningLabel).join('; ') : 'Không chọn dấu hiệu cảnh báo nào'}
                  </dd>
                </div>
              </dl>
              <p className="mb-0 mt-4 border-t border-slate-100 pt-3 text-xs leading-5 text-slate-500">
                Mức đánh giá lấy mức độ bạn tự khai làm cơ sở; dấu hiệu cảnh báo, mức ảnh hưởng sinh hoạt và diễn tiến có thể làm thay đổi kết quả. Thời gian và triệu chứng chính được ghi nhận để cung cấp ngữ cảnh.
              </p>
            </Card>

            <div className="flex flex-wrap justify-end gap-3">
              <Button onClick={() => navigate(-1)}>Quay lại tư vấn</Button>
              <Button type="primary" icon={<HeartOutlined />} onClick={restart}>Đánh giá mới</Button>
            </div>
          </div>
        ) : (
          <Card className="rounded-2xl border-black/5 shadow-sm">
            {step === 0 && (
              <div className="space-y-7">
                <section>
                  <Title level={4} className="mb-1!">1. Triệu chứng chính</Title>
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
                          className={`rounded-xl border p-4 text-left transition-colors ${selected ? 'border-teal-600 bg-teal-50 ring-1 ring-teal-600' : 'border-slate-200 bg-white hover:border-teal-300 hover:bg-slate-50'}`}
                        >
                          <span className="block font-medium text-slate-800">{option.label}</span>
                          <span className="mt-1 block text-sm text-slate-500">{option.description}</span>
                        </button>
                      )
                    })}
                  </div>
                </section>

                <section>
                  <Title level={4} className="mb-1!">2. Triệu chứng kéo dài bao lâu?</Title>
                  <Text type="secondary">Chọn khoảng thời gian gần đúng; đây là thông tin bổ sung cho kết quả.</Text>
                  <div className="mt-3 flex flex-wrap gap-2">
                    {durationOptions.map((option) => (
                      <Button key={option.value} type={answers.duration === option.value ? 'primary' : 'default'} onClick={() => update('duration', option.value)} aria-pressed={answers.duration === option.value}>
                        {option.label}
                      </Button>
                    ))}
                  </div>
                </section>

                <section>
                  <Title level={4} className="mb-1!">3. Bạn đang đánh giá cho ai?</Title>
                  <div className="mt-3 grid gap-2 sm:grid-cols-2">
                    {patientGroupOptions.map((option) => (
                      <Button key={option.value} block type={answers.patientGroup === option.value ? 'primary' : 'default'} onClick={() => update('patientGroup', option.value)} aria-pressed={answers.patientGroup === option.value}>
                        {option.label}
                      </Button>
                    ))}
                  </div>
                </section>
              </div>
            )}

            {step === 1 && (
              <div className="space-y-7">
                <section>
                  <Title level={4} className="mb-1!">Mức độ ảnh hưởng hiện tại</Title>
                  <Text type="secondary">Chọn một mức gần nhất với cảm nhận của bạn. Mô tả bên dưới giúp phân biệt các mức.</Text>
                  <div className="mt-4 grid gap-3 sm:grid-cols-3">
                    {severityOptions.map((option) => {
                      const selected = answers.reportedSeverity === option.value
                      return (
                        <button
                          key={option.value}
                          type="button"
                          aria-pressed={selected}
                          onClick={() => update('reportedSeverity', option.value)}
                          className={`min-h-28 rounded-xl border p-4 text-left transition-colors ${selected ? 'border-teal-600 bg-teal-50 ring-1 ring-teal-600' : 'border-slate-200 bg-white hover:border-teal-300 hover:bg-slate-50'}`}
                        >
                          <span className="block font-semibold text-slate-800">{option.label}</span>
                          <span className="mt-2 block text-sm leading-5 text-slate-500">{option.description}</span>
                        </button>
                      )
                    })}
                  </div>
                </section>

                <section>
                  <Title level={4} className="mb-1!">Diễn tiến trong thời gian gần đây</Title>
                  <Text type="secondary">Chọn tình trạng hiện đang thay đổi theo hướng nào.</Text>
                  <div className="mt-3 flex flex-wrap gap-2">
                    {progressionOptions.map((option) => (
                      <Button key={option.value} type={answers.progression === option.value ? 'primary' : 'default'} onClick={() => update('progression', option.value)} aria-pressed={answers.progression === option.value}>
                        {option.label}
                      </Button>
                    ))}
                  </div>
                </section>
              </div>
            )}

            {step === 2 && (
              <div className="space-y-5">
                <div>
                  <Title level={4} className="mb-1!">Bạn có đang gặp dấu hiệu nào dưới đây không?</Title>
                  <Text type="secondary">Chọn tất cả dấu hiệu đang có. Nếu không có, cứ để trống và tiếp tục.</Text>
                </div>

                <section className="rounded-xl border border-red-200 bg-red-50/60 p-4">
                  <div className="mb-3 flex items-start gap-2 text-red-900">
                    <ExclamationCircleOutlined className="mt-1" />
                    <div>
                      <h3 className="m-0 font-semibold">Dấu hiệu cần trợ giúp khẩn cấp</h3>
                      <p className="mb-0 mt-1 text-sm leading-5 text-red-800">Nếu đang gặp một trong các dấu hiệu này, hãy gọi 115 hoặc đến cơ sở y tế gần nhất ngay.</p>
                    </div>
                  </div>
                  <Checkbox.Group
                    value={answers.warningSigns}
                    onChange={(values) => updateWarningGroup(values as WarningSign[], emergencyWarningOptions)}
                    className="grid w-full gap-2 sm:grid-cols-2"
                  >
                    {emergencyWarningOptions.map((option) => (
                      <Checkbox key={option.value} value={option.value} className="!m-0 rounded-lg border border-red-100 bg-white px-3 py-2.5 leading-5">
                        {option.label}
                      </Checkbox>
                    ))}
                  </Checkbox.Group>
                </section>

                <section className="rounded-xl border border-amber-200 bg-amber-50/60 p-4">
                  <div className="mb-3 text-amber-950">
                    <h3 className="m-0 font-semibold">Dấu hiệu nên được nhân viên y tế xem xét</h3>
                    <p className="mb-0 mt-1 text-sm leading-5 text-amber-900">Các dấu hiệu này sẽ được ghi nhận trong kết quả để bạn trao đổi với nhân viên y tế.</p>
                  </div>
                  <Checkbox.Group
                    value={answers.warningSigns}
                    onChange={(values) => updateWarningGroup(values as WarningSign[], clinicianReviewWarningOptions)}
                    className="grid w-full gap-2 sm:grid-cols-2"
                  >
                    {clinicianReviewWarningOptions.map((option) => (
                      <Checkbox key={option.value} value={option.value} className="!m-0 rounded-lg border border-amber-100 bg-white px-3 py-2.5 leading-5">
                        {option.label}
                      </Checkbox>
                    ))}
                  </Checkbox.Group>
                </section>

                {emergencySelected && <Alert type="error" showIcon message="Bạn đã chọn dấu hiệu cần trợ giúp khẩn cấp. Hãy ưu tiên gọi 115 hoặc đến cơ sở y tế gần nhất." />}
              </div>
            )}

            <div className="mt-7 flex flex-wrap items-center justify-between gap-3 border-t border-slate-100 pt-5">
              <Button disabled={step === 0 || submitting} onClick={() => { setError(null); setStep((current) => current - 1) }}>Quay lại</Button>
              <Text type="secondary">Bước {step + 1} / 3</Text>
              {step < 2 ? (
                <Button type="primary" onClick={next}>Tiếp tục</Button>
              ) : (
                <Button type="primary" loading={submitting} onClick={() => void submit()}>Xem kết quả</Button>
              )}
            </div>
          </Card>
        )}
      </div>
    </AppShell>
  )
}
