import React from 'react'
import { Card, Tag, Typography, Space, Statistic } from 'antd'
import {
  WarningOutlined, FireOutlined, InfoCircleOutlined, BulbOutlined,
} from '@ant-design/icons'

const { Text, Paragraph } = Typography

const SEVERITY = {
  CRITICAL: { color: '#ff4d4f', tag: 'red',   label: 'Критично', icon: <FireOutlined /> },
  WARN:     { color: '#faad14', tag: 'orange', label: 'Внимание', icon: <WarningOutlined /> },
  INFO:     { color: '#1677ff', tag: 'blue',   label: 'Инфо',     icon: <InfoCircleOutlined /> },
}

const CATEGORY_LABEL = {
  PERFORMANCE: 'Производительность',
  AVAILABILITY: 'Доступность',
  QUALITY: 'Качество',
  DOWNTIME: 'Простои',
  PROCESS: 'Процесс',
  STAFFING: 'Орг./персонал',
  EQUIPMENT: 'Оборудование',
  DATA_QUALITY: 'Данные',
}

const ROLE_LABEL = {
  OPERATOR: 'Оператор',
  TECHNOLOGIST: 'Технолог',
  ADMIN: 'Админ',
}

function fmtPct(v) {
  if (v == null) return null
  return `${(Number(v) * 100).toFixed(1)}%`
}

function RecommendationCard({ rec, compact = false, onOpenScope }) {
  const sev = SEVERITY[rec.severity] || SEVERITY.INFO
  const scope = rec.scope || {}

  const isPctMetric = ['oee', 'availability', 'performance', 'quality',
    'scrapRate', 'planFulfillment', 'downtimeRate', 'planLag'].includes(rec.metric)

  return (
    <Card
      size={compact ? 'small' : 'default'}
      style={{
        borderLeft: `4px solid ${sev.color}`,
        marginBottom: compact ? 8 : 12,
      }}
      bodyStyle={compact ? { padding: 12 } : undefined}
    >
      <Space direction="vertical" size={4} style={{ width: '100%' }}>
        <Space wrap>
          <Tag color={sev.tag} icon={sev.icon}>{sev.label}</Tag>
          {rec.category && <Tag>{CATEGORY_LABEL[rec.category] || rec.category}</Tag>}
          {rec.role && <Tag color="purple">{ROLE_LABEL[rec.role] || rec.role}</Tag>}
          {scope.lineId && (
            <Tag color="geekblue">Линия {scope.lineId}</Tag>
          )}
          {scope.shiftId && (
            <Tag color="cyan">
              Смена #{scope.shiftId}
            </Tag>
          )}
          {scope.days && (
            <Tag>за {scope.days} дн</Tag>
          )}
        </Space>

        <Text strong style={{ fontSize: compact ? 14 : 15 }}>{rec.title}</Text>

        {!compact && (
          <Paragraph style={{ marginBottom: 4 }} type="secondary">
            {rec.description}
          </Paragraph>
        )}
        {compact && (
          <Text type="secondary" style={{ fontSize: 12 }}>{rec.description}</Text>
        )}

        {!compact && rec.suggestedAction && (
          <Space align="start">
            <BulbOutlined style={{ color: '#faad14', marginTop: 4 }} />
            <Text>{rec.suggestedAction}</Text>
          </Space>
        )}

        {!compact && rec.metric && rec.value != null && (
          <Space size="large" style={{ marginTop: 6 }}>
            <Statistic
              title="Текущее"
              value={isPctMetric ? fmtPct(rec.value) : Number(rec.value).toFixed(2)}
              valueStyle={{ fontSize: 16, color: sev.color }}
            />
            {rec.threshold != null && (
              <Statistic
                title="Порог"
                value={isPctMetric ? fmtPct(rec.threshold) : Number(rec.threshold).toFixed(2)}
                valueStyle={{ fontSize: 14, color: '#888' }}
              />
            )}
          </Space>
        )}

        {onOpenScope && (scope.shiftId || scope.lineId) && (
          <Space>
            {scope.shiftId && (
              <a onClick={() => onOpenScope('shift', scope.shiftId)}>
                Открыть смену →
              </a>
            )}
            {scope.lineId && !scope.shiftId && (
              <a onClick={() => onOpenScope('line', scope.lineId)}>
                Открыть линию →
              </a>
            )}
          </Space>
        )}
      </Space>
    </Card>
  )
}

export default RecommendationCard
