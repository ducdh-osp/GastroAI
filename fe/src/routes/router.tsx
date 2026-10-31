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
import MedicalProfilePage from '../pages/patient/MedicalProfilePage'
import FoodDiaryPage from '../pages/patient/FoodDiaryPage'
import FoodDiaryHistoryPage from '../pages/patient/FoodDiaryHistoryPage'
import FoodSensitivityProfilePage from '../pages/patient/FoodSensitivityProfilePage'
import AdherencePage from '../pages/patient/AdherencePage'
import BristolPage from '../pages/patient/BristolPage'
import BristolHistoryPage from '../pages/patient/BristolHistoryPage'
import MedicationRemindersPage from '../pages/patient/MedicationRemindersPage'
import ChatPage from '../pages/chat/ChatPage'
import ChatHistoryPage from '../pages/chat/ChatHistoryPage'
import SymptomAssessmentPage from '../pages/chat/SymptomAssessmentPage'
import { ProtectedRoute } from './ProtectedRoute'
import { CmsProtectedRoute } from './CmsProtectedRoute'
import { CmsAdminRoute } from './CmsAdminRoute'
import { CmsAuthProvider } from '../stores/cmsAuthStore'
import { FaviconSwitcher } from '../components/brand/FaviconSwitcher'
import CmsTriageAlertsPage from '../pages/cms/CmsTriageAlertsPage'
import CmsTriageKeywordsPage from '../pages/cms/CmsTriageKeywordsPage'
import CmsRuntimeConfigPage from '../pages/cms/CmsRuntimeConfigPage'
import CmsArticleTopicsPage from '../pages/cms/CmsArticleTopicsPage'
import CmsArticlesPage from '../pages/cms/CmsArticlesPage'
import CmsArticleEditorPage from '../pages/cms/CmsArticleEditorPage'
import CmsDocumentsPage from '../pages/cms/CmsDocumentsPage'
import CmsFaqPage from '../pages/cms/CmsFaqPage'
import CmsCatalogsPage from '../pages/cms/CmsCatalogsPage'
import KnowledgeLibraryPage from '../pages/patient/KnowledgeLibraryPage'
import FaqPage from '../pages/patient/FaqPage'
import NotificationsPage from '../pages/patient/NotificationsPage'
import CmsNotificationsPage from '../pages/cms/CmsNotificationsPage'
import CmsUsersPage from '../pages/cms/CmsUsersPage'
import CmsRbacPage from '../pages/cms/CmsRbacPage'
import CmsChangePasswordPage from '../pages/cms/CmsChangePasswordPage'
import CmsDashboardPage from '../pages/cms/CmsDashboardPage'
import CmsBroadcastPage from '../pages/cms/CmsBroadcastPage'
import CmsAuditLogsPage from '../pages/cms/CmsAuditLogsPage'
import CmsEmailTemplatesPage from '../pages/cms/CmsEmailTemplatesPage'
import CmsEvaluationPage from '../pages/cms/CmsEvaluationPage'

export const router = createBrowserRouter([
  {
    // Bọc toàn bộ route để FaviconSwitcher (cần useLocation, chỉ dùng được bên trong Router)
    // theo dõi được path hiện tại và đổi icon tab tương ứng - không đổi hành vi route nào cả.
    element: (
      <>
        <FaviconSwitcher />
        <Outlet />
      </>
    ),
    children: [
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
        path: '/notifications',
        element: (
          <ProtectedRoute>
            <NotificationsPage />
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
      {
        path: '/medical-profile',
        element: (
          <ProtectedRoute>
            <MedicalProfilePage />
          </ProtectedRoute>
        ),
      },
      {
        path: '/food-diary',
        element: (
          <ProtectedRoute>
            <FoodDiaryPage />
          </ProtectedRoute>
        ),
      },
      {
        path: '/food-diary-history',
        element: (
          <ProtectedRoute>
            <FoodDiaryHistoryPage />
          </ProtectedRoute>
        ),
      },
      {
        path: '/food-sensitivity',
        element: (
          <ProtectedRoute>
            <FoodSensitivityProfilePage />
          </ProtectedRoute>
        ),
      },
      {
        path: '/adherence',
        element: (
          <ProtectedRoute>
            <AdherencePage />
          </ProtectedRoute>
        ),
      },
      {
        path: '/bristol',
        element: (
          <ProtectedRoute>
            <BristolPage />
          </ProtectedRoute>
        ),
      },
      {
        path: '/bristol-history',
        element: (
          <ProtectedRoute>
            <BristolHistoryPage />
          </ProtectedRoute>
        ),
      },
      {
        path: '/medications',
        element: (
          <ProtectedRoute>
            <MedicationRemindersPage />
          </ProtectedRoute>
        ),
      },
      {
        path: '/chat',
        element: (
          <ProtectedRoute>
            <ChatPage />
          </ProtectedRoute>
        ),
      },
      {
        path: '/chat-history',
        element: (
          <ProtectedRoute>
            <ChatHistoryPage />
          </ProtectedRoute>
        ),
      },
      {
        path: '/symptom-assessment',
        element: (
          <ProtectedRoute>
            <SymptomAssessmentPage />
          </ProtectedRoute>
        ),
      },
      {
        path: '/articles',
        element: (
          <ProtectedRoute>
            <KnowledgeLibraryPage />
          </ProtectedRoute>
        ),
      },
      {
        path: '/articles/:slug',
        element: (
          <ProtectedRoute>
            <KnowledgeLibraryPage />
          </ProtectedRoute>
        ),
      },
      {
        path: '/faq',
        element: (
          <ProtectedRoute>
            <FaqPage />
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
            path: '/cms/notifications',
            element: (
              <CmsProtectedRoute>
                <CmsNotificationsPage />
              </CmsProtectedRoute>
            ),
          },
          {
            path: '/cms/users',
            element: (
              <CmsAdminRoute>
                <CmsUsersPage />
              </CmsAdminRoute>
            ),
          },
          {
            path: '/cms/rbac',
            element: (
              <CmsAdminRoute>
                <CmsRbacPage />
              </CmsAdminRoute>
            ),
          },
          {
            path: '/cms/dashboard',
            element: (
              <CmsAdminRoute>
                <CmsDashboardPage />
              </CmsAdminRoute>
            ),
          },
          {
            path: '/cms/broadcast',
            element: (
              <CmsAdminRoute>
                <CmsBroadcastPage />
              </CmsAdminRoute>
            ),
          },
          {
            path: '/cms/audit-logs',
            element: (
              <CmsAdminRoute>
                <CmsAuditLogsPage />
              </CmsAdminRoute>
            ),
          },
          {
            path: '/cms/evaluation',
            element: (
              <CmsProtectedRoute>
                <CmsEvaluationPage />
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
          {
            path: '/cms/change-password',
            element: (
              <CmsProtectedRoute>
                <CmsChangePasswordPage />
              </CmsProtectedRoute>
            ),
          },
          {
            path: '/cms/triage-alerts',
            element: (
              <CmsProtectedRoute>
                <CmsTriageAlertsPage />
              </CmsProtectedRoute>
            ),
          },
          {
            path: '/cms/documents',
            element: (
              <CmsProtectedRoute>
                <CmsDocumentsPage />
              </CmsProtectedRoute>
            ),
          },
          {
            path: '/cms/triage-keywords',
            element: (
              <CmsProtectedRoute>
                <CmsTriageKeywordsPage />
              </CmsProtectedRoute>
            ),
          },
          {
            path: '/cms/runtime-config',
            element: (
              <CmsProtectedRoute>
                <CmsRuntimeConfigPage />
              </CmsProtectedRoute>
            ),
          },
          {
            path: '/cms/email-templates',
            element: (
              <CmsAdminRoute>
                <CmsEmailTemplatesPage />
              </CmsAdminRoute>
            ),
          },
          {
            path: '/cms/article-topics',
            element: (
              <CmsProtectedRoute>
                <CmsArticleTopicsPage />
              </CmsProtectedRoute>
            ),
          },
          {
            path: '/cms/articles',
            element: (
              <CmsProtectedRoute>
                <CmsArticlesPage />
              </CmsProtectedRoute>
            ),
          },
          {
            path: '/cms/articles/new',
            element: (
              <CmsProtectedRoute>
                <CmsArticleEditorPage />
              </CmsProtectedRoute>
            ),
          },
          {
            path: '/cms/articles/:id/edit',
            element: (
              <CmsProtectedRoute>
                <CmsArticleEditorPage />
              </CmsProtectedRoute>
            ),
          },
          {
            path: '/cms/faq',
            element: (
              <CmsProtectedRoute>
                <CmsFaqPage />
              </CmsProtectedRoute>
            ),
          },
          {
            path: '/cms/catalogs',
            element: (
              <CmsProtectedRoute>
                <CmsCatalogsPage />
              </CmsProtectedRoute>
            ),
          },
        ],
      },
    ],
  },
])
