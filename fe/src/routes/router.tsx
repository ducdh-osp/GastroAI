import { createBrowserRouter } from 'react-router-dom'
import LoginPage from '../pages/auth/LoginPage'
import CmsLoginPage from '../pages/cms/CmsLoginPage'
import ForgotPasswordPage from '../pages/auth/ForgotPasswordPage'
import RegisterPage from '../pages/auth/RegisterPage'
import ResetPasswordPage from '../pages/auth/ResetPasswordPage'
import VerifyEmailPage from '../pages/auth/VerifyEmailPage'
import HomePage from '../pages/patient/HomePage'
import LoginHistoryPage from '../pages/patient/LoginHistoryPage'
import { ProtectedRoute } from './ProtectedRoute'

export const router = createBrowserRouter([
  // Patient
  { path: '/login', element: <LoginPage /> },
  { path: '/register', element: <RegisterPage /> },
  { path: '/verify-email', element: <VerifyEmailPage /> },
  { path: '/forgot-password', element: <ForgotPasswordPage /> },
  { path: '/reset-password', element: <ResetPasswordPage /> },
  {
    path: '/',
    element: (
      <ProtectedRoute>
        <HomePage />
      </ProtectedRoute>
    ),
  },
  {
    path: '/login-history',
    element: (
      <ProtectedRoute>
        <LoginHistoryPage />
      </ProtectedRoute>
    ),
  },

  // CMS - Admin / Doctor
  {
    path: '/cms/login',
    element: <CmsLoginPage />,
  },
])

