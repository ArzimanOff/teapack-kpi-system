import React, { useState, useEffect } from 'react'
import {
  Card, Row, Col, Statistic, Select, Typography,
  Badge, Alert, Spin, Progress, Table, Tag
} from 'antd'
import {
  LineChart, Line, XAxis, YAxis, CartesianGrid,
  Tooltip, ResponsiveContainer, BarChart, Bar, Legend
} from 'recharts'
import { getKpiByLine } from '../../api/kpi'
import { getNotificationsByShift } from '../../api/notifications'
import { useWebSocket } from '../../hooks/useWebSocket'
import { useLines } from '../../hooks/useLines'

const { Title, Text } = Typography

const kpiColor = (value, threshold) =>
  value >= threshold ? '#52c41a' : value >= threshold * 0.8 ? '#faad14' : '#ff4d4f'

function KpiGauge({ title, value, threshold }) {
  const pct = Math.round((value || 0) * 100)
  const color = kpiColor(value || 0, threshold)
  return (
    <Card style={{ textAlign: 'center' }}>
      <Text type="secondary">{title}</Text>
      <Progress
        type="dashboard"
        percent={pct}
        strokeColor={color}
        format={p => `${p}%`}
        style={{ marginTop: 8 }}
      />
    </Card>
  )
}

function DashboardPage() {
  const { options: lineOptions, lines } = useLines(true)
  const [selectedLine, setSelectedLine] = useState(undefined)
  const [kpiList, setKpiList] = useState([])
  const [latestKpi, setLatestKpi] = useState(null)
  const [notifications, setNotifications] = useState([])
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (!selectedLine && lines.length > 0) {
      setSelectedLine(lines[0].code)
    }
  }, [lines, selectedLine])

  const { connected } = useWebSocket(selectedLine, (data) => {
    setLatestKpi(data)
  })

  useEffect(() => {
    if (selectedLine) loadKpi()
  }, [selectedLine])

  const loadKpi = async () => {
    setLoading(true)
    try {
      const res = await getKpiByLine(selectedLine)
      const list = res.data || []
      setKpiList(list)
      if (list.length > 0) setLatestKpi(list[0])

      if (list.length > 0) {
        const notifRes = await getNotificationsByShift(list[0].shiftId)
        setNotifications(notifRes.data || [])
      }
    } catch (e) {
      console.error(e)
    } finally {
      setLoading(false)
    }
  }

  const chartData = [...kpiList].reverse().map((k, i) => ({
    name: `Смена ${k.shiftId}`,
    OEE: Math.round((k.oee || 0) * 100),
    Доступность: Math.round((k.availability || 0) * 100),
    Производительность: Math.round((k.performance || 0) * 100),
    Качество: Math.round((k.quality || 0) * 100),
  }))

  const notifColumns = [
    { title: 'Тип', dataIndex: 'type', key: 'type',
      render: t => <Tag color="orange">{t}</Tag> },
    { title: 'Сообщение', dataIndex: 'message', key: 'message' },
    { title: 'Время', dataIndex: 'createdAt', key: 'createdAt',
      render: t => new Date(t).toLocaleString('ru') },
  ]

  return (
    <div className="page-container">
      <Row justify="space-between" align="middle" style={{ marginBottom: 24 }}>
        <Title level={3} style={{ margin: 0 }}>Дашборд KPI</Title>
        <Row align="middle" gutter={16}>
          <Col>
            <Badge status={connected ? 'success' : 'error'}
              text={connected ? 'Online' : 'Offline'} />
          </Col>
          <Col>
            <Select
              value={selectedLine}
              onChange={setSelectedLine}
              style={{ width: 280 }}
              options={lineOptions}
              placeholder="Линия"
            />
          </Col>
        </Row>
      </Row>

      {loading ? <Spin size="large" /> : (
        <>
          {latestKpi && (
            <>
              <Row gutter={16} style={{ marginBottom: 24 }}>
                <Col span={6}>
                  <KpiGauge title="OEE" value={latestKpi.oee} threshold={0.65} />
                </Col>
                <Col span={6}>
                  <KpiGauge title="Доступность" value={latestKpi.availability} threshold={0.80} />
                </Col>
                <Col span={6}>
                  <KpiGauge title="Производительность" value={latestKpi.performance} threshold={0.75} />
                </Col>
                <Col span={6}>
                  <KpiGauge title="Качество" value={latestKpi.quality} threshold={0.95} />
                </Col>
              </Row>

              <Row gutter={16} style={{ marginBottom: 24 }}>
                <Col span={6}>
                  <Card>
                    <Statistic title="Выпуск" value={latestKpi.totalOutput} suffix="шт" />
                  </Card>
                </Col>
                <Col span={6}>
                  <Card>
                    <Statistic title="Брак" value={latestKpi.scrapCount} suffix="шт" />
                  </Card>
                </Col>
                <Col span={6}>
                  <Card>
                    <Statistic title="Простой"
                      value={Number(latestKpi.downtime || 0).toFixed(1)} suffix="мин" />
                  </Card>
                </Col>
                <Col span={6}>
                  <Card>
                    <Statistic title="Остановок" value={latestKpi.numberOfStops} />
                  </Card>
                </Col>
              </Row>
            </>
          )}

          {chartData.length > 0 && (
            <Row gutter={16} style={{ marginBottom: 24 }}>
              <Col span={12}>
                <Card title="OEE по сменам (%)">
                  <ResponsiveContainer width="100%" height={250}>
                    <LineChart data={chartData}>
                      <CartesianGrid strokeDasharray="3 3" />
                      <XAxis dataKey="name" />
                      <YAxis domain={[0, 100]} />
                      <Tooltip />
                      <Legend />
                      <Line type="monotone" dataKey="OEE" stroke="#1890ff" strokeWidth={2} />
                    </LineChart>
                  </ResponsiveContainer>
                </Card>
              </Col>
              <Col span={12}>
                <Card title="Компоненты KPI (%)">
                  <ResponsiveContainer width="100%" height={250}>
                    <BarChart data={chartData}>
                      <CartesianGrid strokeDasharray="3 3" />
                      <XAxis dataKey="name" />
                      <YAxis domain={[0, 100]} />
                      <Tooltip />
                      <Legend />
                      <Bar dataKey="Доступность" fill="#52c41a" />
                      <Bar dataKey="Производительность" fill="#1890ff" />
                      <Bar dataKey="Качество" fill="#722ed1" />
                    </BarChart>
                  </ResponsiveContainer>
                </Card>
              </Col>
            </Row>
          )}

          {notifications.length > 0 && (
            <Card title="Уведомления последней смены">
              <Table
                dataSource={notifications}
                columns={notifColumns}
                rowKey="id"
                pagination={false}
                size="small"
              />
            </Card>
          )}
        </>
      )}
    </div>
  )
}

export default DashboardPage