import React, { useState } from 'react'
import { Layout, Menu, Button, Avatar, Space, Typography, Badge } from 'antd'
import { Outlet, useNavigate, useLocation } from 'react-router-dom'
import {
  DashboardOutlined,
  ControlOutlined,
  FileTextOutlined,
  LogoutOutlined,
  UserOutlined,
  CalendarOutlined,
  HistoryOutlined,
  ThunderboltOutlined,
  SettingOutlined,
  WarningOutlined,
  TeamOutlined,
  AuditOutlined,
  ExperimentOutlined,
  SlidersOutlined,
  BookOutlined,
  DiffOutlined,
  BulbOutlined,
} from '@ant-design/icons'
import { clearAuth, getUser, getRole, hasRole } from '../../utils/auth'
import { ROUTE_ACCESS } from '../../constants/access'
import NotificationBell from '../NotificationBell'

const { Header, Sider, Content } = Layout
const { Text } = Typography

const roleLabels = {
  ROLE_ADMIN: 'Администратор',
  ROLE_OPERATOR: 'Оператор',
  ROLE_TECHNOLOGIST: 'Технолог',
}

const SIDER_BG = '#001529'
const SIDER_WIDTH = 200
const SIDER_COLLAPSED_WIDTH = 80

function MainLayout() {
  const [collapsed, setCollapsed] = useState(false)
  const navigate = useNavigate()
  const location = useLocation()
  const user = getUser()
  const role = getRole()

  const allMenuItems = [
    { key: '/dashboard',      icon: <DashboardOutlined />, label: 'Дашборд' },
    { key: '/recommendations', icon: <BulbOutlined />,     label: 'Рекомендации' },
    { key: '/operator',       icon: <ControlOutlined />,   label: 'Оператор' },
    { key: '/shifts/planned', icon: <CalendarOutlined />,    label: 'Запланированные смены' },
    { key: '/shifts/active',  icon: <ThunderboltOutlined />, label: 'Активные смены' },
    { key: '/shifts/history', icon: <HistoryOutlined />,     label: 'История смен' },
    { key: '/reports',        icon: <FileTextOutlined />,  label: 'Отчёты' },
    { key: '/reports/compare', icon: <DiffOutlined />,     label: 'Сравнение смен' },
    { key: '/thresholds',     icon: <SlidersOutlined />,   label: 'Пороги KPI' },
    { key: '/admin/lines',    icon: <SettingOutlined />,   label: 'Линии (админ)' },
    { key: '/admin/readings', icon: <WarningOutlined />,   label: 'Outlier-показания' },
    { key: '/admin/users',    icon: <TeamOutlined />,      label: 'Пользователи' },
    { key: '/admin/audit',    icon: <AuditOutlined />,     label: 'Журнал аудита' },
    { key: '/admin/emulator', icon: <ExperimentOutlined />, label: 'Эмулятор' },
    { key: '/help/kpi',       icon: <BookOutlined />,      label: 'Справка KPI' },
  ]
  const menuItems = allMenuItems.filter(item => hasRole(ROUTE_ACCESS[item.key]))

  const handleLogout = () => {
    clearAuth()
    navigate('/login')
  }

  const siderWidth = collapsed ? SIDER_COLLAPSED_WIDTH : SIDER_WIDTH

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider
        collapsible
        collapsed={collapsed}
        onCollapse={setCollapsed}
        width={SIDER_WIDTH}
        collapsedWidth={SIDER_COLLAPSED_WIDTH}
        style={{
          background: SIDER_BG,
          position: 'fixed',
          insetInlineStart: 0,
          top: 0,
          bottom: 0,
          height: '100vh',
          zIndex: 100,
          boxShadow: '2px 0 8px rgba(0,0,0,0.15)',
        }}
      >
        <div style={{
          display: 'flex',
          flexDirection: 'column',
          height: 'calc(100vh - 48px)', // 48px — высота AntD trigger
        }}>
          <div style={{
            height: 64, flexShrink: 0,
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            color: 'white', fontWeight: 'bold',
            fontSize: collapsed ? 12 : 16, padding: '0 8px',
            background: SIDER_BG,
            letterSpacing: collapsed ? 0 : 1,
          }}>
            {collapsed ? 'TP' : 'TeaPack KPI'}
          </div>
          <div style={{
            flex: 1,
            overflowY: 'auto',
            overflowX: 'hidden',
            scrollbarWidth: 'thin',
          }}>
            <Menu
              theme="dark"
              mode="inline"
              style={{ background: SIDER_BG, borderInlineEnd: 0 }}
              selectedKeys={[location.pathname]}
              items={menuItems}
              onClick={({ key }) => navigate(key)}
            />
          </div>
        </div>
      </Sider>
      <Layout style={{
        marginInlineStart: siderWidth,
        transition: 'margin-inline-start 0.2s',
      }}>
        <Header style={{
          background: '#fff', padding: '0 24px',
          display: 'flex', alignItems: 'center',
          justifyContent: 'space-between',
          boxShadow: '0 1px 4px rgba(0,0,0,0.1)',
          position: 'sticky', top: 0, zIndex: 50,
        }}>
          <Text strong style={{ fontSize: 16 }}>
            Система мониторинга производственного участка
          </Text>
          <Space size="middle">
            <NotificationBell />
            <Avatar icon={<UserOutlined />} />
            <Text>{user.username}</Text>
            <Text type="secondary">({roleLabels[role] || role})</Text>
            <Button icon={<LogoutOutlined />} onClick={handleLogout}>
              Выйти
            </Button>
          </Space>
        </Header>
        <Content style={{ margin: 24, minHeight: 280 }}>
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  )
}

export default MainLayout