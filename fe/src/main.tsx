import { ConfigProvider } from 'antd'
import dayjs from 'dayjs'
import customParseFormat from 'dayjs/plugin/customParseFormat'
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { RouterProvider } from 'react-router-dom'
import './index.css'
import { router } from './routes/router'
import { AuthProvider } from './stores/authStore'

// Bat buoc cho dayjs(str, format) parse dung dinh dang (vd "HH:mm:ss") - khong co plugin
// nay dayjs bo qua tham so format va roi ve parse bang Date native, luon ra Invalid Date
// voi chuoi gio don le nhu "08:00". Dang ky 1 lan duy nhat o entry point.
dayjs.extend(customParseFormat)

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <ConfigProvider
      theme={{
        token: {
          colorPrimary: '#0d9488',
          borderRadius: 8,
          fontFamily:
            '"Manrope", -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
        },
      }}
    >
      <AuthProvider>
        <RouterProvider router={router} />
      </AuthProvider>
    </ConfigProvider>
  </StrictMode>,
)
