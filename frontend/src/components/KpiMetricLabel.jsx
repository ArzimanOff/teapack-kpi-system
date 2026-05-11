import React from 'react'
import { Popover, Space, Typography, Tag } from 'antd'
import { QuestionCircleOutlined, ArrowUpOutlined, ArrowDownOutlined } from '@ant-design/icons'
import { getKpi } from '../constants/kpiDescriptions'

const { Text, Paragraph } = Typography

// Inline-вопросик рядом с заголовком метрики. По клику показывает popover
// с формулой, описанием и интерпретацией. Источник — kpiDescriptions.js.
function KpiMetricLabel({ metricKey, label, children, iconOnly = false, size = 'small' }) {
  const meta = getKpi(metricKey)
  if (!meta) return <span>{label || children}</span>

  const directionTag = meta.higherIsBetter === true ? (
    <Tag color="green" icon={<ArrowUpOutlined />} style={{ marginInlineEnd: 0 }}>выше — лучше</Tag>
  ) : meta.higherIsBetter === false ? (
    <Tag color="orange" icon={<ArrowDownOutlined />} style={{ marginInlineEnd: 0 }}>ниже — лучше</Tag>
  ) : null

  const content = (
    <div style={{ maxWidth: 360 }}>
      <Text strong>{meta.title}</Text>
      <div style={{ margin: '8px 0' }}>
        <Text type="secondary" style={{ fontSize: 12 }}>Формула:</Text>
        <Paragraph style={{
          background: '#f5f5f5', padding: '6px 10px', borderRadius: 4,
          fontFamily: 'monospace', fontSize: 13, margin: '4px 0', whiteSpace: 'pre-line',
        }}>
          {meta.formula}
        </Paragraph>
      </div>
      <Paragraph style={{ marginBottom: 8 }}>{meta.desc}</Paragraph>
      {meta.interpretation && (
        <Paragraph type="secondary" style={{ fontSize: 12, marginBottom: 8 }}>
          {meta.interpretation}
        </Paragraph>
      )}
      <Space size={6}>
        <Tag>{meta.unit}</Tag>
        {directionTag}
      </Space>
    </div>
  )

  const trigger = iconOnly ? (
    <QuestionCircleOutlined style={{ color: '#8c8c8c', cursor: 'help', fontSize: size === 'large' ? 16 : 13 }} />
  ) : (
    <Space size={4} style={{ cursor: 'help' }}>
      <span>{label || children || meta.title.split(' — ')[0]}</span>
      <QuestionCircleOutlined style={{ color: '#8c8c8c', fontSize: 12 }} />
    </Space>
  )

  return (
    <Popover content={content} trigger={['click', 'hover']} placement="top">
      {trigger}
    </Popover>
  )
}

export default KpiMetricLabel
