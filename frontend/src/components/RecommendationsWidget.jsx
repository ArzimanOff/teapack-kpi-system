import React, { useEffect, useState } from 'react'
import { Card, Empty, Spin, Space, Tag, Typography, Button } from 'antd'
import { BulbOutlined, RightOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import { getRecommendations } from '../api/recommendations'
import { getRole } from '../utils/auth'
import RecommendationCard from './RecommendationCard'

const { Text } = Typography

const ROLE_TO_QUERY = {
  ROLE_OPERATOR: 'OPERATOR',
  ROLE_TECHNOLOGIST: 'TECHNOLOGIST',
  ROLE_ADMIN: 'ADMIN',
}

function RecommendationsWidget({ lineId, limit = 3 }) {
  const [recs, setRecs] = useState([])
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()

  const role = getRole()
  const roleParam = ROLE_TO_QUERY[role]

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    const params = { role: roleParam, days: 7 }
    if (lineId) params.lineId = lineId
    getRecommendations(params)
      .then(res => { if (!cancelled) setRecs(res.data || []) })
      .catch(() => { if (!cancelled) setRecs([]) })
      .finally(() => { if (!cancelled) setLoading(false) })
    return () => { cancelled = true }
  }, [roleParam, lineId])

  const top = recs.slice(0, limit)
  const counts = recs.reduce((acc, r) => {
    acc[r.severity] = (acc[r.severity] || 0) + 1
    return acc
  }, {})

  return (
    <Card
      size="small"
      title={
        <Space>
          <BulbOutlined style={{ color: '#faad14' }} />
          <Text strong>Рекомендации</Text>
          {counts.CRITICAL > 0 && <Tag color="red">{counts.CRITICAL} крит.</Tag>}
          {counts.WARN > 0 && <Tag color="orange">{counts.WARN} внимание</Tag>}
          {counts.INFO > 0 && <Tag color="blue">{counts.INFO} инфо</Tag>}
        </Space>
      }
      extra={
        <Button type="link" size="small" onClick={() => navigate('/recommendations')}>
          Все <RightOutlined />
        </Button>
      }
    >
      {loading ? (
        <Spin />
      ) : top.length === 0 ? (
        <Empty image={Empty.PRESENTED_IMAGE_SIMPLE}
          description="Активных рекомендаций нет — показатели в норме" />
      ) : (
        <>
          {top.map(r => (
            <RecommendationCard key={r.id} rec={r} compact />
          ))}
          {recs.length > limit && (
            <Text type="secondary" style={{ fontSize: 12 }}>
              Показано {limit} из {recs.length}. Откройте «Рекомендации» для полного списка.
            </Text>
          )}
        </>
      )}
    </Card>
  )
}

export default RecommendationsWidget
