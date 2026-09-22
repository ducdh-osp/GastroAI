import { createBrowserRouter } from 'react-router-dom'
import LoginPage from '../pages/auth/LoginPage'
import CmsLoginPage from '../pages/cms/CmsLoginPage'
import HomePage from '../pages/patient/HomePage'
import { ProtectedRoute } from './ProtectedRoute'

export const router = createBrowserRouter([
  // Patient
  { path: '/login', element: <LoginPage /> },
  {
    path: '/',
    element: (
      <ProtectedRoute>
        <HomePage />
      </ProtectedRoute>
    ),
  },

  // CMS - Admin / Doctor
  {
    path: '/cms/login',
    element: <CmsLoginPage />,
  },
])

