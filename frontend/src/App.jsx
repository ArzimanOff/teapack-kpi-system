import React from 'react'
import { Routes, Route, Navigate } from 'react-router-dom'
import { isAuthenticated } from './utils/auth'
import LoginPage from './pages/login/LoginPage'
import OperatorPage from './pages/operator/OperatorPage'
import DashboardPage from './pages/dashboard/DashboardPage'
import ReportsPage from './pages/reports/ReportsPage'
import PlannedShiftsPage from './pages/shifts/PlannedShiftsPage'
import ShiftHistoryPage from './pages/shifts/ShiftHistoryPage'
import MainLayout from './components/layout/MainLayout'
import RoleGuard from './components/RoleGuard'
import { ROUTE_ACCESS } from './constants/access'

const PrivateRoute = ({ children }) => {
  return isAuthenticated() ? children : <Navigate to="/login" />
}

const guarded = (path, element) => (
  <RoleGuard allowed={ROUTE_ACCESS[path]}>{element}</RoleGuard>
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
      </Route>
      <Route path="*" element={<Navigate to="/" />} />
    </Routes>
  )
}

export default App
