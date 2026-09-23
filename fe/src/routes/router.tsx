import { createBrowserRouter, Outlet } from 'react-router-dom'
import LoginPage from '../pages/auth/LoginPage'
import CmsLoginPage from '../pages/cms/CmsLoginPage'
import CmsHomePage from '../pages/cms/CmsHomePage'
import CmsLoginHistoryPage from '../pages/cms/CmsLoginHistoryPage'
import ForgotPasswordPage from '../pages/auth/ForgotPasswordPage'
import RegisterPage from '../pages/auth/RegisterPage'
import ResetPasswordPage from '../pages/auth/ResetPasswordPage'
import ChangePasswordPage from '../pages/auth/ChangePasswordPage'
import VerifyEmailPage from '../pages/auth/VerifyEmailPage'
import HomePage from '../pages/patient/HomePage'
import LoginHistoryPage from '../pages/patient/LoginHistoryPage'
import { ProtectedRoute } from './ProtectedRoute'
import { CmsProtectedRoute } from './CmsProtectedRoute'
import { CmsAuthProvider } from '../stores/cmsAuthStore'

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
  {
    path: '/change-password',
    element: (
      <ProtectedRoute>
        <ChangePasswordPage />
      </ProtectedRoute>
    ),
  },

  // CMS - Admin / Doctor. Dùng 1 CmsAuthProvider chung cho cả nhánh /cms/* — nếu mỗi route
  // tự bọc provider riêng, state đăng nhập ở /cms/login sẽ mất ngay khi điều hướng sang /cms
  // (mỗi provider là 1 instance context độc lập, không chia sẻ được cho nhau).
  {
    element: (
      <CmsAuthProvider>
        <Outlet />
      </CmsAuthProvider>
    ),
    children: [
      { path: '/cms/login', element: <CmsLoginPage /> },
      {
        path: '/cms',
        element: (
          <CmsProtectedRoute>
            <CmsHomePage />
          </CmsProtectedRoute>
        ),
      },
      {
        path: '/cms/login-history',
        element: (
          <CmsProtectedRoute>
            <CmsLoginHistoryPage />
          </CmsProtectedRoute>
        ),
      },
    ],
  },
])
