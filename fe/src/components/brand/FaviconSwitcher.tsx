import { useEffect } from 'react'
import { useLocation } from 'react-router-dom'

const PATIENT_FAVICON = '/favicon.svg'
const CMS_FAVICON = '/favicon-cms.svg'

/** Đổi icon tab theo route: vào /cms/* thì đổi sang icon CMS riêng, ra khỏi thì trả lại icon
 * Gastro AI mặc định. Không render gì, chỉ chỉnh thẻ <link rel="icon"> trong index.html. */
export function FaviconSwitcher() {
  const { pathname } = useLocation()

  useEffect(() => {
    const link = document.querySelector<HTMLLinkElement>('link[rel="icon"]')
    if (!link) return
    link.href = pathname.startsWith('/cms') ? CMS_FAVICON : PATIENT_FAVICON
  }, [pathname])

  return null
}
