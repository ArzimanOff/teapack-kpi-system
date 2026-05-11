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
const CompareShiftsPage  = lazy(() => import('./pages/reports/CompareShiftsPage'))
const PlannedShiftsPage  = lazy(() => import('./pages/shifts/PlannedShiftsPage'))
const ActiveShiftsPage   = lazy(() => import('./pages/shifts/ActiveShiftsPage'))
const ShiftHistoryPage   = lazy(() => import('./pages/shifts/ShiftHistoryPage'))
const LinesAdminPage     = lazy(() => import('./pages/admin/LinesAdminPage'))
const InvalidReadingsPage = lazy(() => import('./pages/admin/InvalidReadingsPage'))
const UsersAdminPage     = lazy(() => import('./pages/admin/UsersAdminPage'))
const AuditPage          = lazy(() => import('./pages/admin/AuditPage'))
const EmulatorAdminPage  = lazy(() => import('./pages/admin/EmulatorAdminPage'))
const ThresholdsPage     = lazy(() => import('./pages/thresholds/ThresholdsPage'))
const KpiHelpPage        = lazy(() => import('./pages/help/KpiHelpPage'))

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
        <Route path="shifts/active" element={guarded('/shifts/active', <ActiveShiftsPage />)} />
        <Route path="shifts/history" element={guarded('/shifts/history', <ShiftHistoryPage />)} />
        <Route path="reports" element={guarded('/reports', <ReportsPage />)} />
        <Route path="reports/compare" element={guarded('/reports/compare', <CompareShiftsPage />)} />
        <Route path="admin/lines" element={guarded('/admin/lines', <LinesAdminPage />)} />
        <Route path="admin/readings" element={guarded('/admin/readings', <InvalidReadingsPage />)} />
        <Route path="admin/users" element={guarded('/admin/users', <UsersAdminPage />)} />
        <Route path="admin/audit" element={guarded('/admin/audit', <AuditPage />)} />
        <Route path="admin/emulator" element={guarded('/admin/emulator', <EmulatorAdminPage />)} />
        <Route path="thresholds" element={guarded('/thresholds', <ThresholdsPage />)} />
        <Route path="help/kpi" element={guarded('/help/kpi', <KpiHelpPage />)} />
      </Route>
      <Route path="*" element={<Navigate to="/" />} />
    </Routes>
  )
}

export default App
