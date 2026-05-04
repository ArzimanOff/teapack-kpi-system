import React, { useState, useEffect, useMemo } from 'react'
import {
  Card, Row, Col, Statistic, Select, Typography, Tabs,
  Badge, Alert, Spin, Progress, Table, Tag, Empty, Space, Divider, Descriptions
} from 'antd'
import {
  LineChart, Line, XAxis, YAxis, CartesianGrid,
  Tooltip, ResponsiveContainer, BarChart, Bar, Legend
} from 'recharts'
import { getKpiByLine } from '../../api/kpi'
import { findShifts, getShiftData } from '../../api/shifts'
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

const formatDate = (iso) => iso ? new Date(iso).toLocaleString('ru-RU') : '—'
const fmtPct = (v) => v == null ? '—' : `${(Number(v) * 100).toFixed(1)}%`
const fmtNum = (v, d = 1) => v == null ? '—' : Number(v).toFixed(d)

function OnlineShiftPanel({ lineId, latestKpi, connected }) {
  const [shift, setShift] = useState(null)
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  const loadActive = async () => {
    if (!lineId) return
    setLoading(true)
    setError(null)
    try {
      const res = await findShifts({ status: 'ACTIVE', lineId, page: 0, size: 1 })
      const found = res.data?.content?.[0]
      setShift(found || null)
      if (found) {
        const d = await getShiftData(found.id)
        setData(d.data)
      } else {
        setData(null)
      }
    } catch (e) {
      setError('Не удалось загрузить активную смену')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    setShift(null)
    setData(null)
    loadActive()
  }, [lineId])

  useEffect(() => {
    if (!shift?.id) return
    const interval = setInterval(async () => {
      try {
        const d = await getShiftData(shift.id)
        setData(d.data)
      } catch (e) {}
    }, 5000)
    return () => clearInterval(interval)
  }, [shift?.id])

  if (loading && !shift) return <Spin size="large" />
  if (error) return <Alert type="error" message={error} />
  if (!shift) {
    return (
      <Empty description={
        <Space direction="vertical">
          <Text type="secondary">На выбранной линии нет активной смены</Text>
          <Text type="secondary" style={{ fontSize: 12 }}>
            Создайте и запустите смену в кабинете оператора
          </Text>
        </Space>
      } />
    )
  }

  // Оценочные KPI на лету (для CLOSED — приходят через WebSocket в latestKpi)
  const planned = data?.plannedOutput ?? shift.plannedOutput
  const total = data?.totalOutput ?? 0
  const good = data?.goodOutput ?? 0
  const scrap = data?.scrapCount ?? 0
  const downtime = Number(data?.downtimeMinutes ?? 0)
  const stops = data?.numberOfStops ?? 0
  const avgSpeed = Number(data?.avgSpeed ?? 0)
  const nominalSpeed = Number(data?.nominalSpeed ?? shift.nominalSpeed ?? 0)

  const planFulfillment = planned > 0 ? total / planned : 0
  const quality = total > 0 ? good / total : 1
  const performance = nominalSpeed > 0 ? Math.min(avgSpeed / nominalSpeed, 1) : 0

  return (
    <>
      <Card style={{ marginBottom: 16 }}>
        <Row align="middle" justify="space-between">
          <Col>
            <Space split={<Divider type="vertical" />}>
              <Text strong>Смена #{shift.id}</Text>
              <Tag color="green">ACTIVE</Tag>
              <Text>Линия: {shift.lineId}</Text>
              <Text type="secondary">
                Начата: {formatDate(shift.actualStart || shift.plannedStart)}
              </Text>
            </Space>
          </Col>
          <Col>
            <Badge status={connected ? 'success' : 'error'}
              text={connected ? 'WS online' : 'WS offline'} />
          </Col>
        </Row>
      </Card>

      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col span={6}>
          <Card><Statistic title="Выпуск" value={total} suffix={`/ ${planned ?? '—'}`} /></Card>
        </Col>
        <Col span={6}>
          <Card><Statistic title="Годных" value={good} suffix="шт" /></Card>
        </Col>
        <Col span={6}>
          <Card><Statistic title="Брак" value={scrap} suffix="шт" /></Card>
        </Col>
        <Col span={6}>
          <Card><Statistic title="Простой" value={downtime.toFixed(1)} suffix="мин" /></Card>
        </Col>
      </Row>

      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col span={6}>
          <KpiGauge title="Качество (live)" value={quality} threshold={0.95} />
        </Col>
        <Col span={6}>
          <KpiGauge title="Производительность (оценка)" value={performance} threshold={0.75} />
        </Col>
        <Col span={6}>
          <KpiGauge title="Выполнение плана" value={planFulfillment} threshold={0.85} />
        </Col>
        <Col span={6}>
          <Card style={{ textAlign: 'center' }}>
            <Text type="secondary">Скорость</Text>
            <div style={{ marginTop: 16 }}>
              <Statistic value={fmtNum(avgSpeed)} suffix={`/ ${fmtNum(nominalSpeed)}`} />
              <Text type="secondary">факт. / номин., шт/мин</Text>
            </div>
          </Card>
        </Col>
      </Row>

      <Card title="Прочие показатели смены" size="small">
        <Descriptions column={2} size="small" bordered>
          <Descriptions.Item label="Остановок">{stops}</Descriptions.Item>
          <Descriptions.Item label="Средн. длительность остановки, мин">
            {stops > 0 ? (downtime / stops).toFixed(1) : '—'}
          </Descriptions.Item>
          <Descriptions.Item label="Output Rate, шт/мин">
            {fmtNum(avgSpeed)}
          </Descriptions.Item>
          <Descriptions.Item label="Scrap Rate">
            {fmtPct(total > 0 ? scrap / total : 0)}
          </Descriptions.Item>
        </Descriptions>
      </Card>

      {latestKpi && (
        <Card title="Последний расчёт KPI (получен по WebSocket)" size="small"
          style={{ marginTop: 16 }}>
          <Row gutter={16}>
            <Col span={6}><Statistic title="OEE" value={fmtPct(latestKpi.oee)} /></Col>
            <Col span={6}><Statistic title="Avail" value={fmtPct(latestKpi.availability)} /></Col>
            <Col span={6}><Statistic title="Perf" value={fmtPct(latestKpi.performance)} /></Col>
            <Col span={6}><Statistic title="Qual" value={fmtPct(latestKpi.quality)} /></Col>
          </Row>
          <Text type="secondary" style={{ fontSize: 12 }}>
            * KPI считается при закрытии смены. Здесь — последнее значение, пришедшее на эту линию.
          </Text>
        </Card>
      )}
    </>
  )
}

function LineSummaryPanel({ lineId }) {
  const [kpiList, setKpiList] = useState([])
  const [loading, setLoading] = useState(false)
  const [notifications, setNotifications] = useState([])

  useEffect(() => {
    if (!lineId) {
      setKpiList([])
      setNotifications([])
      return
    }
    let cancelled = false
    const load = async () => {
      setLoading(true)
      try {
        const res = await getKpiByLine(lineId)
        if (cancelled) return
        const list = res.data || []
        setKpiList(list)
        if (list.length > 0) {
          try {
            const n = await getNotificationsByShift(list[0].shiftId)
            if (!cancelled) setNotifications(n.data || [])
          } catch (e) { if (!cancelled) setNotifications([]) }
        } else {
          setNotifications([])
        }
      } catch (e) {
        if (!cancelled) setKpiList([])
      } finally {
        if (!cancelled) setLoading(false)
      }
    }
    load()
    return () => { cancelled = true }
  }, [lineId])

  const summary = useMemo(() => {
    if (!kpiList.length) return null
    const avg = (k) => kpiList.reduce((s, x) => s + (Number(x[k]) || 0), 0) / kpiList.length
    const sum = (k) => kpiList.reduce((s, x) => s + (Number(x[k]) || 0), 0)
    return {
      shifts: kpiList.length,
      avgOee: avg('oee'),
      avgAvail: avg('availability'),
      avgPerf: avg('performance'),
      avgQual: avg('quality'),
      totalOutput: sum('totalOutput'),
      totalGood: sum('goodOutput'),
      totalScrap: sum('scrapCount'),
      totalDowntime: sum('downtime'),
      totalStops: sum('numberOfStops'),
    }
  }, [kpiList])

  const chartData = useMemo(() =>
    [...kpiList].reverse().map((k) => ({
      name: `#${k.shiftId}`,
      OEE: Math.round((k.oee || 0) * 100),
      Доступность: Math.round((k.availability || 0) * 100),
      Производительность: Math.round((k.performance || 0) * 100),
      Качество: Math.round((k.quality || 0) * 100),
    })),
  [kpiList])

  const tableColumns = [
    { title: 'Смена', dataIndex: 'shiftId', width: 80 },
    {
      title: 'Дата',
      dataIndex: 'calculatedAt',
      width: 170,
      render: formatDate,
    },
    {
      title: 'OEE',
      dataIndex: 'oee',
      width: 90,
      render: (v) => fmtPct(v),
    },
    { title: 'Avail', dataIndex: 'availability', width: 90, render: fmtPct },
    { title: 'Perf', dataIndex: 'performance', width: 90, render: fmtPct },
    { title: 'Qual', dataIndex: 'quality', width: 90, render: fmtPct },
    { title: 'Выпуск', dataIndex: 'totalOutput', width: 90 },
    { title: 'Брак', dataIndex: 'scrapCount', width: 80 },
  ]

  const notifColumns = [
    { title: 'Тип', dataIndex: 'type', key: 'type',
      render: t => <Tag color="orange">{t}</Tag> },
    { title: 'Сообщение', dataIndex: 'message', key: 'message' },
    { title: 'Время', dataIndex: 'createdAt', key: 'createdAt',
      render: formatDate },
  ]

  if (loading) return <Spin size="large" />
  if (!summary) {
    return <Empty description="Нет завершённых смен по выбранной линии" />
  }

  return (
    <>
      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col span={6}>
          <KpiGauge title="Средний OEE" value={summary.avgOee} threshold={0.65} />
        </Col>
        <Col span={6}>
          <KpiGauge title="Средняя доступность" value={summary.avgAvail} threshold={0.80} />
        </Col>
        <Col span={6}>
          <KpiGauge title="Средняя производительность" value={summary.avgPerf} threshold={0.75} />
        </Col>
        <Col span={6}>
          <KpiGauge title="Среднее качество" value={summary.avgQual} threshold={0.95} />
        </Col>
      </Row>

      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col span={6}>
          <Card><Statistic title="Смен (CLOSED)" value={summary.shifts} /></Card>
        </Col>
        <Col span={6}>
          <Card><Statistic title="Суммарный выпуск" value={summary.totalOutput} suffix="шт" /></Card>
        </Col>
        <Col span={6}>
          <Card><Statistic title="Суммарный брак" value={summary.totalScrap} suffix="шт" /></Card>
        </Col>
        <Col span={6}>
          <Card><Statistic title="Суммарный простой"
            value={Number(summary.totalDowntime).toFixed(0)} suffix="мин" /></Card>
        </Col>
      </Row>

      {chartData.length > 0 && (
        <Row gutter={16} style={{ marginBottom: 16 }}>
          <Col span={12}>
            <Card title="OEE по сменам, %">
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
            <Card title="Компоненты KPI, %">
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

      <Card title="Завершённые смены по линии" size="small" style={{ marginBottom: 16 }}>
        <Table
          rowKey="shiftId"
          dataSource={kpiList}
          columns={tableColumns}
          pagination={{ pageSize: 10 }}
          size="small"
        />
      </Card>

      {notifications.length > 0 && (
        <Card title="Уведомления последней смены" size="small">
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
  )
}

function DashboardPage() {
  const { options: lineOptions, lines } = useLines(true)
  const [selectedLine, setSelectedLine] = useState(undefined)
  const [latestKpi, setLatestKpi] = useState(null)
  const [activeTab, setActiveTab] = useState('online')

  useEffect(() => {
    if (!selectedLine && lines.length > 0) {
      setSelectedLine(lines[0].code)
    }
  }, [lines, selectedLine])

  // Сбрасываем последний WS-KPI при смене линии — иначе показывается чужой
  useEffect(() => {
    setLatestKpi(null)
  }, [selectedLine])

  const { connected } = useWebSocket(selectedLine, (data) => {
    if (data?.lineId && data.lineId !== selectedLine) return
    setLatestKpi(data)
  })

  return (
    <div className="page-container">
      <Row justify="space-between" align="middle" style={{ marginBottom: 16 }}>
        <Title level={3} style={{ margin: 0 }}>Дашборд KPI</Title>
        <Row align="middle" gutter={16}>
          <Col>
            <Badge status={connected ? 'success' : 'error'}
              text={connected ? 'WS online' : 'WS offline'} />
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

      <Tabs
        activeKey={activeTab}
        onChange={setActiveTab}
        items={[
          {
            key: 'online',
            label: 'Текущая смена (online)',
            children: <OnlineShiftPanel
              lineId={selectedLine}
              latestKpi={latestKpi}
              connected={connected}
            />,
          },
          {
            key: 'summary',
            label: 'Сводка по линии',
            children: <LineSummaryPanel lineId={selectedLine} />,
          },
        ]}
      />
    </div>
  )
}

export default DashboardPage
