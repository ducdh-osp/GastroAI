import { createBrowserRouter } from 'react-router-dom'
import LoginPage from '../pages/auth/LoginPage'
import HomePage from '../pages/patient/HomePage'
import { ProtectedRoute } from './ProtectedRoute'

export const router = createBrowserRouter([
  { path: '/login', element: <LoginPage /> },
  {
    path: '/',
    element: (
      <ProtectedRoute>
        <HomePage />
      </ProtectedRoute>
    ),
  },
])
