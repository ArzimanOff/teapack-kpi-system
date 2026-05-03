import React, { useState, useEffect } from 'react'
import {
  Card, Button, Form, Input, Select, InputNumber,
  Space, Tag, Typography, Row, Col, Statistic, Alert, Modal
} from 'antd'
import {
  PlayCircleOutlined, PauseCircleOutlined,
  StopOutlined, ExclamationCircleOutlined
} from '@ant-design/icons'
import { createShift, startShift, closeShift, getShift, getShiftAggregate } from '../../api/shifts'
import { sendOperatorEvent, startEmulator, stopEmulator } from '../../api/events'

const { Title, Text } = Typography
const { Option } = Select

const DOWNTIME_REASONS = [
  'Плановое ТО', 'Поломка оборудования', 'Отсутствие сырья',
  'Смена упаковки', 'Перерыв оператора', 'Другое'
]

function OperatorPage() {
  const [shift, setShift] = useState(null)
  const [aggregate, setAggregate] = useState(null)
  const [loading, setLoading] = useState(false)
  const [createForm] = Form.useForm()
  const [scrapForm] = Form.useForm()
  const [downtimeForm] = Form.useForm()

  useEffect(() => {
    if (shift?.id && shift.status === 'ACTIVE') {
      const interval = setInterval(loadAggregate, 5000)
      return () => clearInterval(interval)
    }
  }, [shift])

  const loadAggregate = async () => {
    if (!shift?.id) return
    try {
      const res = await getShiftAggregate(shift.id)
      setAggregate(res.data)
    } catch (e) {}
  }

  const handleCreateShift = async (values) => {
    setLoading(true)
    try {
      const res = await createShift({
        lineId: values.lineId,
        operatorId: 1,
        plannedStart: values.plannedStart,
        plannedEnd: values.plannedEnd,
        plannedOutput: values.plannedOutput,
        nominalSpeed: values.nominalSpeed,
      })
      setShift(res.data)
      createForm.resetFields()
    } catch (e) {
      Modal.error({ title: 'Ошибка', content: 'Не удалось создать смену' })
    } finally {
      setLoading(false)
    }
  }

  const handleStartShift = async () => {
    setLoading(true)
    try {
      const res = await startShift(shift.id)
      setShift(res.data)
      await startEmulator(shift.id)
      loadAggregate()
    } catch (e) {
      Modal.error({ title: 'Ошибка', content: 'Не удалось запустить смену' })
    } finally {
      setLoading(false)
    }
  }

  const handleStopLine = async () => {
    Modal.confirm({
      title: 'Остановка линии',
      icon: <ExclamationCircleOutlined />,
      content: (
        <Form form={downtimeForm} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item name="reason" label="Причина простоя" rules={[{ required: true }]}>
            <Select placeholder="Выберите причину">
              {DOWNTIME_REASONS.map(r => <Option key={r} value={r}>{r}</Option>)}
            </Select>
          </Form.Item>
        </Form>
      ),
      onOk: async () => {
        const values = downtimeForm.getFieldsValue()
        await sendOperatorEvent({
          shiftId: shift.id,
          lineId: shift.lineId,
          timestamp: new Date().toISOString().slice(0, 19),
          eventType: 'STOP',
          reason: values.reason,
          operatorId: 1,
        })
        await stopEmulator()
        downtimeForm.resetFields()
      }
    })
  }

  const handleStartLine = async () => {
    await sendOperatorEvent({
      shiftId: shift.id,
      lineId: shift.lineId,
      timestamp: new Date().toISOString().slice(0, 19),
      eventType: 'START',
      operatorId: 1,
    })
    await startEmulator(shift.id)
  }

  const handleScrap = async (values) => {
    await sendOperatorEvent({
      shiftId: shift.id,
      lineId: shift.lineId,
      timestamp: new Date().toISOString().slice(0, 19),
      eventType: 'SCRAP',
      scrapCount: values.scrapCount,
      comment: values.comment,
      operatorId: 1,
    })
    scrapForm.resetFields()
  }

  const handleCloseShift = async () => {
    Modal.confirm({
      title: 'Завершить смену?',
      content: 'После завершения смены данные будут заблокированы и рассчитаются KPI.',
      onOk: async () => {
        setLoading(true)
        try {
          await stopEmulator()
          const res = await closeShift(shift.id)
          setShift(res.data)
        } catch (e) {
          Modal.error({ title: 'Ошибка', content: 'Не удалось завершить смену' })
        } finally {
          setLoading(false)
        }
      }
    })
  }

  const statusColor = {
    PLANNED: 'blue', ACTIVE: 'green', CLOSED: 'default'
  }
  const statusLabel = {
    PLANNED: 'Запланирована', ACTIVE: 'Активна', CLOSED: 'Завершена'
  }

  return (
    <div className="page-container">
      <Title level={3}>Кабинет оператора</Title>

      {!shift && (
        <Card title="Создать смену" style={{ marginBottom: 24 }}>
          <Form form={createForm} onFinish={handleCreateShift} layout="vertical">
            <Row gutter={16}>
              <Col span={8}>
                <Form.Item name="lineId" label="Линия" rules={[{ required: true }]}>
                  <Select placeholder="Выберите линию">
                    <Option value="LINE-01">LINE-01</Option>
                    <Option value="LINE-02">LINE-02</Option>
                    <Option value="LINE-03">LINE-03</Option>
                  </Select>
                </Form.Item>
              </Col>
              <Col span={8}>
                <Form.Item name="plannedStart" label="Начало (ISO)" rules={[{ required: true }]}>
                  <Input placeholder="2026-05-03T08:00:00" />
                </Form.Item>
              </Col>
              <Col span={8}>
                <Form.Item name="plannedEnd" label="Конец (ISO)" rules={[{ required: true }]}>
                  <Input placeholder="2026-05-03T16:00:00" />
                </Form.Item>
              </Col>
              <Col span={8}>
                <Form.Item name="plannedOutput" label="План выпуска" rules={[{ required: true }]}>
                  <InputNumber min={1} style={{ width: '100%' }} />
                </Form.Item>
              </Col>
              <Col span={8}>
                <Form.Item name="nominalSpeed" label="Номинальная скорость" rules={[{ required: true }]}>
                  <InputNumber min={1} style={{ width: '100%' }} />
                </Form.Item>
              </Col>
            </Row>
            <Button type="primary" htmlType="submit" loading={loading}>
              Создать смену
            </Button>
          </Form>
        </Card>
      )}

      {shift && (
        <>
          <Card style={{ marginBottom: 24 }}>
            <Row align="middle" justify="space-between">
              <Col>
                <Space>
                  <Text strong>Смена #{shift.id}</Text>
                  <Text>Линия: {shift.lineId}</Text>
                  <Tag color={statusColor[shift.status]}>
                    {statusLabel[shift.status]}
                  </Tag>
                </Space>
              </Col>
              <Col>
                <Space>
                  {shift.status === 'PLANNED' && (
                    <Button type="primary" icon={<PlayCircleOutlined />}
                      onClick={handleStartShift} loading={loading}>
                      Начать смену
                    </Button>
                  )}
                  {shift.status === 'ACTIVE' && (
                    <>
                      <Button icon={<PauseCircleOutlined />} onClick={handleStopLine}>
                        Остановить линию
                      </Button>
                      <Button icon={<PlayCircleOutlined />} onClick={handleStartLine}>
                        Запустить линию
                      </Button>
                      <Button danger icon={<StopOutlined />}
                        onClick={handleCloseShift} loading={loading}>
                        Завершить смену
                      </Button>
                    </>
                  )}
                </Space>
              </Col>
            </Row>
          </Card>

          {aggregate && shift.status === 'ACTIVE' && (
            <Row gutter={16} style={{ marginBottom: 24 }}>
              <Col span={6}>
                <Card><Statistic title="Выпуск" value={aggregate.totalOutput} suffix="шт" /></Card>
              </Col>
              <Col span={6}>
                <Card><Statistic title="Годная продукция" value={aggregate.goodOutput} suffix="шт" /></Card>
              </Col>
              <Col span={6}>
                <Card><Statistic title="Брак" value={aggregate.scrapCount} suffix="шт" /></Card>
              </Col>
              <Col span={6}>
                <Card><Statistic title="Простой" value={Number(aggregate.downtimeMinutes).toFixed(1)} suffix="мин" /></Card>
              </Col>
            </Row>
          )}

          {shift.status === 'ACTIVE' && (
            <Card title="Зафиксировать брак">
              <Form form={scrapForm} onFinish={handleScrap} layout="inline">
                <Form.Item name="scrapCount" label="Количество брака" rules={[{ required: true }]}>
                  <InputNumber min={1} />
                </Form.Item>
                <Form.Item name="comment" label="Комментарий">
                  <Input placeholder="Причина брака" style={{ width: 200 }} />
                </Form.Item>
                <Form.Item>
                  <Button type="primary" htmlType="submit">Зафиксировать</Button>
                </Form.Item>
              </Form>
            </Card>
          )}
        </>
      )}
    </div>
  )
}

export default OperatorPage