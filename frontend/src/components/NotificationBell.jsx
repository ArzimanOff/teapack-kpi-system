import React, { useEffect, useState } from 'react'
import { Badge, Button, Dropdown, List, Tag, Typography, Empty, Space, message, notification } from 'antd'
import { BellOutlined, CheckOutlined } from '@ant-design/icons'
import { getUnreadNotifications, markAllRead, markAsRead } from '../api/notifications'
import { useNotificationSocket } from '../hooks/useNotificationSocket'

const { Text } = Typography

const TYPE_COLOR = {
  OEE_LOW: 'red',
  AVAILABILITY_LOW: 'volcano',
  PERFORMANCE_LOW: 'orange',
  QUALITY_LOW: 'gold',
}

const formatDate = (iso) => iso ? new Date(iso).toLocaleString('ru-RU') : ''

function NotificationBell() {
  const [items, setItems] = useState([])
  const [open, setOpen] = useState(false)
  const [api, contextHolder] = notification.useNotification()

  const reload = async () => {
    try {
      const res = await getUnreadNotifications()
      setItems(res.data || [])
    } catch (e) {}
  }

  useEffect(() => { reload() }, [])

  useNotificationSocket(
    (created) => {
      // приходят свежие — добавим в начало и покажем toast по первой
      setItems(prev => [...created, ...prev])
      const first = created[0]
      if (first) {
        api.warning({
          message: first.metricName ? `KPI: ${first.metricName}` : 'Новое уведомление',
          description: first.message,
          duration: 4,
          placement: 'topRight',
        })
      }
    },
    () => setItems([])
  )

  const handleMarkOne = async (id) => {
    try {
      await markAsRead(id)
      setItems(prev => prev.filter(n => n.id !== id))
    } catch (e) { message.error('Не удалось пометить как прочитанное') }
  }

  const handleMarkAll = async () => {
    try {
      await markAllRead()
      setItems([])
    } catch (e) { message.error('Не удалось пометить все') }
  }

  const dropdownContent = (
    <div style={{
      width: 380, maxHeight: 460, overflowY: 'auto',
      background: '#fff', borderRadius: 8,
      boxShadow: '0 6px 24px rgba(0,0,0,0.12)', padding: 8,
    }}>
      <Space style={{ width: '100%', justifyContent: 'space-between', padding: '4px 8px' }}>
        <Text strong>Уведомления ({items.length})</Text>
        {items.length > 0 && (
          <Button type="link" size="small" icon={<CheckOutlined />} onClick={handleMarkAll}>
            Прочитать все
          </Button>
        )}
      </Space>
      {items.length === 0 ? (
        <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="Нет новых" />
      ) : (
        <List
          size="small"
          dataSource={items.slice(0, 30)}
          renderItem={(n) => (
            <List.Item
              actions={[
                <Button type="text" size="small" icon={<CheckOutlined />}
                  onClick={() => handleMarkOne(n.id)} key="read" />
              ]}
            >
              <List.Item.Meta
                title={
                  <Space>
                    <Tag color={TYPE_COLOR[n.type] || 'default'}>{n.type}</Tag>
                    <Text>{n.lineId} / смена #{n.shiftId}</Text>
                  </Space>
                }
                description={
                  <>
                    <div>{n.message}</div>
                    <Text type="secondary" style={{ fontSize: 11 }}>
                      {formatDate(n.createdAt)}
                    </Text>
                  </>
                }
              />
            </List.Item>
          )}
        />
      )}
    </div>
  )

  return (
    <>
      {contextHolder}
      <Dropdown
        open={open}
        onOpenChange={setOpen}
        trigger={['click']}
        placement="bottomRight"
        popupRender={() => dropdownContent}
      >
        <Badge count={items.length} overflowCount={99}>
          <Button shape="circle" icon={<BellOutlined />} />
        </Badge>
      </Dropdown>
    </>
  )
}

export default NotificationBell
