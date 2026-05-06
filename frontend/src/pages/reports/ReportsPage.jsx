import React, { useEffect, useState } from 'react'
import {
  Card, Select, Button, Table, Typography,
  Row, Col, Statistic, Tag, Space, Divider, message
} from 'antd'
import { SearchOutlined, ReloadOutlined, DownloadOutlined } from '@ant-design/icons'
import { getReportsByLine, getReportByShift, generateReport, exportShiftCsv } from '../../api/reports'
import { getKpiByShift } from '../../api/kpi'
import { useLines } from '../../hooks/useLines'
import KpiMetricLabel from '../../components/KpiMetricLabel'

const { Title } = Typography

function ReportsPage() {
  const { options: lineOptions, lines } = useLines(true)
  const [lineId, setLineId] = useState(undefined)
  const [reports, setReports] = useState([])
  const [selectedKpi, setSelectedKpi] = useState(null)
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (!lineId && lines.length > 0) setLineId(lines[0].code)
  }, [lines, lineId])

  const loadReports = async () => {
    if (!lineId) return
    setLoading(true)
    try {
      const res = await getReportsByLine(lineId)
      setReports(res.data || [])
    } catch (e) {
      console.error(e)
    } finally {
      setLoading(false)
    }
  }

  const loadDetail = async (shiftId) => {
    try {
      const [, kpiRes] = await Promise.all([
        getReportByShift(shiftId),
        getKpiByShift(shiftId),
      ])
      setSelectedKpi(kpiRes.data)
    } catch (e) {
      console.error(e)
    }
  }

  const handleGenerate = async (shiftId) => {
    await generateReport(shiftId)
    await loadDetail(shiftId)
  }

  const handleDownload = async (shiftId) => {
    try {
      const res = await exportShiftCsv(shiftId)
      const url = window.URL.createObjectURL(new Blob([res.data], { type: 'text/csv' }))
      const a = document.createElement('a')
      a.href = url
      a.download = `shift-${shiftId}-report.csv`
      document.body.appendChild(a)
      a.click()
      a.remove()
      window.URL.revokeObjectURL(url)
    } catch (e) {
      message.error('Не удалось скачать отчёт')
    }
  }

  const columns = [
    { title: 'ID смены', dataIndex: 'shiftId', key: 'shiftId' },
    { title: 'Линия', dataIndex: 'lineId', key: 'lineId' },
    {
      title: <KpiMetricLabel metricKey="oee" label="OEE" />, dataIndex: 'oee', key: 'oee',
      render: v => v ? `${(v * 100).toFixed(1)}%` : '—'
    },
    {
      title: 'Статус', dataIndex: 'status', key: 'status',
      render: s => <Tag color="green">{s}</Tag>
    },
    {
      title: 'Дата', dataIndex: 'createdAt', key: 'createdAt',
      render: t => new Date(t).toLocaleString('ru')
    },
    {
      title: 'Действия', key: 'actions',
      render: (_, record) => (
        <Space>
          <Button size="small" onClick={() => loadDetail(record.shiftId)}>
            Просмотр
          </Button>
          <Button size="small" icon={<ReloadOutlined />}
            onClick={() => handleGenerate(record.shiftId)}>
            Перегенерировать
          </Button>
          <Button size="small" icon={<DownloadOutlined />}
            onClick={() => handleDownload(record.shiftId)}>
            CSV
          </Button>
        </Space>
      )
    }
  ]

  return (
    <div className="page-container">
      <Title level={3}>Отчёты по сменам</Title>

      <Card style={{ marginBottom: 24 }}>
        <Space>
          <Select
            value={lineId}
            onChange={setLineId}
            options={lineOptions}
            placeholder="Линия"
            style={{ width: 280 }}
          />
          <Button type="primary" icon={<SearchOutlined />} onClick={loadReports} loading={loading}>
            Загрузить отчёты
          </Button>
        </Space>
      </Card>

      <Table
        dataSource={reports}
        columns={columns}
        rowKey="id"
        loading={loading}
        style={{ marginBottom: 24 }}
      />

      {selectedKpi && (
        <Card title={`Детали смены #${selectedKpi.shiftId}`}>
          <Row gutter={16}>
            <Col span={6}><Statistic title={<KpiMetricLabel metricKey="oee" label="OEE" />} value={`${(selectedKpi.oee * 100).toFixed(2)}%`} /></Col>
            <Col span={6}><Statistic title={<KpiMetricLabel metricKey="availability" label="Доступность" />} value={`${(selectedKpi.availability * 100).toFixed(2)}%`} /></Col>
            <Col span={6}><Statistic title={<KpiMetricLabel metricKey="performance" label="Производительность" />} value={`${(selectedKpi.performance * 100).toFixed(2)}%`} /></Col>
            <Col span={6}><Statistic title={<KpiMetricLabel metricKey="quality" label="Качество" />} value={`${(selectedKpi.quality * 100).toFixed(2)}%`} /></Col>
          </Row>
          <Divider />
          <Row gutter={16}>
            <Col span={6}><Statistic title="Выпуск" value={selectedKpi.totalOutput} suffix="шт" /></Col>
            <Col span={6}><Statistic title={<KpiMetricLabel metricKey="scrapRate" label="Брак" />} value={selectedKpi.scrapCount} suffix="шт" /></Col>
            <Col span={6}><Statistic title={<KpiMetricLabel metricKey="downtime" label="Простой" />} value={`${Number(selectedKpi.downtime).toFixed(1)} мин`} /></Col>
            <Col span={6}><Statistic title={<KpiMetricLabel metricKey="planFulfillment" label="Выполнение плана" />} value={`${(selectedKpi.planFulfillment * 100).toFixed(1)}%`} /></Col>
          </Row>
          <Divider />
          <Row gutter={16}>
            <Col span={6}><Statistic title={<KpiMetricLabel metricKey="plannedTime" label="Плановое время" />} value={`${selectedKpi.plannedTime} мин`} /></Col>
            <Col span={6}><Statistic title={<KpiMetricLabel metricKey="operatingTime" label="Рабочее время" />} value={`${selectedKpi.operatingTime} мин`} /></Col>
            <Col span={6}><Statistic title={<KpiMetricLabel metricKey="speedLoss" label="Скорость потерь" />} value={`${selectedKpi.speedLoss} ед/мин`} /></Col>
            <Col span={6}><Statistic title={<KpiMetricLabel metricKey="numberOfStops" label="Остановок" />} value={selectedKpi.numberOfStops} /></Col>
          </Row>
        </Card>
      )}
    </div>
  )
}

export default ReportsPage
