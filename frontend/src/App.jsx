import React from 'react'
import { Routes, Route, Navigate } from 'react-router-dom'
import { isAuthenticated, getRole } from './utils/auth'
import LoginPage from './pages/login/LoginPage'
import OperatorPage from './pages/operator/OperatorPage'
import DashboardPage from './pages/dashboard/DashboardPage'
import ReportsPage from './pages/reports/ReportsPage'
import MainLayout from './components/layout/MainLayout'

const PrivateRoute = ({ children }) => {
  return isAuthenticated() ? children : <Navigate to="/login" />
}

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
        <Route path="dashboard" element={<DashboardPage />} />
        <Route path="operator" element={<OperatorPage />} />
        <Route path="reports" element={<ReportsPage />} />
      </Route>
      <Route path="*" element={<Navigate to="/" />} />
    </Routes>
  )
}

export default App