import React, { useEffect, useMemo, useState } from 'react'
import {
  Card, Select, Space, Button, Typography, Row, Col, Tag, InputNumber,
  Empty, Spin, Alert, Segmented, message,
} from 'antd'
import { ReloadOutlined, BulbOutlined, FilterOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import { getRecommendations } from '../../api/recommendations'
import { useLines } from '../../hooks/useLines'
import { getRole } from '../../utils/auth'
import RecommendationCard from '../../components/RecommendationCard'

const { Title, Text } = Typography

const ROLE_OPTIONS = [
  { value: 'OPERATOR',     label: 'Оператор' },
  { value: 'TECHNOLOGIST', label: 'Технолог' },
  { value: 'ADMIN',        label: 'Админ' },
]

const ROLE_TO_QUERY = {
  ROLE_OPERATOR: 'OPERATOR',
  ROLE_TECHNOLOGIST: 'TECHNOLOGIST',
  ROLE_ADMIN: 'ADMIN',
}

const SEVERITY_OPTIONS = [
  { value: 'ALL',       label: 'Все' },
  { value: 'CRITICAL',  label: 'Критические' },
  { value: 'WARN',      label: 'Предупреждения' },
  { value: 'INFO',      label: 'Информационные' },
]

function RecommendationsPage() {
  const navigate = useNavigate()
  const userRole = getRole()
  const isAdmin = userRole === 'ROLE_ADMIN'

  const { options: lineOptions } = useLines(false)
  const [role, setRole] = useState(ROLE_TO_QUERY[userRole] || 'TECHNOLOGIST')
  const [lineId, setLineId] = useState(undefined)
  const [days, setDays] = useState(7)
  const [severityFilter, setSeverityFilter] = useState('ALL')
  const [recs, setRecs] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  const load = async () => {
    setLoading(true)
    setError(null)
    try {
      const params = { role, days }
      if (lineId) params.lineId = lineId
      const res = await getRecommendations(params)
      setRecs(res.data || [])
    } catch (e) {
      setError('Не удалось загрузить рекомендации')
      message.error('Не удалось загрузить рекомендации')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [role, lineId, days])

  const filtered = useMemo(() => {
    if (severityFilter === 'ALL') return recs
    return recs.filter(r => r.severity === severityFilter)
  }, [recs, severityFilter])

  const stats = useMemo(() => {
    return recs.reduce((acc, r) => {
      acc[r.severity] = (acc[r.severity] || 0) + 1
      acc.total = (acc.total || 0) + 1
      return acc
    }, { CRITICAL: 0, WARN: 0, INFO: 0, total: 0 })
  }, [recs])

  const handleOpenScope = (kind, id) => {
    if (kind === 'shift') {
      // Технолог/админ — в историю с фильтром по shiftId не предусмотрено;
      // оператор — в свой кабинет; для общего случая открываем дашборд.
      navigate('/dashboard')
    } else if (kind === 'line') {
      navigate('/dashboard')
    }
  }

  return (
    <div className="page-container">
      <Title level={3}>
        <BulbOutlined style={{ color: '#faad14', marginRight: 8 }} />
        Рекомендации
      </Title>
      <Text type="secondary">
        Автоматический анализ KPI и истории смен. Правила движка детерминированы —
        каждая рекомендация привязана к конкретной метрике и порогу.
      </Text>

      <Card style={{ marginTop: 16, marginBottom: 16 }}>
        <Row gutter={[16, 12]} align="middle">
          <Col xs={24} sm={12} md={6}>
            <Space direction="vertical" size={2} style={{ width: '100%' }}>
              <Text type="secondary">Роль (аудитория)</Text>
              <Select
                style={{ width: '100%' }}
                value={role}
                onChange={setRole}
                options={ROLE_OPTIONS}
                disabled={!isAdmin}
              />
            </Space>
          </Col>
          <Col xs={24} sm={12} md={6}>
            <Space direction="vertical" size={2} style={{ width: '100%' }}>
              <Text type="secondary">Линия</Text>
              <Select
                allowClear
                placeholder="Все линии"
                style={{ width: '100%' }}
                value={lineId}
                onChange={setLineId}
                options={lineOptions}
              />
            </Space>
          </Col>
          <Col xs={24} sm={12} md={6}>
            <Space direction="vertical" size={2} style={{ width: '100%' }}>
              <Text type="secondary">Период анализа (дней)</Text>
              <InputNumber
                min={1}
                max={90}
                style={{ width: '100%' }}
                value={days}
                onChange={(v) => setDays(v || 7)}
              />
            </Space>
          </Col>
          <Col xs={24} sm={12} md={6}>
            <Space style={{ width: '100%', justifyContent: 'flex-end' }}>
              <Button icon={<ReloadOutlined />} onClick={load} loading={loading}>
                Обновить
              </Button>
            </Space>
          </Col>
        </Row>
      </Card>

      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col xs={12} sm={6}>
          <Card size="small">
            <Space direction="vertical" size={0}>
              <Text type="secondary">Всего</Text>
              <Text strong style={{ fontSize: 24 }}>{stats.total}</Text>
            </Space>
          </Card>
        </Col>
        <Col xs={12} sm={6}>
          <Card size="small" style={{ borderLeft: '3px solid #ff4d4f' }}>
            <Space direction="vertical" size={0}>
              <Text type="secondary">Критические</Text>
              <Text strong style={{ fontSize: 24, color: '#ff4d4f' }}>{stats.CRITICAL}</Text>
            </Space>
          </Card>
        </Col>
        <Col xs={12} sm={6}>
          <Card size="small" style={{ borderLeft: '3px solid #faad14' }}>
            <Space direction="vertical" size={0}>
              <Text type="secondary">Предупреждения</Text>
              <Text strong style={{ fontSize: 24, color: '#faad14' }}>{stats.WARN}</Text>
            </Space>
          </Card>
        </Col>
        <Col xs={12} sm={6}>
          <Card size="small" style={{ borderLeft: '3px solid #1677ff' }}>
            <Space direction="vertical" size={0}>
              <Text type="secondary">Информационные</Text>
              <Text strong style={{ fontSize: 24, color: '#1677ff' }}>{stats.INFO}</Text>
            </Space>
          </Card>
        </Col>
      </Row>

      <Card
        title={<Space><FilterOutlined />Фильтр</Space>}
        size="small"
        style={{ marginBottom: 16 }}
      >
        <Segmented
          options={SEVERITY_OPTIONS}
          value={severityFilter}
          onChange={setSeverityFilter}
        />
      </Card>

      {error && <Alert type="error" message={error} style={{ marginBottom: 16 }} />}

      {loading ? (
        <Spin size="large" style={{ display: 'block', margin: '60px auto' }} />
      ) : filtered.length === 0 ? (
        <Empty description={
          stats.total === 0
            ? 'Активных рекомендаций нет — показатели в норме'
            : 'Под выбранный фильтр ничего не подходит'
        } />
      ) : (
        filtered.map(r => (
          <RecommendationCard
            key={r.id}
            rec={r}
            onOpenScope={handleOpenScope}
          />
        ))
      )}
    </div>
  )
}

export default RecommendationsPage
