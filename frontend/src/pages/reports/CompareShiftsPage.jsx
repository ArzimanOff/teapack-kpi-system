import React, { useEffect, useMemo, useState } from 'react'
import {
  Card, Table, Select, DatePicker, Space, Button, Typography, Row, Col,
  message, Tag, Empty, Divider, Alert, Statistic,
} from 'antd'
import { ReloadOutlined, BarChartOutlined } from '@ant-design/icons'
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip,
  Legend, ResponsiveContainer, RadarChart, PolarGrid, PolarAngleAxis,
  PolarRadiusAxis, Radar,
} from 'recharts'
import { getKpiHistory } from '../../api/kpi'
import { useLines } from '../../hooks/useLines'
import KpiMetricLabel from '../../components/KpiMetricLabel'
import DowntimeTimelineModal from '../../components/DowntimeTimelineModal'

const { Title, Text } = Typography
const { RangePicker } = DatePicker

const fmtPct = (v) => v == null ? '—' : `${(Number(v) * 100).toFixed(1)}%`
const fmtNum = (v, d = 1) => v == null ? '—' : Number(v).toFixed(d)
const fmtDate = (iso) => iso ? new Date(iso).toLocaleString('ru-RU') : '—'

const KPI_COMPONENTS = [
  { key: 'oee',          label: 'OEE',          color: '#1890ff' },
  { key: 'availability', label: 'Availability', color: '#52c41a' },
  { key: 'performance',  label: 'Performance',  color: '#722ed1' },
  { key: 'quality',      label: 'Quality',      color: '#fa8c16' },
  { key: 'planFulfillment', label: 'PlanFulfillment', color: '#13c2c2' },
]

const MAX_SELECTED = 5

function CompareShiftsPage() {
  const { options: lineOptions } = useLines(false)
  const [lineId, setLineId] = useState(undefined)
  const [dateRange, setDateRange] = useState(null)
  const [candidates, setCandidates] = useState([])
  const [loading, setLoading] = useState(false)
  const [selectedKeys, setSelectedKeys] = useState([])
  const [selectedRows, setSelectedRows] = useState([])
  const [downtimeShiftId, setDowntimeShiftId] = useState(null)

  const loadCandidates = async () => {
    setLoading(true)
    try {
      const params = { page: 0, size: 50 }
      if (lineId) params.lineId = lineId
      if (dateRange?.length === 2) {
        params.dateFrom = dateRange[0].toISOString()
        params.dateTo = dateRange[1].toISOString()
      }
      const res = await getKpiHistory(params)
      setCandidates(res.data?.content || [])
    } catch (e) {
      message.error('Не удалось загрузить список смен')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { loadCandidates() }, [])

  const handleSelect = (keys, rows) => {
    if (keys.length > MAX_SELECTED) {
      message.warning(`Максимум ${MAX_SELECTED} смен для сравнения`)
      return
    }
    setSelectedKeys(keys)
    setSelectedRows(rows)
  }

  const handleReset = () => {
    setSelectedKeys([])
    setSelectedRows([])
  }

  const barData = useMemo(() => selectedRows.map(r => ({
    name: `#${r.shiftId}`,
    OEE: Math.round((r.oee || 0) * 100),
    Availability: Math.round((r.availability || 0) * 100),
    Performance: Math.round((r.performance || 0) * 100),
    Quality: Math.round((r.quality || 0) * 100),
    PlanFulfillment: Math.round((r.planFulfillment || 0) * 100),
  })), [selectedRows])

  // Радарная диаграмма: метрики по осям, серии = смены
  const radarData = useMemo(() => KPI_COMPONENTS.map(c => {
    const row = { metric: c.label }
    selectedRows.forEach(r => {
      row[`#${r.shiftId}`] = Math.round((r[c.key] || 0) * 100)
    })
    return row
  }), [selectedRows])

  const candidateColumns = [
    { title: 'ID', dataIndex: 'shiftId', width: 80 },
    { title: 'Линия', dataIndex: 'lineId', width: 120 },
    { title: 'Завершена', dataIndex: 'calculatedAt', width: 170, render: fmtDate },
    { title: <KpiMetricLabel metricKey="oee" label="OEE" />, dataIndex: 'oee', width: 100, align: 'right', render: fmtPct },
    { title: 'Avail', dataIndex: 'availability', width: 90, align: 'right', render: fmtPct },
    { title: 'Perf',  dataIndex: 'performance',  width: 90, align: 'right', render: fmtPct },
    { title: 'Qual',  dataIndex: 'quality',      width: 90, align: 'right', render: fmtPct },
    { title: 'Выпуск', dataIndex: 'totalOutput', width: 90, align: 'right' },
    { title: 'Простой, мин', dataIndex: 'downtime', width: 110, align: 'right', render: (v) => fmtNum(v, 1) },
  ]

  // Сравнительная таблица: строка = метрика, колонки = смены
  const compareRows = useMemo(() => [
    { metric: <KpiMetricLabel metricKey="oee" label="OEE" />,                 key: 'oee',           kind: 'pct' },
    { metric: <KpiMetricLabel metricKey="availability" label="Availability" />, key: 'availability', kind: 'pct' },
    { metric: <KpiMetricLabel metricKey="performance" label="Performance" />,   key: 'performance',  kind: 'pct' },
    { metric: <KpiMetricLabel metricKey="quality" label="Quality" />,           key: 'quality',      kind: 'pct' },
    { metric: <KpiMetricLabel metricKey="planFulfillment" label="Plan Fulfillment" />, key: 'planFulfillment', kind: 'pct' },
    { metric: <KpiMetricLabel metricKey="scrapRate" label="Scrap Rate" />,      key: 'scrapRate',    kind: 'pct', lowerIsBetter: true },
    { metric: <KpiMetricLabel metricKey="downtime" label="Простой, мин" />,     key: 'downtime',     kind: 'num1', lowerIsBetter: true },
    { metric: <KpiMetricLabel metricKey="numberOfStops" label="Остановки" />,   key: 'numberOfStops', kind: 'int', lowerIsBetter: true },
    { metric: <KpiMetricLabel metricKey="avgDowntime" label="Средн. длит. ост." />, key: 'avgDowntime', kind: 'num1', lowerIsBetter: true },
    { metric: <KpiMetricLabel metricKey="outputRate" label="Output Rate" />,    key: 'outputRate',   kind: 'num1' },
    { metric: <KpiMetricLabel metricKey="speedLoss" label="Speed Loss" />,      key: 'speedLoss',    kind: 'num1', lowerIsBetter: true },
    { metric: 'Выпуск, шт',                                                     key: 'totalOutput',  kind: 'int' },
    { metric: 'Брак, шт',                                                       key: 'scrapCount',   kind: 'int', lowerIsBetter: true },
  ], [])

  const bestForRow = (row) => {
    if (!selectedRows.length) return null
    let bestIdx = 0
    selectedRows.forEach((s, i) => {
      const cur = Number(s[row.key] ?? 0)
      const best = Number(selectedRows[bestIdx][row.key] ?? 0)
      if (row.lowerIsBetter ? cur < best : cur > best) bestIdx = i
    })
    return bestIdx
  }

  const fmtVal = (v, kind) => {
    if (v == null) return '—'
    if (kind === 'pct') return fmtPct(v)
    if (kind === 'num1') return fmtNum(v, 1)
    if (kind === 'int') return String(v)
    return String(v)
  }

  const compareColumns = useMemo(() => {
    const cols = [
      {
        title: 'Метрика',
        dataIndex: 'metric',
        key: 'metric',
        width: 240,
        fixed: 'left',
      },
    ]
    selectedRows.forEach((row, idx) => {
      cols.push({
        title: (
          <Space direction="vertical" size={0} align="center">
            <Text strong>#{row.shiftId}</Text>
            <Text type="secondary" style={{ fontSize: 11 }}>{row.lineId}</Text>
          </Space>
        ),
        key: `shift_${row.shiftId}`,
        align: 'center',
        render: (_, metricRow) => {
          const v = row[metricRow.key]
          const bestIdx = bestForRow(metricRow)
          const isBest = idx === bestIdx && selectedRows.length > 1
          return (
            <Space>
              <Text strong={isBest}>{fmtVal(v, metricRow.kind)}</Text>
              {isBest && <Tag color="green" style={{ marginInlineEnd: 0 }}>лучше</Tag>}
            </Space>
          )
        },
      })
    })
    return cols
  }, [selectedRows])

  return (
    <div className="page-container">
      <Title level={3}>Сравнение смен</Title>

      <Card style={{ marginBottom: 16 }}>
        <Space wrap>
          <Select
            allowClear
            placeholder="Все линии"
            value={lineId}
            onChange={setLineId}
            options={lineOptions}
            style={{ width: 260 }}
          />
          <RangePicker
            showTime
            value={dateRange}
            onChange={setDateRange}
            placeholder={['Завершена с', 'Завершена по']}
          />
          <Button type="primary" icon={<ReloadOutlined />} onClick={loadCandidates}>
            Найти смены
          </Button>
          {selectedKeys.length > 0 && (
            <Button onClick={handleReset}>
              Сбросить выбор ({selectedKeys.length})
            </Button>
          )}
        </Space>
      </Card>

      <Card
        title={
          <Space>
            <Text>Список смен (последние 50 завершённых)</Text>
            <Tag color="blue">Выбрано: {selectedKeys.length} / {MAX_SELECTED}</Tag>
          </Space>
        }
        style={{ marginBottom: 16 }}
      >
        <Table
          rowKey="shiftId"
          loading={loading}
          dataSource={candidates}
          columns={candidateColumns}
          pagination={{ pageSize: 10, showSizeChanger: false }}
          rowSelection={{
            type: 'checkbox',
            selectedRowKeys: selectedKeys,
            onChange: handleSelect,
          }}
          size="small"
        />
      </Card>

      {selectedRows.length === 0 && (
        <Alert
          showIcon
          type="info"
          message="Выберите от 2 до 5 смен для сравнения"
          description="Отметьте чекбоксы в таблице выше. Графики и таблица сравнения появятся ниже."
        />
      )}

      {selectedRows.length === 1 && (
        <Alert
          showIcon
          type="warning"
          message="Выберите ещё минимум одну смену"
        />
      )}

      {selectedRows.length >= 2 && (
        <>
          <Row gutter={16} style={{ marginBottom: 16 }}>
            <Col span={14}>
              <Card title={<Space><BarChartOutlined />Компоненты KPI по сменам, %</Space>}>
                <ResponsiveContainer width="100%" height={320}>
                  <BarChart data={barData}>
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis dataKey="name" />
                    <YAxis domain={[0, 100]} />
                    <Tooltip />
                    <Legend />
                    {KPI_COMPONENTS.map(c => (
                      <Bar key={c.key} dataKey={c.label} fill={c.color} />
                    ))}
                  </BarChart>
                </ResponsiveContainer>
              </Card>
            </Col>
            <Col span={10}>
              <Card title="Профиль смен (радар), %">
                <ResponsiveContainer width="100%" height={320}>
                  <RadarChart data={radarData}>
                    <PolarGrid />
                    <PolarAngleAxis dataKey="metric" />
                    <PolarRadiusAxis angle={30} domain={[0, 100]} />
                    {selectedRows.map((r, i) => (
                      <Radar
                        key={r.shiftId}
                        name={`#${r.shiftId}`}
                        dataKey={`#${r.shiftId}`}
                        stroke={KPI_COMPONENTS[i % KPI_COMPONENTS.length].color}
                        fill={KPI_COMPONENTS[i % KPI_COMPONENTS.length].color}
                        fillOpacity={0.25}
                      />
                    ))}
                    <Legend />
                    <Tooltip />
                  </RadarChart>
                </ResponsiveContainer>
              </Card>
            </Col>
          </Row>

          <Row gutter={16} style={{ marginBottom: 16 }}>
            {selectedRows.map(r => (
              <Col key={r.shiftId} span={Math.max(4, Math.floor(24 / selectedRows.length))}>
                <Card size="small" title={`Смена #${r.shiftId}`}>
                  <Statistic
                    title={<KpiMetricLabel metricKey="oee" label="OEE" />}
                    value={fmtPct(r.oee)}
                  />
                  <Divider style={{ margin: '8px 0' }} />
                  <Space direction="vertical" size={0} style={{ fontSize: 12 }}>
                    <Text type="secondary">Линия: {r.lineId}</Text>
                    <Text type="secondary">{fmtDate(r.calculatedAt)}</Text>
                  </Space>
                  <Divider style={{ margin: '8px 0' }} />
                  <Button
                    size="small"
                    type="link"
                    style={{ padding: 0 }}
                    onClick={() => setDowntimeShiftId(r.shiftId)}
                  >
                    Простои ({fmtNum(r.downtime, 1)} мин) →
                  </Button>
                </Card>
              </Col>
            ))}
          </Row>

          <Card title="Сравнение по метрикам (зелёным — лучший результат)">
            <Table
              rowKey="key"
              dataSource={compareRows}
              columns={compareColumns}
              pagination={false}
              size="small"
              scroll={{ x: 'max-content' }}
            />
          </Card>
        </>
      )}

      <DowntimeTimelineModal
        open={downtimeShiftId != null}
        shiftId={downtimeShiftId}
        onClose={() => setDowntimeShiftId(null)}
      />
    </div>
  )
}

export default CompareShiftsPage
