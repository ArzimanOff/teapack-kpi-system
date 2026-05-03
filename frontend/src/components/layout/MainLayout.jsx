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
} from '@ant-design/icons'
import { clearAuth, getUser, getRole, hasRole } from '../../utils/auth'
import { ROUTE_ACCESS } from '../../constants/access'

const { Header, Sider, Content } = Layout
const { Text } = Typography

const roleLabels = {
  ROLE_ADMIN: 'Администратор',
  ROLE_OPERATOR: 'Оператор',
  ROLE_TECHNOLOGIST: 'Технолог',
}

function MainLayout() {
  const [collapsed, setCollapsed] = useState(false)
  const navigate = useNavigate()
  const location = useLocation()
  const user = getUser()
  const role = getRole()

  const allMenuItems = [
    { key: '/dashboard',      icon: <DashboardOutlined />, label: 'Дашборд' },
    { key: '/operator',       icon: <ControlOutlined />,   label: 'Оператор' },
    { key: '/shifts/planned', icon: <CalendarOutlined />,  label: 'Запланированные смены' },
    { key: '/shifts/history', icon: <HistoryOutlined />,   label: 'История смен' },
    { key: '/reports',        icon: <FileTextOutlined />,  label: 'Отчёты' },
  ]
  const menuItems = allMenuItems.filter(item => hasRole(ROUTE_ACCESS[item.key]))

  const handleLogout = () => {
    clearAuth()
    navigate('/login')
  }

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider collapsible collapsed={collapsed} onCollapse={setCollapsed}
        style={{ background: '#001529' }}>
        <div style={{
          height: 64, display: 'flex', alignItems: 'center',
          justifyContent: 'center', color: 'white', fontWeight: 'bold',
          fontSize: collapsed ? 12 : 16, padding: '0 8px'
        }}>
          {collapsed ? 'TP' : 'TeaPack KPI'}
        </div>
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={[location.pathname]}
          items={menuItems}
          onClick={({ key }) => navigate(key)}
        />
      </Sider>
      <Layout>
        <Header style={{
          background: '#fff', padding: '0 24px',
          display: 'flex', alignItems: 'center',
          justifyContent: 'space-between',
          boxShadow: '0 1px 4px rgba(0,0,0,0.1)'
        }}>
          <Text strong style={{ fontSize: 16 }}>
            Система мониторинга производственного участка
          </Text>
          <Space>
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