import React, { useEffect, useState } from 'react'
import {
  Card, Table, Form, Input, DatePicker, Select, Space, Button,
  Typography, Tag, Row, Col, message
} from 'antd'
import { FilterOutlined, ReloadOutlined } from '@ant-design/icons'
import { findAuditEvents } from '../../api/audit'

const { Title, Text } = Typography
const { RangePicker } = DatePicker

const ACTION_OPTIONS = [
  { value: 'SHIFT_CREATE', label: 'Создание смены' },
  { value: 'SHIFT_START', label: 'Старт смены' },
  { value: 'SHIFT_CLOSE', label: 'Закрытие смены' },
  { value: 'SHIFT_CANCEL', label: 'Отмена смены' },
  { value: 'LINE_CREATE', label: 'Создание линии' },
  { value: 'LINE_UPDATE', label: 'Изменение линии' },
  { value: 'LINE_DEACTIVATE', label: 'Деактивация линии' },
]

const ACTION_COLOR = {
  SHIFT_CREATE: 'blue',
  SHIFT_START: 'green',
  SHIFT_CLOSE: 'gold',
  SHIFT_CANCEL: 'red',
  LINE_CREATE: 'cyan',
  LINE_UPDATE: 'purple',
  LINE_DEACTIVATE: 'volcano',
}

const TARGET_OPTIONS = [
  { value: 'Shift', label: 'Shift' },
  { value: 'ProductionLine', label: 'ProductionLine' },
]

const formatDate = (iso) => iso ? new Date(iso).toLocaleString('ru-RU') : '—'

function AuditPage() {
  const [form] = Form.useForm()
  const [data, setData] = useState([])
  const [total, setTotal] = useState(0)
  const [loading, setLoading] = useState(false)
  const [pagination, setPagination] = useState({ current: 1, pageSize: 25 })

  const buildParams = (values = {}) => {
    const params = {}
    if (values.actor) params.actor = values.actor
    if (values.action) params.action = values.action
    if (values.targetType) params.targetType = values.targetType
    if (values.targetId) params.targetId = values.targetId
    if (values.dateRange?.length === 2) {
      params.dateFrom = values.dateRange[0].toISOString()
      params.dateTo = values.dateRange[1].toISOString()
    }
    return params
  }

  const load = async (values = form.getFieldsValue(), page = 1, size = pagination.pageSize) => {
    setLoading(true)
    try {
      const params = buildParams(values)
      params.page = page - 1
      params.size = size
      const res = await findAuditEvents(params)
      const body = res.data || {}
      setData(body.content || [])
      setTotal(body.totalElements ?? 0)
      setPagination({ current: page, pageSize: size })
    } catch (e) {
      message.error('Не удалось загрузить аудит-журнал')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load({}, 1, pagination.pageSize) }, [])

  const handleApply = (values) => load(values, 1, pagination.pageSize)
  const handleReset = () => { form.resetFields(); load({}, 1, pagination.pageSize) }
  const handleTableChange = (next) =>
    load(form.getFieldsValue(), next.current, next.pageSize)

  const columns = [
    {
      title: 'Время',
      dataIndex: 'occurredAt',
      width: 170,
      render: formatDate,
      sorter: (a, b) => new Date(a.occurredAt) - new Date(b.occurredAt),
      defaultSortOrder: 'descend',
    },
    {
      title: 'Пользователь',
      dataIndex: 'actor',
      width: 140,
      render: (v, r) => (
        <Space size={4} direction="vertical">
          <Text strong>{v || '—'}</Text>
          {r.actorRole && <Tag style={{ fontSize: 11 }}>{r.actorRole.replace('ROLE_', '')}</Tag>}
        </Space>
      ),
    },
    {
      title: 'Действие',
      dataIndex: 'action',
      width: 170,
      render: (v) => <Tag color={ACTION_COLOR[v] || 'default'}>{v}</Tag>,
    },
    {
      title: 'Объект',
      key: 'target',
      width: 200,
      render: (_, r) => <Text>{r.targetType} #{r.targetId ?? '—'}</Text>,
    },
    { title: 'Подробности', dataIndex: 'details', ellipsis: true },
  ]

  return (
    <div className="page-container">
      <Title level={3}>Журнал аудита</Title>

      <Card style={{ marginBottom: 16 }} title={<Space><FilterOutlined />Фильтры</Space>}>
        <Form form={form} layout="vertical" onFinish={handleApply}>
          <Row gutter={16}>
            <Col xs={24} sm={12} md={6}>
              <Form.Item name="actor" label="Пользователь">
                <Input placeholder="username" allowClear />
              </Form.Item>
            </Col>
            <Col xs={24} sm={12} md={6}>
              <Form.Item name="action" label="Действие">
                <Select allowClear placeholder="Любое" options={ACTION_OPTIONS} />
              </Form.Item>
            </Col>
            <Col xs={24} sm={12} md={4}>
              <Form.Item name="targetType" label="Тип объекта">
                <Select allowClear placeholder="Любой" options={TARGET_OPTIONS} />
              </Form.Item>
            </Col>
            <Col xs={24} sm={12} md={4}>
              <Form.Item name="targetId" label="ID объекта">
                <Input placeholder="напр. 12" allowClear />
              </Form.Item>
            </Col>
            <Col xs={24} sm={24} md={4}>
              <Form.Item name="dateRange" label="Период">
                <RangePicker showTime style={{ width: '100%' }} />
              </Form.Item>
            </Col>
          </Row>
          <Space>
            <Button type="primary" htmlType="submit" icon={<FilterOutlined />}>
              Применить
            </Button>
            <Button onClick={handleReset} icon={<ReloadOutlined />}>
              Сбросить
            </Button>
          </Space>
        </Form>
      </Card>

      <Card>
        <Table
          rowKey="id"
          loading={loading}
          dataSource={data}
          columns={columns}
          onChange={handleTableChange}
          pagination={{
            current: pagination.current,
            pageSize: pagination.pageSize,
            total,
            showSizeChanger: true,
            showTotal: (t) => `Всего: ${t}`,
          }}
        />
      </Card>
    </div>
  )
}

export default AuditPage
