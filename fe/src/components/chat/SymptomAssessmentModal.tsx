import { Alert, Button, Checkbox, Modal, Radio, Result, Select, Steps, Tag } from 'antd'
import { useState } from 'react'
import {
  assessSymptoms,
  type ActivityImpact,
  type PatientGroup,
  type PrimarySymptom,
  type Progression,
  type SeverityLevel,
  type SymptomAssessmentRequest,
  type SymptomDuration,
  type SymptomAssessmentResponse,
  type WarningSign,
} from '../../api/symptomAssessment'

interface SymptomAssessmentModalProps {
  open: boolean
  onClose: () => void
}

const DEFAULT_ANSWERS: Partial<SymptomAssessmentRequest> = { warningSigns: [] }

const symptomOptions: { value: PrimarySymptom; label: string }[] = [
  { value: 'ABDOMINAL_PAIN', label: 'Đau bụng' },
  { value: 'DIARRHEA', label: 'Tiêu chảy' },
  { value: 'CONSTIPATION', label: 'Táo bón' },
  { value: 'NAUSEA', label: 'Buồn nôn' },
  { value: 'VOMITING', label: 'Nôn' },
  { value: 'HEARTBURN', label: 'Ợ nóng / trào ngược' },
  { value: 'BLOATING', label: 'Đầy bụng' },
  { value: 'OTHER', label: 'Triệu chứng tiêu hóa khác' },
]

const warningOptions: { value: WarningSign; label: string }[] = [
  { value: 'BLOOD_IN_VOMIT', label: 'Nôn ra máu hoặc chất nôn như bã cà phê' },
  { value: 'BLACK_OR_BLOODY_STOOL', label: 'Đi ngoài ra máu hoặc phân đen như hắc ín' },
  { value: 'SUDDEN_SEVERE_ABDOMINAL_PAIN', label: 'Đau bụng đột ngột hoặc dữ dội' },
  { value: 'RIGID_OR_TENDER_ABDOMEN', label: 'Bụng cứng hoặc đau nhiều khi chạm vào' },
  { value: 'UNABLE_TO_PASS_STOOL_OR_GAS', label: 'Không thể đi ngoài hoặc trung tiện' },
  { value: 'UNABLE_TO_URINATE', label: 'Không thể đi tiểu' },
  { value: 'BREATHING_DIFFICULTY_OR_CHEST_PAIN', label: 'Khó thở hoặc đau/tức ngực' },
  { value: 'FAINTING_OR_CONFUSION', label: 'Ngất, lú lẫn hoặc khó đánh thức' },
  { value: 'DIABETES_WITH_VOMITING', label: 'Đang mắc tiểu đường và bị nôn' },
  { value: 'SEVERE_DEHYDRATION', label: 'Tiểu rất ít kèm lả, lơ mơ hoặc khó đánh thức' },
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

export function SymptomAssessmentModal({ open, onClose }: SymptomAssessmentModalProps) {
  const [step, setStep] = useState(0)
  const [answers, setAnswers] = useState<Partial<SymptomAssessmentRequest>>(DEFAULT_ANSWERS)
  const [result, setResult] = useState<SymptomAssessmentResponse | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  function update<K extends keyof SymptomAssessmentRequest>(key: K, value: SymptomAssessmentRequest[K]) {
    setAnswers((current) => ({ ...current, [key]: value }))
    setError(null)
  }

  function resetAndClose() {
    setStep(0)
    setAnswers(DEFAULT_ANSWERS)
    setResult(null)
    setError(null)
    onClose()
  }

  async function submit() {
    if (
      !answers.primarySymptom || !answers.duration || !answers.reportedSeverity ||
      !answers.activityImpact || !answers.progression || !answers.patientGroup
    ) {
      setError('Vui lòng hoàn thành các câu hỏi trước khi nhận kết quả.')
      return
    }

    setSubmitting(true)
    setError(null)
    try {
      const assessment = await assessSymptoms({
        primarySymptom: answers.primarySymptom,
        duration: answers.duration,
        reportedSeverity: answers.reportedSeverity,
        activityImpact: answers.activityImpact,
        progression: answers.progression,
        patientGroup: answers.patientGroup,
        warningSigns: answers.warningSigns ?? [],
      })
      setResult(assessment)
      setStep(3)
    } catch {
      setError('Không thể đánh giá lúc này. Vui lòng thử lại hoặc liên hệ nhân viên y tế nếu bạn lo lắng.')
    } finally {
      setSubmitting(false)
    }
  }

  function next() {
    if (step === 0 && (!answers.primarySymptom || !answers.duration || !answers.patientGroup)) {
      setError('Vui lòng trả lời các câu hỏi trong bước này.')
      return
    }
    if (step === 1 && (!answers.reportedSeverity || !answers.activityImpact || !answers.progression)) {
      setError('Vui lòng trả lời các câu hỏi trong bước này.')
      return
    }
    setError(null)
    setStep((current) => current + 1)
  }

  const warningSet = new Set(answers.warningSigns ?? [])
  const urgentSignsSelected = warningSet.has('BLOOD_IN_VOMIT')
    || warningSet.has('BLACK_OR_BLOODY_STOOL')
    || warningSet.has('SUDDEN_SEVERE_ABDOMINAL_PAIN')
    || warningSet.has('RIGID_OR_TENDER_ABDOMEN')
    || warningSet.has('UNABLE_TO_PASS_STOOL_OR_GAS')
    || warningSet.has('UNABLE_TO_URINATE')
    || warningSet.has('BREATHING_DIFFICULTY_OR_CHEST_PAIN')
    || warningSet.has('FAINTING_OR_CONFUSION')
    || warningSet.has('DIABETES_WITH_VOMITING')
    || warningSet.has('SEVERE_DEHYDRATION')

  return (
    <Modal
      title="Đánh giá triệu chứng"
      open={open}
      onCancel={submitting ? undefined : resetAndClose}
      closable={!submitting}
      maskClosable={!submitting}
      width={640}
      footer={result ? (
        <Button type="primary" onClick={resetAndClose}>Đóng</Button>
      ) : (
        <div className="flex justify-between">
          <Button disabled={step === 0 || submitting} onClick={() => setStep((current) => current - 1)}>Quay lại</Button>
          {step < 2 ? (
            <Button type="primary" onClick={next}>Tiếp tục</Button>
          ) : (
            <Button type="primary" loading={submitting} onClick={() => void submit()}>Xem đánh giá</Button>
          )}
        </div>
      )}
    >
      <p className="mb-4 text-sm leading-6 text-slate-600">
        Trả lời một số câu hỏi để nhận đánh giá tham khảo. Công cụ không chẩn đoán bệnh và không thay thế bác sĩ.
      </p>

      {!result && <Steps size="small" current={step} items={[{ title: 'Triệu chứng' }, { title: 'Mức độ' }, { title: 'Dấu hiệu cảnh báo' }]} className="mb-6" />}
      {error && <Alert type="error" showIcon message={error} className="mb-4" />}

      {step === 0 && !result && (
        <div className="space-y-5">
          <label className="block text-sm font-medium text-slate-700">Triệu chứng chính</label>
          <Radio.Group
            value={answers.primarySymptom}
            onChange={(event) => update('primarySymptom', event.target.value as PrimarySymptom)}
            className="grid grid-cols-2 gap-2"
          >
            {symptomOptions.map((option) => <Radio.Button key={option.value} value={option.value} className="h-auto! rounded-lg! py-2 text-center">{option.label}</Radio.Button>)}
          </Radio.Group>

          <div>
            <label htmlFor="symptom-duration" className="mb-2 block text-sm font-medium text-slate-700">Triệu chứng đã kéo dài</label>
            <Select
              id="symptom-duration"
              value={answers.duration}
              onChange={(value) => update('duration', value as SymptomDuration)}
              className="w-full"
              placeholder="Chọn khoảng thời gian"
              options={[
                { value: 'LESS_THAN_24_HOURS', label: 'Dưới 24 giờ' },
                { value: 'ONE_TO_THREE_DAYS', label: '1–3 ngày' },
                { value: 'MORE_THAN_THREE_DAYS', label: 'Trên 3 ngày' },
                { value: 'RECURRING', label: 'Tái diễn nhiều lần' },
                { value: 'UNSURE', label: 'Không chắc' },
              ]}
            />
          </div>

          <div>
            <p className="mb-2 text-sm font-medium text-slate-700">Đối tượng được đánh giá</p>
            <Radio.Group
              value={answers.patientGroup}
              onChange={(event) => update('patientGroup', event.target.value as PatientGroup)}
              className="flex flex-col gap-2"
            >
              <Radio value="ADULT">Người trưởng thành</Radio>
              <Radio value="UNDER_18">Người dưới 18 tuổi</Radio>
              <Radio value="PREGNANT_OR_RECENTLY_POSTPARTUM">Đang mang thai hoặc mới sinh</Radio>
              <Radio value="UNSURE">Không chắc</Radio>
            </Radio.Group>
          </div>
        </div>
      )}

      {step === 1 && !result && (
        <div className="space-y-5">
          <div>
            <p className="mb-2 text-sm font-medium text-slate-700">Theo cảm nhận của bạn, mức độ triệu chứng hiện tại là</p>
            <Radio.Group
              value={answers.reportedSeverity}
              onChange={(event) => update('reportedSeverity', event.target.value as SymptomAssessmentRequest['reportedSeverity'])}
              className="flex flex-col gap-2"
            >
              <Radio value="MILD">Nhẹ — vẫn sinh hoạt gần như bình thường</Radio>
              <Radio value="MODERATE">Trung bình — gây khó chịu và ảnh hưởng một phần sinh hoạt</Radio>
              <Radio value="SEVERE">Nặng — ảnh hưởng nhiều hoặc khó tiếp tục sinh hoạt</Radio>
            </Radio.Group>
          </div>

          <div>
            <p className="mb-2 text-sm font-medium text-slate-700">Triệu chứng ảnh hưởng sinh hoạt thế nào?</p>
            <Radio.Group
              value={answers.activityImpact}
              onChange={(event) => update('activityImpact', event.target.value as ActivityImpact)}
              className="flex flex-col gap-2"
            >
              <Radio value="NONE">Không ảnh hưởng đáng kể</Radio>
              <Radio value="SOME_LIMITATION">Có hạn chế một số hoạt động</Radio>
              <Radio value="PREVENTS_NORMAL_ACTIVITY">Không thể sinh hoạt như bình thường</Radio>
            </Radio.Group>
          </div>

          <div>
            <p className="mb-2 text-sm font-medium text-slate-700">Diễn tiến hiện tại</p>
            <Radio.Group
              value={answers.progression}
              onChange={(event) => update('progression', event.target.value as Progression)}
              className="flex flex-wrap gap-2"
            >
              <Radio.Button value="IMPROVING">Đang giảm</Radio.Button>
              <Radio.Button value="STABLE">Không thay đổi</Radio.Button>
              <Radio.Button value="WORSENING">Đang nặng hơn</Radio.Button>
            </Radio.Group>
          </div>
        </div>
      )}

      {step === 2 && !result && (
        <div>
          <p className="mb-1 text-sm font-medium text-slate-700">Bạn có đang gặp dấu hiệu nào dưới đây không?</p>
          <p className="mb-4 text-xs leading-5 text-slate-500">Chọn tất cả các dấu hiệu hiện đang có. Nếu có dấu hiệu khẩn cấp, hãy ưu tiên tìm trợ giúp y tế ngay.</p>
          <Checkbox.Group
            value={answers.warningSigns}
            onChange={(values) => update('warningSigns', values as WarningSign[])}
            className="grid grid-cols-1 gap-3 sm:grid-cols-2"
          >
            {warningOptions.map((option) => <Checkbox key={option.value} value={option.value}>{option.label}</Checkbox>)}
          </Checkbox.Group>
        </div>
      )}

      {result && (
        <Result
          status={result.emergency ? 'error' : result.severityLevel === 'UNDETERMINED' ? 'info' : 'success'}
          title={<span className="flex flex-wrap items-center justify-center gap-2">Mức độ: <Tag color={severityColors[result.severityLevel]}>{severityLabels[result.severityLevel]}</Tag></span>}
          subTitle={result.requiresClinicianReview ? 'Một số dấu hiệu hoặc thông tin của bạn cần được nhân viên y tế xem xét thêm. Công cụ không thay thế tư vấn y tế.' : 'Đây là đánh giá tham khảo dựa trên câu trả lời của bạn, không phải chẩn đoán.'}
          extra={result.emergency ? (
            <Alert type="error" showIcon message="Có dấu hiệu cảnh báo khẩn cấp" description="Vui lòng gọi 115 hoặc đến cơ sở y tế gần nhất ngay. Đừng chờ chatbot đánh giá thêm." />
          ) : result.severityLevel === 'SEVERE' ? (
            <Alert type="warning" showIcon message="Bạn tự đánh giá triệu chứng ở mức nặng" description="Hãy liên hệ nhân viên y tế để được đánh giá trực tiếp. Nếu xuất hiện dấu hiệu khẩn cấp, gọi 115." />
          ) : null}
        />
      )}

      {step === 2 && !result && urgentSignsSelected && (
        <Alert type="warning" showIcon className="mt-4" message="Bạn đã chọn ít nhất một dấu hiệu khẩn cấp" description="Nếu dấu hiệu đang xảy ra, hãy gọi 115 hoặc đến cơ sở y tế gần nhất ngay." />
      )}
      {step === 2 && !result && warningSet.size > 0 && !urgentSignsSelected && (
        <Alert type="info" showIcon className="mt-4" message="Một số dấu hiệu cần được xem xét thêm" description="Kết quả sẽ yêu cầu đánh giá của nhân viên y tế; công cụ không tự chẩn đoán nguyên nhân." />
      )}
    </Modal>
  )
}
