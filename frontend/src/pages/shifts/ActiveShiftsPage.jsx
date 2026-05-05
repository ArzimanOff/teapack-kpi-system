import React, { useEffect, useState } from 'react'
import {
  Card, Table, Button, Space, Typography, Tag, Modal, message,
  Tooltip, Row, Col, Statistic, Empty, Select
} from 'antd'
import {
  PlayCircleOutlined, StopOutlined, ReloadOutlined,
  ExclamationCircleOutlined, EyeOutlined,
} from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import { findShifts, closeShift, getShiftData } from '../../api/shifts'
import { stopEmulator } from '../../api/events'
import { useLines } from '../../hooks/useLines'

const { Title, Text } = Typography

const formatDate = (iso) => iso ? new Date(iso).toLocaleString('ru-RU') : '—'
const formatNumber = (v, d = 1) => v == null ? '—' : Number(v).toFixed(d)

function durationMin(startIso) {
  if (!startIso) return null
  const ms = Date.now() - new Date(startIso).getTime()
  return ms > 0 ? Math.floor(ms / 60000) : 0
}

function ActiveShiftsPage() {
  const navigate = useNavigate()
  const { options: lineOptions } = useLines(true)
  const [rows, setRows] = useState([])     // [{shift, data}]
  const [loading, setLoading] = useState(false)
  const [closingId, setClosingId] = useState(null)
  const [lineFilter, setLineFilter] = useState(undefined)

  const load = async () => {
    setLoading(true)
    try {
      const params = { status: 'ACTIVE', size: 100, page: 0 }
      if (lineFilter) params.lineId = lineFilter
      const res = await findShifts(params)
      const shifts = res.data?.content || []
      // Параллельно тянем агрегаты для каждой смены
      const enriched = await Promise.all(shifts.map(async (s) => {
        try {
          const d = await getShiftData(s.id)
          return { shift: s, data: d.data }
        } catch (e) {
          return { shift: s, data: null }
        }
      }))
      setRows(enriched)
    } catch (e) {
      message.error('Не удалось загрузить активные смены')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [lineFilter])

  // Обновляем таблицу каждые 10 секунд — для динамики метрик и длительности
  useEffect(() => {
    const t = setInterval(load, 10000)
    return () => clearInterval(t)
  }, [lineFilter])

  const handleClose = (row) => {
    Modal.confirm({
      title: `Закрыть смену #${row.shift.id}?`,
      icon: <ExclamationCircleOutlined />,
      content: (
        <div>
          <div>Линия: <b>{row.shift.lineId}</b></div>
          <div>После закрытия данные будут заблокированы и рассчитаются KPI.</div>
        </div>
      ),
      okText: 'Закрыть смену',
      okButtonProps: { danger: true },
      cancelText: 'Отмена',
      onOk: async () => {
        setClosingId(row.shift.id)
        try {
          // Если эта смена сейчас в эмуляторе — корректно остановим
          try { await stopEmulator(row.shift.id) } catch (e) {}
          await closeShift(row.shift.id)
          message.success(`Смена #${row.shift.id} закрыта`)
          load()
        } catch (e) {
          message.error('Не удалось закрыть смену')
        } finally {
          setClosingId(null)
        }
      }
    })
  }

  const handleOpen = (row) => navigate(`/operator?shiftId=${row.shift.id}`)

  const columns = [
    { title: 'Смена', dataIndex: ['shift', 'id'], width: 80,
      render: (v) => <Text strong>#{v}</Text> },
    { title: 'Линия', dataIndex: ['shift', 'lineId'], width: 110,
      render: (v) => <Tag color="blue">{v}</Tag> },
    { title: 'Оператор', dataIndex: ['shift', 'operatorId'], width: 100,
      render: (v) => v ? `#${v}` : '—' },
    {
      title: 'Старт',
      key: 'start',
      width: 170,
      render: (_, row) => formatDate(row.shift.actualStart || row.shift.plannedStart),
    },
    {
      title: 'Длительность',
      key: 'duration',
      width: 130,
      render: (_, row) => {
        const m = durationMin(row.shift.actualStart || row.shift.plannedStart)
        if (m == null) return '—'
        const h = Math.floor(m / 60), mm = m % 60
        return `${h}ч ${mm}м`
      },
    },
    {
      title: 'Выпуск',
      key: 'output',
      width: 140,
      align: 'right',
      render: (_, row) => row.data
        ? <Text>{row.data.totalOutput} / {row.data.plannedOutput ?? '—'}</Text>
        : '—',
    },
    {
      title: 'Брак',
      key: 'scrap',
      width: 90,
      align: 'right',
      render: (_, row) => row.data?.scrapCount ?? '—',
    },
    {
      title: 'Простой, мин',
      key: 'downtime',
      width: 130,
      align: 'right',
      render: (_, row) => formatNumber(row.data?.downtimeMinutes),
    },
    {
      title: 'Скорость',
      key: 'speed',
      width: 140,
      align: 'right',
      render: (_, row) => row.data
        ? `${formatNumber(row.data.avgSpeed)} / ${formatNumber(row.data.nominalSpeed)}`
        : '—',
    },
    {
      title: 'Действия',
      key: 'actions',
      width: 220,
      render: (_, row) => (
        <Space>
          <Tooltip title="Открыть в кабинете оператора">
            <Button size="small" icon={<EyeOutlined />} onClick={() => handleOpen(row)}>
              Открыть
            </Button>
          </Tooltip>
          <Button
            size="small"
            danger
            icon={<StopOutlined />}
            loading={closingId === row.shift.id}
            onClick={() => handleClose(row)}
          >
            Закрыть смену
          </Button>
        </Space>
      ),
    },
  ]

  const totals = rows.reduce((acc, r) => {
    if (!r.data) return acc
    return {
      output: acc.output + (r.data.totalOutput || 0),
      scrap: acc.scrap + (r.data.scrapCount || 0),
      downtime: acc.downtime + Number(r.data.downtimeMinutes || 0),
    }
  }, { output: 0, scrap: 0, downtime: 0 })

  return (
    <div className="page-container">
      <Title level={3}>Активные смены</Title>

      <Card style={{ marginBottom: 16 }}>
        <Row gutter={16} align="middle">
          <Col flex="auto">
            <Space wrap>
              <Select
                allowClear
                placeholder="Все линии"
                style={{ width: 240 }}
                value={lineFilter}
                onChange={setLineFilter}
                options={lineOptions}
              />
              <Button icon={<ReloadOutlined />} onClick={load} loading={loading}>
                Обновить
              </Button>
              <Tooltip title="Авто-обновление каждые 10 секунд">
                <Tag color="green">live</Tag>
              </Tooltip>
            </Space>
          </Col>
          <Col>
            <Space size="large">
              <Statistic title="Активных смен" value={rows.length} />
              <Statistic title="Общий выпуск" value={totals.output} suffix="шт" />
              <Statistic title="Брак" value={totals.scrap} suffix="шт" />
              <Statistic title="Простой" value={totals.downtime.toFixed(0)} suffix="мин" />
            </Space>
          </Col>
        </Row>
      </Card>

      <Card>
        {rows.length === 0 && !loading
          ? <Empty description="Нет активных смен" />
          : (
            <Table
              rowKey={(r) => r.shift.id}
              loading={loading}
              dataSource={rows}
              columns={columns}
              pagination={false}
              scroll={{ x: 1300 }}
            />
          )}
      </Card>
    </div>
  )
}

export default ActiveShiftsPage
