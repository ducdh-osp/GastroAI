import { Client, type IMessage } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { useEffect, useRef, useState } from 'react'
import type { TriageAlertStatus } from '../api/cmsTriage'


export interface TriageAlertSocketEvent {
  id: number
  patientId: number
  patientFullName: string | null
  patientPhone: string | null
  sessionId: number | null
  messageId: number | null
  messageContent: string
  matchedGroups: string[]
  status: TriageAlertStatus
  occurredAt: string
}

export interface TriageAlertStatusChangedSocketEvent {
  id: number
  status: TriageAlertStatus
  claimedById: number | null
  claimedByType: 'ADMIN' | 'DOCTOR' | null
  resolvedById: number | null
  resolvedByType: 'ADMIN' | 'DOCTOR' | null
  statusChangedAt: string
}

export type TriageSocketConnectionState = 'connecting' | 'connected' | 'disconnected'

interface UseTriageAlertSocketOptions {
  /** Chi mo ket noi khi true - tranh connect truoc khi CMS dang nhap xong (session chua co). */
  enabled: boolean
  onAlert: (event: TriageAlertSocketEvent) => void
  onStatusChange: (event: TriageAlertStatusChangedSocketEvent) => void
  /**
   * Goi moi khi ket noi THANH CONG - ca lan dau lan @stomp/stompjs tu dong noi lai sau khi
   * roi mang. Dung de dong bo lai danh sach tu API, bu lai nhung canh bao phat sinh trong
   * luc mat ket noi (WebSocket khong luu lai tin de gui bu).
   */
  onConnected?: () => void
}

const ALERTS_DESTINATION = '/topic/triage-alerts'
const STATUS_DESTINATION = '/topic/triage-alerts-status'


function resolveWsUrl(): string {
  const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api/v1'
  return `${new URL(apiBaseUrl).origin}/ws`
}


export function useTriageAlertSocket({
  enabled,
  onAlert,
  onStatusChange,
  onConnected,
}: UseTriageAlertSocketOptions): { connectionState: TriageSocketConnectionState } {
  const [connectionState, setConnectionState] = useState<TriageSocketConnectionState>('disconnected')

  // Callback moi nhat qua ref de khong phai tao lai STOMP client (va reconnect) moi khi
  // component cha re-render voi closure moi cua onAlert/onStatusChange/onConnected.
  const onAlertRef = useRef(onAlert)
  onAlertRef.current = onAlert
  const onStatusChangeRef = useRef(onStatusChange)
  onStatusChangeRef.current = onStatusChange
  const onConnectedRef = useRef(onConnected)
  onConnectedRef.current = onConnected

  useEffect(() => {
    if (!enabled) {
      setConnectionState('disconnected')
      return
    }

    const client = new Client({
      webSocketFactory: () => new SockJS(resolveWsUrl()) as WebSocket,
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
    })

    client.onConnect = () => {
      setConnectionState('connected')

      client.subscribe(ALERTS_DESTINATION, (message: IMessage) => {
        try {
          onAlertRef.current(JSON.parse(message.body) as TriageAlertSocketEvent)
        } catch {
          // Bo qua ban tin khong parse duoc - khong lam vo luong ket noi.
        }
      })

      client.subscribe(STATUS_DESTINATION, (message: IMessage) => {
        try {
          onStatusChangeRef.current(JSON.parse(message.body) as TriageAlertStatusChangedSocketEvent)
        } catch {
          // Bo qua ban tin khong parse duoc.
        }
      })

      // Dang ky 2 kenh xong roi moi dong bo lai danh sach - neu dong bo truoc, canh bao
      // den dung giua luc dang tai se lot vao khe ho va mat (@stomp/stompjs goi onConnect
      // ca lan dau lan moi lan tu dong noi lai, nen 1 cho nay la du cho ca 2 truong hop).
      onConnectedRef.current?.()
    }


    client.onWebSocketClose = () => setConnectionState('disconnected')
    client.onStompError = () => setConnectionState('disconnected')

    setConnectionState('connecting')
    client.activate()

    return () => {
      void client.deactivate()
      setConnectionState('disconnected')
    }
  }, [enabled])

  return { connectionState }
}