import { cmsApiClient } from '../lib/cmsAxios'

export interface EvaluationSummary {
  // UC0038 Triage metrics
  triageSensitivityRecall: number
  triageFalseAlarmRate: number
  triageF2Score: number
  triageTotalCases: number
  triageTruePositives: number
  triageFalseNegatives: number
  // UC0037 RAG metrics
  ragHitRateAt1: number
  ragHitRateAt3: number
  ragHitRateAtK: number
  ragMrr: number
  ragAvgSimilarity: number
  ragTotalQueries: number
  ragTopK: number
  lastEvaluatedAt: string
}

export interface CategoryMetric {
  category: string
  totalCases: number
  truePositives: number
  falseNegatives: number
  recall: number
}

export interface TriageCaseEvalResult {
  id: string
  query: string
  expectedEmergency: boolean
  predictedEmergency: boolean
  matchedGroups: string[]
  classification: 'TP' | 'TN' | 'FP' | 'FN'
  category: string
  notes: string
}

export interface TriageEvaluationReport {
  totalCases: number
  positiveCases: number
  negativeCases: number
  truePositives: number
  falseNegatives: number
  falsePositives: number
  trueNegatives: number
  sensitivityRecall: number
  falseAlarmRate: number
  specificity: number
  precision: number
  accuracy: number
  f1Score: number
  f2Score: number
  categoryMetrics: CategoryMetric[]
  caseResults: TriageCaseEvalResult[]
  evaluatedAt: string
}

export interface RetrievedChunkSnippet {
  chunkId: number
  documentId: number
  documentTitle: string
  distance: number
  similarity: number
  isRelevant: boolean
  rank: number
}

export interface RagQueryEvalResult {
  id: string
  query: string
  category: string
  expectedDocKeyword: string
  hit: boolean
  firstHitRank: number
  reciprocalRank: number
  top1Similarity: number
  latencyMs: number
  retrievedChunks: RetrievedChunkSnippet[]
}

export interface RagEvaluationReport {
  totalQueries: number
  topK: number
  hitCountAt1: number
  hitCountAt3: number
  hitCountAtK: number
  hitRateAt1: number
  hitRateAt3: number
  hitRateAtK: number
  mrr: number
  averagePrecisionAtK: number
  averageRecallAtK: number
  averageSimilarity: number
  averageLatencyMs: number
  retrievalMode: string
  queryResults: RagQueryEvalResult[]
  evaluatedAt: string
}

export async function getEvaluationSummary(): Promise<EvaluationSummary> {
  const { data } = await cmsApiClient.get<EvaluationSummary>('/cms/evaluation/summary')
  return data
}

export async function getTriageEvaluation(): Promise<TriageEvaluationReport> {
  const { data } = await cmsApiClient.get<TriageEvaluationReport>('/cms/evaluation/triage')
  return data
}

export async function runTriageEvaluation(): Promise<TriageEvaluationReport> {
  const { data } = await cmsApiClient.post<TriageEvaluationReport>('/cms/evaluation/triage/run')
  return data
}

export async function getRagEvaluation(): Promise<RagEvaluationReport> {
  const { data } = await cmsApiClient.get<RagEvaluationReport>('/cms/evaluation/rag')
  return data
}

export async function runRagEvaluation(topK = 5): Promise<RagEvaluationReport> {
  const { data } = await cmsApiClient.post<RagEvaluationReport>('/cms/evaluation/rag/run', null, {
    params: { topK },
  })
  return data
}
