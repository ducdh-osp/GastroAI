import axios from 'axios'

const baseURL =
  import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api/v1'

export const cmsApiClient = axios.create({
  baseURL,
  timeout: 10000,
  withCredentials: true,
})