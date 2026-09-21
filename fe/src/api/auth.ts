import { apiClient } from '../lib/axios'

export interface LoginPayload {
  email: string
  password: string
}

export interface AuthResponse {
  token: string
  patientId: number
  email: string
  fullName: string | null
}

export async function login(payload: LoginPayload): Promise<AuthResponse> {
  const { data } = await apiClient.post<AuthResponse>('/auth/login', payload)
  return data
}
