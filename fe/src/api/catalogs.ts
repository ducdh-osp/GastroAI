import { apiClient } from '../lib/axios'

export type CatalogType = 'SYMPTOM' | 'CONDITION' | 'MEDICATION'

export interface CatalogItem {
  id: number
  type: CatalogType
  code: string
  name: string
}

export async function listCatalogItems(type: CatalogType): Promise<CatalogItem[]> {
  const { data } = await apiClient.get<CatalogItem[]>(`/catalogs/${type}`)
  return data
}
