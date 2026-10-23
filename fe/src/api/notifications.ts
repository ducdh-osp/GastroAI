import { apiClient } from '../lib/axios'
import { cmsApiClient } from '../lib/cmsAxios'

export interface AppNotification {
  id: number
  type: NotificationType
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
export type NotificationType = 'JOURNAL_REMINDER' | 'MEDICATION_REMINDER' | 'TRIAGE_ALERT' | 'SYSTEM'

export interface NotificationFilters {
  unreadOnly?: boolean
  type?: NotificationType
}

function client(audience: NotificationAudience) {
  return audience === 'patient' ? apiClient : cmsApiClient
}

function basePath(audience: NotificationAudience) {
  return audience === 'patient' ? '/patient/notifications' : '/cms/notifications'
}

export async function getNotifications(
  audience: NotificationAudience,
  page = 0,
  size = 20,
  filters: NotificationFilters = {},
) {
  const { data } = await client(audience).get<NotificationPage>(basePath(audience), {
    params: { page, size, unreadOnly: filters.unreadOnly ?? false, type: filters.type },
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

export async function markNotificationUnread(audience: NotificationAudience, id: number) {
  await client(audience).post(`${basePath(audience)}/${id}/unread`)
}

export async function markAllNotificationsRead(audience: NotificationAudience) {
  await client(audience).post(`${basePath(audience)}/read-all`)
}

export async function deleteNotification(audience: NotificationAudience, id: number) {
  await client(audience).delete(`${basePath(audience)}/${id}`)
}
