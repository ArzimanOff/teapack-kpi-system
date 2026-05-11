import React, { useEffect, useState, useMemo } from 'react'
import {
  Modal, Timeline, Empty, Spin, Tag, Typography, Space,
  Statistic, Row, Col, Card, Alert, Tooltip
} from 'antd'
import { ClockCircleOutlined, StopOutlined, PlayCircleOutlined } from '@ant-design/icons'
import { getShiftDowntimes } from '../api/shifts'

const { Text } = Typography

const fmtDt = (iso) => iso ? new Date(iso).toLocaleString('ru-RU') : '—'
const fmtTime = (iso) => iso ? new Date(iso).toLocaleTimeString('ru-RU', { hour: '2-digit', minute: '2-digit', second: '2-digit' }) : '—'

function DowntimeTimelineModal({ open, shiftId, onClose }) {
  const [loading, setLoading] = useState(false)
  const [events, setEvents] = useState([])
  const [error, setError] = useState(null)

  useEffect(() => {
    if (!open || !shiftId) return
    let cancelled = false
    setLoading(true)
    setError(null)
    getShiftDowntimes(shiftId)
      .then((res) => { if (!cancelled) setEvents(res.data || []) })
      .catch(() => { if (!cancelled) setError('Не удалось загрузить простои') })
      .finally(() => { if (!cancelled) setLoading(false) })
    return () => { cancelled = true }
  }, [open, shiftId])

  const stats = useMemo(() => {
    if (!events.length) return { count: 0, total: 0, avg: 0, max: 0 }
    const durations = events.map(e => Number(e.durationMinutes || 0))
    const total = durations.reduce((a, b) => a + b, 0)
    return {
      count: events.length,
      total: total.toFixed(1),
      avg: (total / events.length).toFixed(1),
      max: Math.max(...durations).toFixed(1),
    }
  }, [events])

  const items = events.map((e) => ({
    key: e.id,
    color: e.endTime ? 'red' : 'orange',
    dot: e.endTime
      ? <StopOutlined style={{ fontSize: 16 }} />
      : <ClockCircleOutlined style={{ fontSize: 16, color: '#faad14' }} />,
    children: (
      <Card size="small" style={{ marginBottom: 4 }}>
        <Space direction="vertical" size={2} style={{ width: '100%' }}>
          <Space wrap>
            <Tag color={e.endTime ? 'red' : 'orange'}>
              {e.endTime ? 'Простой' : 'Простой (открыт)'}
            </Tag>
            <Tooltip title="Длительность">
              <Tag icon={<ClockCircleOutlined />} color="blue">
                {Number(e.durationMinutes || 0).toFixed(1)} мин
              </Tag>
            </Tooltip>
            {e.reason && <Text type="secondary">{e.reason}</Text>}
          </Space>
          <Space split={<Text type="secondary">→</Text>}>
            <Text>
              <StopOutlined style={{ color: '#ff4d4f', marginRight: 4 }} />
              {fmtTime(e.startTime)}
            </Text>
            <Text>
              <PlayCircleOutlined style={{ color: '#52c41a', marginRight: 4 }} />
              {e.endTime ? fmtTime(e.endTime) : <Text type="warning">в работе</Text>}
            </Text>
          </Space>
          <Text type="secondary" style={{ fontSize: 11 }}>
            {fmtDt(e.startTime)}
          </Text>
        </Space>
      </Card>
    ),
  }))

  return (
    <Modal
      title={`Журнал простоев — смена #${shiftId}`}
      open={open}
      onCancel={onClose}
      footer={null}
      width={680}
      destroyOnClose
    >
      {loading && <Spin size="large" style={{ display: 'block', margin: '40px auto' }} />}
      {error && <Alert type="error" message={error} />}
      {!loading && !error && (
        <>
          <Row gutter={16} style={{ marginBottom: 16 }}>
            <Col span={6}><Card size="small"><Statistic title="Кол-во" value={stats.count} /></Card></Col>
            <Col span={6}><Card size="small"><Statistic title="Сумма, мин" value={stats.total} /></Card></Col>
            <Col span={6}><Card size="small"><Statistic title="Средн., мин" value={stats.avg} /></Card></Col>
            <Col span={6}><Card size="small"><Statistic title="Максим., мин" value={stats.max} /></Card></Col>
          </Row>
          {events.length === 0 ? (
            <Empty description="За смену простоев не зарегистрировано" />
          ) : (
            <Timeline mode="left" items={items} />
          )}
        </>
      )}
    </Modal>
  )
}

export default DowntimeTimelineModal
