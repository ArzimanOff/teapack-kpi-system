import React, { lazy, Suspense } from 'react'
import { Routes, Route, Navigate } from 'react-router-dom'
import { Spin } from 'antd'
import { isAuthenticated } from './utils/auth'
import LoginPage from './pages/login/LoginPage'
import MainLayout from './components/layout/MainLayout'
import RoleGuard from './components/RoleGuard'
import { ROUTE_ACCESS } from './constants/access'

const DashboardPage      = lazy(() => import('./pages/dashboard/DashboardPage'))
const OperatorPage       = lazy(() => import('./pages/operator/OperatorPage'))
const ReportsPage        = lazy(() => import('./pages/reports/ReportsPage'))
const PlannedShiftsPage  = lazy(() => import('./pages/shifts/PlannedShiftsPage'))
const ShiftHistoryPage   = lazy(() => import('./pages/shifts/ShiftHistoryPage'))
const LinesAdminPage     = lazy(() => import('./pages/admin/LinesAdminPage'))
const InvalidReadingsPage = lazy(() => import('./pages/admin/InvalidReadingsPage'))

const PrivateRoute = ({ children }) => {
  return isAuthenticated() ? children : <Navigate to="/login" />
}

const PageFallback = () => (
  <div style={{ display: 'flex', justifyContent: 'center', padding: 64 }}>
    <Spin size="large" />
  </div>
)

const guarded = (path, element) => (
  <RoleGuard allowed={ROUTE_ACCESS[path]}>
    <Suspense fallback={<PageFallback />}>{element}</Suspense>
  </RoleGuard>
)

function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/" element={
        <PrivateRoute>
          <MainLayout />
        </PrivateRoute>
      }>
        <Route index element={<Navigate to="/dashboard" />} />
        <Route path="dashboard" element={guarded('/dashboard', <DashboardPage />)} />
        <Route path="operator" element={guarded('/operator', <OperatorPage />)} />
        <Route path="shifts/planned" element={guarded('/shifts/planned', <PlannedShiftsPage />)} />
        <Route path="shifts/history" element={guarded('/shifts/history', <ShiftHistoryPage />)} />
        <Route path="reports" element={guarded('/reports', <ReportsPage />)} />
        <Route path="admin/lines" element={guarded('/admin/lines', <LinesAdminPage />)} />
        <Route path="admin/readings" element={guarded('/admin/readings', <InvalidReadingsPage />)} />
      </Route>
      <Route path="*" element={<Navigate to="/" />} />
    </Routes>
  )
}

export default App
