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

export interface NotificationSettings {
  medicationReminderEnabled: boolean
  journalReminderEnabled: boolean
  systemBroadcastEnabled: boolean
  securityAlertEnabled: boolean
  updatedAt?: string
}

export interface UpdateNotificationSettingsRequest {
  medicationReminderEnabled: boolean
  journalReminderEnabled: boolean
  systemBroadcastEnabled: boolean
  securityAlertEnabled: boolean
}

export async function getNotificationSettings(): Promise<NotificationSettings> {
  const { data } = await apiClient.get<NotificationSettings>('/patient/notifications/settings')
  return data
}

export async function updateNotificationSettings(
  payload: UpdateNotificationSettingsRequest,
): Promise<NotificationSettings> {
  const { data } = await apiClient.put<NotificationSettings>(
    '/patient/notifications/settings',
    payload,
  )
  return data
}

export interface BroadcastAnnouncement {
  id: number
  title: string
  message: string
  targetGroup: string
  actionUrl: string | null
  createdByAdminId: number | null
  recipientCount: number
  createdAt: string
}

export interface BroadcastAnnouncementPage {
  content?: BroadcastAnnouncement[]
  items?: BroadcastAnnouncement[]
  page?: number
  number?: number
  size: number
  totalElements: number
  totalPages: number
}

export interface SendBroadcastRequest {
  title: string
  message: string
  targetGroup?: string
  actionUrl?: string
  sendWebSocket?: boolean
}

export async function sendBroadcastNotification(
  payload: SendBroadcastRequest,
): Promise<BroadcastAnnouncement> {
  const { data } = await cmsApiClient.post<BroadcastAnnouncement>(
    '/cms/notifications/broadcast',
    payload,
  )
  return data
}

export async function getBroadcastAnnouncements(
  page = 0,
  size = 10,
): Promise<BroadcastAnnouncementPage> {
  const { data } = await cmsApiClient.get<BroadcastAnnouncementPage>(
    '/cms/notifications/broadcast',
    { params: { page, size } },
  )
  return data
}

export async function getBroadcastAnnouncementDetail(
  id: number,
): Promise<BroadcastAnnouncement> {
  const { data } = await cmsApiClient.get<BroadcastAnnouncement>(
    `/cms/notifications/broadcast/${id}`,
  )
  return data
}
