import { CmsAppShell } from '../../components/cms/CmsAppShell'
import { NotificationCenterPage } from '../shared/NotificationCenterPage'
import { Navigate } from 'react-router-dom'
import { useCmsAuth } from '../../stores/cmsAuthStore'

export default function CmsNotificationsPage() {
  const { user } = useCmsAuth()
  if (user?.userType !== 'ADMIN') return <Navigate to="/cms" replace />
  return (
    <CmsAppShell>
      <NotificationCenterPage audience="admin" />
    </CmsAppShell>
  )
}
