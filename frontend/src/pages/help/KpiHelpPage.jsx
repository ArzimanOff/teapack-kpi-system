import React from 'react'
import { Card, Table, Typography, Tag, Alert, Anchor, Space, Divider } from 'antd'
import { ArrowUpOutlined, ArrowDownOutlined, BookOutlined } from '@ant-design/icons'
import { KPI_DESCRIPTIONS, KPI_GROUPS } from '../../constants/kpiDescriptions'

const { Title, Paragraph, Text } = Typography

const directionTag = (h) => {
  if (h === true) return <Tag color="green" icon={<ArrowUpOutlined />}>выше — лучше</Tag>
  if (h === false) return <Tag color="orange" icon={<ArrowDownOutlined />}>ниже — лучше</Tag>
  return <Tag>информационная</Tag>
}

const slug = (group) => group.replace(/[^a-zа-я0-9]+/gi, '-').toLowerCase()

function GroupTable({ group, items }) {
  const columns = [
    {
      title: 'Метрика',
      dataIndex: 'title',
      key: 'title',
      width: 280,
      render: (t) => <Text strong>{t}</Text>,
    },
    {
      title: 'Формула',
      dataIndex: 'formula',
      key: 'formula',
      render: (f) => (
        <pre style={{
          margin: 0, fontFamily: 'monospace', fontSize: 13,
          background: '#fafafa', padding: '6px 10px', borderRadius: 4,
          whiteSpace: 'pre-wrap',
        }}>{f}</pre>
      ),
    },
    {
      title: 'Ед.',
      dataIndex: 'unit',
      key: 'unit',
      width: 70,
      render: (u) => <Tag>{u}</Tag>,
    },
    {
      title: 'Направление',
      dataIndex: 'higherIsBetter',
      key: 'dir',
      width: 160,
      render: (h) => directionTag(h),
    },
  ]
  const dataSource = items.map(([key, m]) => ({ key, ...m }))
  return (
    <Card
      id={slug(group)}
      title={group}
      style={{ marginBottom: 24 }}
      styles={{ body: { padding: 0 } }}
    >
      <Table
        columns={columns}
        dataSource={dataSource}
        pagination={false}
        size="middle"
        expandable={{
          expandedRowRender: (rec) => (
            <div style={{ padding: '4px 12px' }}>
              <Paragraph style={{ marginBottom: 6 }}>{rec.desc}</Paragraph>
              {rec.interpretation && (
                <Paragraph type="secondary" style={{ fontSize: 13, marginBottom: 0 }}>
                  <Text type="secondary" strong>Интерпретация: </Text>
                  {rec.interpretation}
                </Paragraph>
              )}
            </div>
          ),
          defaultExpandAllRows: true,
        }}
      />
    </Card>
  )
}

function KpiHelpPage() {
  const grouped = Object.values(KPI_GROUPS).map((group) => [
    group,
    Object.entries(KPI_DESCRIPTIONS).filter(([_, m]) => m.group === group),
  ])

  return (
    <div style={{ display: 'flex', gap: 24 }}>
      <div style={{ flex: 1, minWidth: 0 }}>
        <Space align="center" style={{ marginBottom: 16 }}>
          <BookOutlined style={{ fontSize: 22 }} />
          <Title level={3} style={{ margin: 0 }}>Справка по KPI</Title>
        </Space>

        <Alert
          type="info"
          showIcon
          style={{ marginBottom: 16 }}
          message="Расчёт KPI ведётся по классической методике ОЕЕ (Nakajima)"
          description={
            <span>
              Три независимых множителя — Availability (время), Performance (скорость), Quality (качество).
              Их произведение и есть OEE. Формулы реализованы в <Text code>kpi-calculation-service / KpiCalculator.java</Text>,
              расчёт запускается при закрытии смены и сохраняется в <Text code>kpi.shift_kpi</Text>.
            </span>
          }
        />

        <Card style={{ marginBottom: 24 }} title="Пример расчёта">
          <Paragraph>
            Смена 8 часов (480 мин), простоев 2 часа (120 мин), номинальная скорость 100 шт/мин,
            фактически выпущено 24 000 шт, из них годных 22 800.
          </Paragraph>
          <Paragraph style={{ fontFamily: 'monospace', fontSize: 13, background: '#fafafa', padding: 10, borderRadius: 4 }}>
            OperatingTime = 480 − 120 = 360 мин<br />
            Availability = 360 / 480 = 0.75 (75%)<br />
            Performance = 24 000 / (100 × 360) = 0.667 (66.7%)<br />
            Quality = 22 800 / 24 000 = 0.95 (95%)<br />
            <Text strong>OEE = 0.75 × 0.667 × 0.95 = 0.475 (47.5%)</Text>
          </Paragraph>
        </Card>

        {grouped.map(([group, items]) => (
          <GroupTable key={group} group={group} items={items} />
        ))}

        <Divider />
        <Paragraph type="secondary" style={{ fontSize: 12 }}>
          При изменении формул в KpiCalculator.java необходимо обновить
          <Text code> frontend/src/constants/kpiDescriptions.js</Text> — это единый источник
          описаний для подсказок и для этой страницы.
        </Paragraph>
      </div>

      <div style={{ width: 220, flexShrink: 0 }}>
        <Anchor
          affix
          offsetTop={80}
          items={Object.values(KPI_GROUPS).map((g) => ({
            key: slug(g),
            href: `#${slug(g)}`,
            title: g,
          }))}
        />
      </div>
    </div>
  )
}

export default KpiHelpPage
