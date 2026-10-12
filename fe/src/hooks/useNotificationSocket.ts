import { Client, type IMessage } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { useEffect, useRef } from 'react'
import type { AppNotification, NotificationAudience } from '../api/notifications'

function apiOrigin() {
  const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api/v1'
  return apiBaseUrl.replace(/\/api\/v1\/?$/, '')
}

function patientIdFromToken(token: string) {
  try {
    const payload = token.split('.')[1]
    const base64 = payload.replace(/-/g, '+').replace(/_/g, '/')
    const normalized = base64.padEnd(Math.ceil(base64.length / 4) * 4, '=')
    const decoded = JSON.parse(atob(normalized)) as { sub?: string }
    return decoded.sub ?? null
  } catch {
    return null
  }
}

interface Options {
  audience: NotificationAudience
  enabled: boolean
  token?: string | null
  onNotification: (notification: AppNotification) => void
}

export function useNotificationSocket({ audience, enabled, token, onNotification }: Options) {
  const callbackRef = useRef(onNotification)
  useEffect(() => {
    callbackRef.current = onNotification
  }, [onNotification])

  useEffect(() => {
    if (!enabled) return
    const patientId = audience === 'patient' && token ? patientIdFromToken(token) : null
    if (audience === 'patient' && (!token || !patientId)) return

    const url = audience === 'patient'
      ? `${apiOrigin()}/ws/patient?access_token=${encodeURIComponent(token!)}`
      : `${apiOrigin()}/ws`
    const destination = audience === 'patient'
      ? `/topic/notifications/patient/${patientId}`
      : '/topic/notifications/admin'
    const stompClient = new Client({
      webSocketFactory: () => new SockJS(url) as WebSocket,
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
    })
    stompClient.onConnect = () => {
      stompClient.subscribe(destination, (message: IMessage) => {
        try {
          callbackRef.current(JSON.parse(message.body) as AppNotification)
        } catch {
          // Bỏ qua frame không hợp lệ, giữ kết nối cho các thông báo sau.
        }
      })
    }
    stompClient.activate()
    return () => {
      void stompClient.deactivate()
    }
  }, [audience, enabled, token])
}
