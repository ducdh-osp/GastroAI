import { apiClient } from '../lib/axios'

export type PrimarySymptom =
  | 'ABDOMINAL_PAIN'
  | 'DIARRHEA'
  | 'CONSTIPATION'
  | 'NAUSEA'
  | 'VOMITING'
  | 'HEARTBURN'
  | 'BLOATING'
  | 'OTHER'

export type SymptomDuration =
  | 'LESS_THAN_24_HOURS'
  | 'ONE_TO_THREE_DAYS'
  | 'MORE_THAN_THREE_DAYS'
  | 'RECURRING'
  | 'UNSURE'

export type SeverityLevel = 'MILD' | 'MODERATE' | 'SEVERE' | 'UNDETERMINED'
export type ActivityImpact = 'NONE' | 'SOME_LIMITATION' | 'PREVENTS_NORMAL_ACTIVITY'
export type Progression = 'IMPROVING' | 'STABLE' | 'WORSENING'
export type PatientGroup = 'ADULT' | 'UNDER_18' | 'PREGNANT_OR_RECENTLY_POSTPARTUM' | 'UNSURE'

export type WarningSign =
  | 'BLOOD_IN_VOMIT'
  | 'BLACK_OR_BLOODY_STOOL'
  | 'SUDDEN_SEVERE_ABDOMINAL_PAIN'
  | 'RIGID_OR_TENDER_ABDOMEN'
  | 'UNABLE_TO_PASS_STOOL_OR_GAS'
  | 'UNABLE_TO_URINATE'
  | 'BREATHING_DIFFICULTY_OR_CHEST_PAIN'
  | 'FAINTING_OR_CONFUSION'
  | 'HIGH_FEVER_WITH_ABDOMINAL_PAIN'
  | 'PAIN_RADIATING_TO_BACK_OR_SHOULDER'
  | 'DIABETES_WITH_VOMITING'
  | 'JAUNDICE_WITH_ABDOMINAL_PAIN'
  | 'SEVERE_DEHYDRATION'
  | 'UNEXPLAINED_WEIGHT_LOSS'
  | 'DIFFICULT_OR_PAINFUL_SWALLOWING'

export interface SymptomAssessmentRequest {
  primarySymptom: PrimarySymptom
  primarySymptomDetail?: string
  duration: SymptomDuration
  reportedSeverity: Exclude<SeverityLevel, 'UNDETERMINED'>
  activityImpact: ActivityImpact
  progression: Progression
  patientGroup: PatientGroup
  warningSigns: WarningSign[]
}

export interface SymptomAssessmentResponse {
  id: number
  assessedAt: string
  severityLevel: SeverityLevel
  emergency: boolean
  matchedGroups: string[]
  reasonCodes: string[]
  requiresClinicianReview: boolean
}

export interface SymptomAssessmentHistoryItem extends SymptomAssessmentRequest, SymptomAssessmentResponse {
  severityLevel: SeverityLevel
  matchedGroups: string[]
  reasonCodes: string[]
}

export async function assessSymptoms(request: SymptomAssessmentRequest): Promise<SymptomAssessmentResponse> {
  const { data } = await apiClient.post<SymptomAssessmentResponse>('/patient/triage/assessments', request)
  return data
}

export async function getSymptomAssessmentHistory(): Promise<SymptomAssessmentHistoryItem[]> {
  const { data } = await apiClient.get<SymptomAssessmentHistoryItem[]>('/patient/triage/assessments')
  return data
}
