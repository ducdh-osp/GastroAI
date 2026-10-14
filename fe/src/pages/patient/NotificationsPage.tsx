import { AppShell } from '../../components/layout/AppShell'
import { NotificationCenterPage } from '../shared/NotificationCenterPage'

export default function NotificationsPage() {
  return (
    <AppShell>
      <NotificationCenterPage audience="patient" />
    </AppShell>
  )
}
