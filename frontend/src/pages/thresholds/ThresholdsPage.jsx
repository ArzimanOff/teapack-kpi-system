import React, { useEffect, useState } from 'react'
import {
  Card, Form, InputNumber, Button, Typography, Row, Col, message,
  Space, Divider, Alert, Tag, Table, Tooltip
} from 'antd'
import { SaveOutlined, ReloadOutlined, EditOutlined } from '@ant-design/icons'
import { getThresholds, updateThresholds } from '../../api/thresholds'
import { getLines, updateLine } from '../../api/lines'
import { useNavigate } from 'react-router-dom'
import { hasRole } from '../../utils/auth'
import { ROLES } from '../../constants/access'

const { Title, Text } = Typography

const formatDate = (iso) => iso ? new Date(iso).toLocaleString('ru-RU') : '—'
const formatNumber = (v) => v == null ? '—' : Number(v).toFixed(2)

function ThresholdsPage() {
  const navigate = useNavigate()
  const canEdit = hasRole([ROLES.TECHNOLOGIST, ROLES.ADMIN])
  const canEditLines = hasRole([ROLES.ADMIN])

  const [form] = Form.useForm()
  const [meta, setMeta] = useState(null) // {updatedAt, updatedBy}
  const [saving, setSaving] = useState(false)
  const [lines, setLines] = useState([])
  const [loading, setLoading] = useState(false)

  const loadAll = async () => {
    setLoading(true)
    try {
      const [t, l] = await Promise.all([getThresholds(), getLines(false)])
      const data = t.data
      form.setFieldsValue({
        oeeMin: Number((data.oeeMin || 0) * 100),
        availabilityMin: Number((data.availabilityMin || 0) * 100),
        performanceMin: Number((data.performanceMin || 0) * 100),
        qualityMin: Number((data.qualityMin || 0) * 100),
      })
      setMeta({ updatedAt: data.updatedAt, updatedBy: data.updatedBy })
      setLines(l.data || [])
    } catch (e) {
      message.error('Не удалось загрузить пороги')
    } finally { setLoading(false) }
  }

  useEffect(() => { loadAll() }, [])

  const handleSave = async (values) => {
    setSaving(true)
    try {
      await updateThresholds({
        oeeMin: values.oeeMin / 100,
        availabilityMin: values.availabilityMin / 100,
        performanceMin: values.performanceMin / 100,
        qualityMin: values.qualityMin / 100,
      })
      message.success('Пороги сохранены')
      loadAll()
    } catch (e) {
      message.error(e?.response?.data?.message || 'Не удалось сохранить')
    } finally { setSaving(false) }
  }

  const lineColumns = [
    { title: 'Код', dataIndex: 'code', width: 110 },
    { title: 'Название', dataIndex: 'name' },
    {
      title: 'Скорость [мин..макс]',
      key: 'speed',
      width: 200,
      render: (_, r) => `${formatNumber(r.minSpeed)} … ${formatNumber(r.maxSpeed)}`,
    },
    {
      title: 'Темп. [мин..макс]',
      key: 'temp',
      width: 200,
      render: (_, r) => `${formatNumber(r.minTemperature)} … ${formatNumber(r.maxTemperature)}`,
    },
    {
      title: 'Активна',
      dataIndex: 'isActive',
      width: 100,
      render: (v) => v ? <Tag color="green">Да</Tag> : <Tag>Нет</Tag>,
    },
    {
      title: '',
      key: 'edit',
      width: 130,
      render: (_, r) => (
        <Tooltip title={canEditLines ? 'Редактировать на странице линий' : 'Только админ'}>
          <Button size="small" icon={<EditOutlined />}
            disabled={!canEditLines}
            onClick={() => navigate('/admin/lines')}>
            Изменить
          </Button>
        </Tooltip>
      ),
    },
  ]

  return (
    <div className="page-container">
      <Title level={3}>Настройка порогов</Title>

      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 16 }}
        message="Глобальные KPI-пороги применяются ко всем сменам — при выходе за них формируются уведомления (см. колокольчик). Per-line outlier-пороги задаются индивидуально на странице «Линии (админ)» — выходящие за них показания помечаются is_valid=false и не попадают в агрегаты."
      />

      <Card title="Глобальные KPI-пороги" loading={loading} style={{ marginBottom: 24 }}>
        <Form form={form} layout="vertical" onFinish={handleSave} disabled={!canEdit}>
          <Row gutter={16}>
            <Col xs={12} md={6}>
              <Form.Item
                name="oeeMin"
                label="OEE ≥ %"
                rules={[{ required: true, type: 'number', min: 0, max: 100 }]}
              >
                <InputNumber min={0} max={100} step={1} style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col xs={12} md={6}>
              <Form.Item
                name="availabilityMin"
                label="Availability ≥ %"
                rules={[{ required: true, type: 'number', min: 0, max: 100 }]}
              >
                <InputNumber min={0} max={100} step={1} style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col xs={12} md={6}>
              <Form.Item
                name="performanceMin"
                label="Performance ≥ %"
                rules={[{ required: true, type: 'number', min: 0, max: 100 }]}
              >
                <InputNumber min={0} max={100} step={1} style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col xs={12} md={6}>
              <Form.Item
                name="qualityMin"
                label="Quality ≥ %"
                rules={[{ required: true, type: 'number', min: 0, max: 100 }]}
              >
                <InputNumber min={0} max={100} step={1} style={{ width: '100%' }} />
              </Form.Item>
            </Col>
          </Row>
          <Space>
            <Button type="primary" htmlType="submit" icon={<SaveOutlined />}
              loading={saving} disabled={!canEdit}>
              Сохранить
            </Button>
            <Button icon={<ReloadOutlined />} onClick={loadAll}>
              Сбросить
            </Button>
          </Space>
        </Form>
        <Divider />
        <Space split={<Divider type="vertical" />}>
          <Text type="secondary">
            Последнее изменение: {formatDate(meta?.updatedAt)}
          </Text>
          {meta?.updatedBy && (
            <Text type="secondary">кем: {meta.updatedBy}</Text>
          )}
          {!canEdit && <Tag color="default">только просмотр</Tag>}
        </Space>
      </Card>

      <Card title="Per-line outlier-пороги (скорость / температура)">
        <Alert
          type="warning"
          showIcon
          style={{ marginBottom: 12 }}
          message="Выходящие за эти диапазоны показания equipment-эмулятора помечаются ValidationService как невалидные и не учитываются в агрегатах смены. Audit — на странице «Outlier-показания»."
        />
        <Table
          rowKey="id"
          dataSource={lines}
          columns={lineColumns}
          pagination={{ pageSize: 10 }}
          size="small"
        />
      </Card>
    </div>
  )
}

export default ThresholdsPage
