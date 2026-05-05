import React, { useEffect, useState } from 'react'
import {
  Card, Table, Button, Space, Typography, Tag, Drawer, Form,
  Input, Select, Switch, Row, Col, Popconfirm, message, Alert
} from 'antd'
import {
  PlusOutlined, EditOutlined, StopOutlined, ReloadOutlined,
} from '@ant-design/icons'
import { getUsers, createUser, updateUser, deactivateUser, getRoles } from '../../api/users'

const { Title } = Typography

const ROLE_LABEL = {
  ROLE_ADMIN: 'Администратор',
  ROLE_OPERATOR: 'Оператор',
  ROLE_TECHNOLOGIST: 'Технолог',
}
const ROLE_COLOR = {
  ROLE_ADMIN: 'red',
  ROLE_OPERATOR: 'blue',
  ROLE_TECHNOLOGIST: 'green',
}

const formatDate = (iso) => iso ? new Date(iso).toLocaleString('ru-RU') : '—'

function UsersAdminPage() {
  const [users, setUsers] = useState([])
  const [roles, setRoles] = useState([])
  const [loading, setLoading] = useState(false)
  const [drawer, setDrawer] = useState({ open: false, mode: 'create', row: null })
  const [form] = Form.useForm()

  const load = async () => {
    setLoading(true)
    try {
      const [u, r] = await Promise.all([getUsers(), getRoles()])
      setUsers(u.data || [])
      setRoles(r.data || [])
    } catch (e) {
      message.error('Не удалось загрузить пользователей')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [])

  const openCreate = () => {
    form.resetFields()
    form.setFieldsValue({ enabled: true, role: 'ROLE_OPERATOR' })
    setDrawer({ open: true, mode: 'create', row: null })
  }

  const openEdit = (row) => {
    form.resetFields()
    form.setFieldsValue({
      username: row.username,
      fullName: row.fullName,
      email: row.email,
      role: row.role,
      enabled: row.enabled,
      password: '',
    })
    setDrawer({ open: true, mode: 'edit', row })
  }

  const handleSubmit = async (values) => {
    try {
      if (drawer.mode === 'create') {
        await createUser({
          username: values.username,
          password: values.password,
          fullName: values.fullName,
          email: values.email,
          role: values.role,
        })
        message.success('Пользователь создан')
      } else {
        const payload = {
          fullName: values.fullName,
          email: values.email,
          role: values.role,
          enabled: values.enabled,
        }
        if (values.password) payload.password = values.password
        await updateUser(drawer.row.id, payload)
        message.success('Пользователь обновлён')
      }
      setDrawer({ open: false, mode: 'create', row: null })
      load()
    } catch (e) {
      message.error(e?.response?.data?.message || 'Ошибка сохранения')
    }
  }

  const handleDeactivate = async (row) => {
    try {
      await deactivateUser(row.id)
      message.success(`${row.username} деактивирован`)
      load()
    } catch (e) {
      message.error('Не удалось деактивировать')
    }
  }

  const columns = [
    { title: 'ID', dataIndex: 'id', width: 70 },
    { title: 'Логин', dataIndex: 'username', width: 140 },
    { title: 'ФИО', dataIndex: 'fullName' },
    { title: 'Email', dataIndex: 'email' },
    {
      title: 'Роль',
      dataIndex: 'role',
      width: 160,
      render: (r) => <Tag color={ROLE_COLOR[r] || 'default'}>{ROLE_LABEL[r] || r}</Tag>,
      filters: roles.map(r => ({ text: ROLE_LABEL[r] || r, value: r })),
      onFilter: (val, rec) => rec.role === val,
    },
    {
      title: 'Активен',
      dataIndex: 'enabled',
      width: 100,
      render: (v) => v
        ? <Tag color="green">Да</Tag>
        : <Tag color="default">Нет</Tag>,
    },
    {
      title: 'Создан',
      dataIndex: 'createdAt',
      width: 170,
      render: formatDate,
    },
    {
      title: '',
      key: 'actions',
      width: 200,
      render: (_, row) => (
        <Space>
          <Button size="small" icon={<EditOutlined />} onClick={() => openEdit(row)}>
            Изменить
          </Button>
          {row.enabled && (
            <Popconfirm
              title={`Деактивировать ${row.username}?`}
              description="Пользователь не сможет войти. Запись не удаляется."
              okText="Да" cancelText="Нет"
              onConfirm={() => handleDeactivate(row)}
            >
              <Button size="small" danger icon={<StopOutlined />}>
                Отключить
              </Button>
            </Popconfirm>
          )}
        </Space>
      ),
    },
  ]

  return (
    <div className="page-container">
      <Title level={3}>Управление пользователями</Title>

      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 16 }}
        message="Создание учётных записей операторов и технологов, смена ролей и пароля. Удаление soft-delete (enabled=false)."
      />

      <Card style={{ marginBottom: 16 }}>
        <Space>
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
            Создать пользователя
          </Button>
          <Button icon={<ReloadOutlined />} onClick={load}>Обновить</Button>
        </Space>
      </Card>

      <Card>
        <Table
          rowKey="id"
          loading={loading}
          dataSource={users}
          columns={columns}
          pagination={{ pageSize: 10, showSizeChanger: true }}
        />
      </Card>

      <Drawer
        title={drawer.mode === 'create' ? 'Новый пользователь' : `Редактирование: ${drawer.row?.username}`}
        open={drawer.open}
        width={480}
        onClose={() => setDrawer({ open: false, mode: 'create', row: null })}
        destroyOnClose
      >
        <Form form={form} layout="vertical" onFinish={handleSubmit}>
          <Form.Item
            name="username" label="Логин"
            rules={drawer.mode === 'create'
              ? [{ required: true, min: 3 }] : []}
          >
            <Input disabled={drawer.mode === 'edit'} />
          </Form.Item>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="fullName" label="ФИО">
                <Input />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="email" label="Email">
                <Input type="email" />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="role" label="Роль" rules={[{ required: true }]}>
            <Select
              options={roles.map(r => ({ value: r, label: ROLE_LABEL[r] || r }))}
            />
          </Form.Item>
          <Form.Item
            name="password"
            label={drawer.mode === 'create' ? 'Пароль' : 'Новый пароль (оставьте пустым, чтобы не менять)'}
            rules={drawer.mode === 'create'
              ? [{ required: true, min: 4 }]
              : [{ min: 4, message: 'Минимум 4 символа' }]}
          >
            <Input.Password autoComplete="new-password" />
          </Form.Item>
          {drawer.mode === 'edit' && (
            <Form.Item name="enabled" label="Активен" valuePropName="checked">
              <Switch />
            </Form.Item>
          )}
          <Space>
            <Button type="primary" htmlType="submit">
              {drawer.mode === 'create' ? 'Создать' : 'Сохранить'}
            </Button>
            <Button onClick={() => setDrawer({ open: false, mode: 'create', row: null })}>
              Отмена
            </Button>
          </Space>
        </Form>
      </Drawer>
    </div>
  )
}

export default UsersAdminPage
