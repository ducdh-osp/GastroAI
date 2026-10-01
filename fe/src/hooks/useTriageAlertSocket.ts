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
  statusChangedAt: string
}

export type TriageSocketConnectionState = 'connecting' | 'connected' | 'disconnected'

interface UseTriageAlertSocketOptions {
  /** Chi mo ket noi khi true - tranh connect truoc khi CMS dang nhap xong (session chua co). */
  enabled: boolean
  onAlert: (event: TriageAlertSocketEvent) => void
  onStatusChange: (event: TriageAlertStatusChangedSocketEvent) => void
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
}: UseTriageAlertSocketOptions): { connectionState: TriageSocketConnectionState } {
  const [connectionState, setConnectionState] = useState<TriageSocketConnectionState>('disconnected')

  // Callback moi nhat qua ref de khong phai tao lai STOMP client (va reconnect) moi khi
  // component cha re-render voi closure moi cua onAlert/onStatusChange.
  const onAlertRef = useRef(onAlert)
  onAlertRef.current = onAlert
  const onStatusChangeRef = useRef(onStatusChange)
  onStatusChangeRef.current = onStatusChange

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