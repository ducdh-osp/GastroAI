import { apiClient } from '../lib/axios'
import { cmsApiClient } from '../lib/cmsAxios'

export interface AppNotification {
  id: number
  type: 'JOURNAL_REMINDER' | 'TRIAGE_ALERT' | 'SYSTEM'
  title: string
  message: string
  sourceType: string | null
  sourceId: number | null
  actionUrl: string | null
  createdAt: string
  readAt: string | null
}

export interface NotificationPage {
  items: AppNotification[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  unreadCount: number
}

export type NotificationAudience = 'patient' | 'admin'

function client(audience: NotificationAudience) {
  return audience === 'patient' ? apiClient : cmsApiClient
}

function basePath(audience: NotificationAudience) {
  return audience === 'patient' ? '/patient/notifications' : '/cms/notifications'
}

export async function getNotifications(audience: NotificationAudience, page = 0, size = 20) {
  const { data } = await client(audience).get<NotificationPage>(basePath(audience), {
    params: { page, size },
  })
  return data
}

export async function getUnreadNotificationCount(audience: NotificationAudience) {
  const { data } = await client(audience).get<{ unreadCount: number }>(
    `${basePath(audience)}/unread-count`,
  )
  return data.unreadCount
}

export async function markNotificationRead(audience: NotificationAudience, id: number) {
  await client(audience).post(`${basePath(audience)}/${id}/read`)
}

export async function markAllNotificationsRead(audience: NotificationAudience) {
  await client(audience).post(`${basePath(audience)}/read-all`)
}

export async function deleteNotification(audience: NotificationAudience, id: number) {
  await client(audience).delete(`${basePath(audience)}/${id}`)
}
