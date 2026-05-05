import React, { useEffect, useState } from 'react'
import {
  Card, Table, Button, Space, Typography, Tag, Modal, message,
  Tooltip, Select, InputNumber, Form, Popconfirm, Alert, Row, Col, Statistic
} from 'antd'
import {
  PlayCircleOutlined, StopOutlined, ReloadOutlined, ExperimentOutlined,
  ThunderboltOutlined, DeleteOutlined,
} from '@ant-design/icons'
import {
  getEmulatorRuns, stopEmulator, removeEmulatorRun, applyEmulatorScenario, startEmulator,
} from '../../api/events'
import { findShifts } from '../../api/shifts'

const { Title, Text } = Typography

const SCENARIOS = [
  { value: 'NORMAL',       label: 'Сбросить (NORMAL)',  color: 'default' },
  { value: 'SPEED_DROP',   label: 'Падение скорости',   color: 'orange'  },
  { value: 'SCRAP_BURST',  label: 'Всплеск брака',      color: 'red'     },
  { value: 'OUTLIER',      label: 'Outlier-показания',  color: 'purple'  },
]
const SCENARIO_COLOR = Object.fromEntries(SCENARIOS.map(s => [s.value, s.color]))
const SCENARIO_LABEL = Object.fromEntries(SCENARIOS.map(s => [s.value, s.label]))

const formatDate = (iso) => iso ? new Date(iso).toLocaleString('ru-RU') : '—'
const formatNumber = (v, d = 1) => v == null ? '—' : Number(v).toFixed(d)

function EmulatorAdminPage() {
  const [runs, setRuns] = useState([])
  const [loading, setLoading] = useState(false)
  const [activeShifts, setActiveShifts] = useState([])
  const [startForm] = Form.useForm()

  const load = async () => {
    setLoading(true)
    try {
      const [r, s] = await Promise.all([
        getEmulatorRuns(),
        findShifts({ status: 'ACTIVE', size: 100, page: 0 }),
      ])
      setRuns(r.data || [])
      setActiveShifts(s.data?.content || [])
    } catch (e) {
      message.error('Не удалось загрузить состояние эмулятора')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [])
  useEffect(() => {
    const t = setInterval(load, 5000)
    return () => clearInterval(t)
  }, [])

  const handleStartFromForm = async (values) => {
    try {
      const shift = activeShifts.find(s => s.id === values.shiftId)
      await startEmulator(values.shiftId, shift?.lineId)
      message.success(`Эмулятор запущен для смены #${values.shiftId}`)
      startForm.resetFields()
      load()
    } catch (e) {
      message.error('Не удалось запустить эмулятор')
    }
  }

  const handleStop = async (shiftId) => {
    try {
      await stopEmulator(shiftId)
      message.success('Эмулятор остановлен')
      load()
    } catch (e) { message.error('Не удалось остановить') }
  }

  const handleRemove = async (shiftId) => {
    try {
      await removeEmulatorRun(shiftId)
      message.success('Запись удалена')
      load()
    } catch (e) { message.error('Не удалось удалить') }
  }

  const handleScenario = (run) => {
    let scenario = 'SPEED_DROP'
    let ticks = 5
    Modal.confirm({
      title: `Применить сценарий к смене #${run.shiftId}`,
      icon: <ExperimentOutlined />,
      content: (
        <Form layout="vertical" style={{ marginTop: 16 }}
          initialValues={{ scenario, ticks }}
          onValuesChange={(_, all) => { scenario = all.scenario; ticks = all.ticks }}
        >
          <Form.Item name="scenario" label="Сценарий">
            <Select options={SCENARIOS.map(s => ({ value: s.value, label: s.label }))} />
          </Form.Item>
          <Form.Item name="ticks" label="Сколько тиков (по 5 сек) применять">
            <InputNumber min={1} max={120} style={{ width: '100%' }} />
          </Form.Item>
          <Alert type="info" showIcon style={{ marginTop: 8 }} message={
            'SPEED_DROP — скорость 30-50% от номинала (Performance просядет). ' +
            'SCRAP_BURST — статус SCRAP в потоке. ' +
            'OUTLIER — заведомо невалидные показания (отбросятся ValidationService). ' +
            'NORMAL — сбросить активный сценарий.'
          } />
        </Form>
      ),
      okText: 'Применить',
      onOk: async () => {
        try {
          await applyEmulatorScenario(run.shiftId, scenario, ticks)
          message.success(`Сценарий ${scenario} применён к смене #${run.shiftId}`)
          load()
        } catch (e) {
          message.error('Не удалось применить сценарий')
        }
      }
    })
  }

  const columns = [
    { title: 'Смена', dataIndex: 'shiftId', width: 90,
      render: (v) => <Text strong>#{v}</Text> },
    { title: 'Линия', dataIndex: 'lineId', width: 110,
      render: (v) => <Tag color="blue">{v}</Tag> },
    {
      title: 'Статус',
      dataIndex: 'status',
      width: 110,
      render: (v) => <Tag color={v === 'RUNNING' ? 'green' : 'default'}>{v}</Tag>,
    },
    {
      title: 'Сценарий',
      key: 'scenario',
      width: 220,
      render: (_, r) => (
        <Space size={4}>
          <Tag color={SCENARIO_COLOR[r.scenario] || 'default'}>
            {SCENARIO_LABEL[r.scenario] || r.scenario}
          </Tag>
          {r.scenarioTicksLeft > 0 && (
            <Text type="secondary" style={{ fontSize: 12 }}>
              осталось {r.scenarioTicksLeft} тиков
            </Text>
          )}
        </Space>
      ),
    },
    {
      title: 'Скорость [мин..макс]',
      key: 'speedRange',
      width: 170,
      render: (_, r) => `${formatNumber(r.minSpeed)} … ${formatNumber(r.maxSpeed)}`,
    },
    { title: 'Тиков отправлено', dataIndex: 'ticks', width: 140, align: 'right' },
    { title: 'Старт', dataIndex: 'startedAt', width: 170, render: formatDate },
    {
      title: 'Действия',
      key: 'actions',
      width: 320,
      render: (_, r) => (
        <Space>
          <Button size="small" icon={<ExperimentOutlined />}
            onClick={() => handleScenario(r)}>
            Сценарий
          </Button>
          {r.status === 'RUNNING' ? (
            <Button size="small" danger icon={<StopOutlined />}
              onClick={() => handleStop(r.shiftId)}>
              Стоп
            </Button>
          ) : (
            <Tag color="default">остановлено</Tag>
          )}
          <Popconfirm
            title={`Удалить запись о смене #${r.shiftId}?`}
            description="Эмулятор забудет о смене. Данные в БД сохранятся."
            okText="Да" cancelText="Нет"
            onConfirm={() => handleRemove(r.shiftId)}
          >
            <Button size="small" icon={<DeleteOutlined />}>Забыть</Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  // Активные смены без запущенного эмулятора (можно поднять руками)
  const orphanShifts = activeShifts.filter(s =>
    !runs.find(r => r.shiftId === s.id))

  return (
    <div className="page-container">
      <Title level={3}>Управление эмулятором</Title>

      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 16 }}
        message="Эмулятор поддерживает несколько одновременно запущенных смен. Сценарии применяются на ближайшие N тиков (1 тик = интервал отправки, по умолчанию 5 секунд), полезны для демо: чтобы показать срабатывание уведомлений, outlier-detection и просадку KPI."
      />

      <Card style={{ marginBottom: 16 }}>
        <Row gutter={16} align="middle">
          <Col flex="auto">
            <Form layout="inline" form={startForm} onFinish={handleStartFromForm}>
              <Form.Item name="shiftId" label="Поднять для активной смены"
                rules={[{ required: true, message: 'выберите смену' }]}>
                <Select
                  placeholder="Активные смены без эмулятора"
                  style={{ width: 300 }}
                  options={orphanShifts.map(s => ({
                    value: s.id, label: `#${s.id} — ${s.lineId}`,
                  }))}
                  notFoundContent="Все активные смены уже подключены"
                />
              </Form.Item>
              <Form.Item>
                <Button type="primary" htmlType="submit" icon={<PlayCircleOutlined />}>
                  Запустить
                </Button>
              </Form.Item>
              <Form.Item>
                <Button icon={<ReloadOutlined />} onClick={load} loading={loading}>
                  Обновить
                </Button>
              </Form.Item>
            </Form>
          </Col>
          <Col>
            <Space size="large">
              <Statistic title="Всего" value={runs.length}
                prefix={<ThunderboltOutlined />} />
              <Statistic title="RUNNING"
                value={runs.filter(r => r.status === 'RUNNING').length}
                valueStyle={{ color: '#52c41a' }} />
              <Statistic title="Активных смен" value={activeShifts.length} />
            </Space>
          </Col>
        </Row>
      </Card>

      <Card>
        <Table
          rowKey="shiftId"
          loading={loading}
          dataSource={runs}
          columns={columns}
          pagination={false}
          locale={{ emptyText: 'Эмулятор не активен ни для одной смены' }}
          scroll={{ x: 1200 }}
        />
      </Card>
    </div>
  )
}

export default EmulatorAdminPage
