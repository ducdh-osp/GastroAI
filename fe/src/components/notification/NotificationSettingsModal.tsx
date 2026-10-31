import {
  BellOutlined,
  BookOutlined,
  MedicineBoxOutlined,
  SafetyCertificateOutlined,
  SaveOutlined,
} from '@ant-design/icons'
import { Button, message, Modal, Spin, Switch, Typography } from 'antd'
import { useEffect, useState } from 'react'
import {
  getNotificationSettings,
  updateNotificationSettings,
  type NotificationSettings,
} from '../../api/notifications'
import { extractErrorMessage } from '../../lib/errors'

const { Text, Paragraph } = Typography

interface Props {
  open: boolean
  onClose: () => void
}

export function NotificationSettingsModal({ open, onClose }: Props) {
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)
  const [settings, setSettings] = useState<NotificationSettings>({
    medicationReminderEnabled: true,
    journalReminderEnabled: true,
    systemBroadcastEnabled: true,
    securityAlertEnabled: true,
  })

  useEffect(() => {
    if (!open) return
    let isMounted = true
    setLoading(true)
    getNotificationSettings()
      .then((res) => {
        if (isMounted) setSettings(res)
      })
      .catch((err) => {
        message.error(extractErrorMessage(err, 'Không thể tải cài đặt thông báo').message)
      })
      .finally(() => {
        if (isMounted) setLoading(false)
      })
    return () => {
      isMounted = false
    }
  }, [open])

  const handleSave = async () => {
    setSaving(true)
    try {
      const updated = await updateNotificationSettings({
        medicationReminderEnabled: settings.medicationReminderEnabled,
        journalReminderEnabled: settings.journalReminderEnabled,
        systemBroadcastEnabled: settings.systemBroadcastEnabled,
        securityAlertEnabled: settings.securityAlertEnabled,
      })
      setSettings(updated)
      message.success('Đã lưu cài đặt thông báo thành công')
      onClose()
    } catch (err) {
      message.error(extractErrorMessage(err, 'Không thể cập nhật cài đặt thông báo').message)
    } finally {
      setSaving(false)
    }
  }

  return (
    <Modal
      title={
        <div className="flex items-center gap-2">
          <BellOutlined className="text-teal-600 text-lg" />
          <span className="font-semibold text-base">Cài đặt thông báo cá nhân</span>
        </div>
      }
      open={open}
      onCancel={onClose}
      footer={[
        <Button key="cancel" onClick={onClose} disabled={saving}>
          Hủy
        </Button>,
        <Button
          key="save"
          type="primary"
          icon={<SaveOutlined />}
          loading={saving}
          onClick={handleSave}
          className="bg-teal-600 hover:bg-teal-500"
        >
          Lưu thay đổi
        </Button>,
      ]}
      centered
      destroyOnClose
    >
      {loading ? (
        <div className="flex justify-center items-center py-12">
          <Spin tip="Đang tải cài đặt..." />
        </div>
      ) : (
        <div className="py-2 space-y-4">
          <Paragraph type="secondary" className="mb-4 text-xs">
            Tùy chỉnh các loại thông báo và nhắc nhở bạn muốn nhận từ ứng dụng GastroAI. Các thiết lập sẽ có hiệu lực ngay lập tức.
          </Paragraph>

          {/* Nhắc nhở uống thuốc */}
          <div className="flex items-start justify-between gap-4 p-3 rounded-xl bg-slate-50 border border-slate-100 hover:border-teal-200 transition-colors">
            <div className="flex gap-3">
              <div className="p-2.5 rounded-lg bg-purple-100 text-purple-600 text-lg flex items-center justify-center">
                <MedicineBoxOutlined />
              </div>
              <div>
                <Text strong className="text-slate-800 text-sm block">
                  Nhắc uống thuốc
                </Text>
                <Text type="secondary" className="text-xs">
                  Nhận thông báo nhắc nhở khi đến lịch uống thuốc theo đơn điều trị của bạn.
                </Text>
              </div>
            </div>
            <Switch
              checked={settings.medicationReminderEnabled}
              onChange={(checked) =>
                setSettings((prev) => ({ ...prev, medicationReminderEnabled: checked }))
              }
            />
          </div>

          {/* Nhắc nhở ghi nhật ký */}
          <div className="flex items-start justify-between gap-4 p-3 rounded-xl bg-slate-50 border border-slate-100 hover:border-teal-200 transition-colors">
            <div className="flex gap-3">
              <div className="p-2.5 rounded-lg bg-emerald-100 text-emerald-600 text-lg flex items-center justify-center">
                <BookOutlined />
              </div>
              <div>
                <Text strong className="text-slate-800 text-sm block">
                  Nhắc ghi nhật ký sức khỏe
                </Text>
                <Text type="secondary" className="text-xs">
                  Nhận lời nhắc ghi nhật ký bữa ăn và theo dõi phân chuẩn Bristol hằng ngày.
                </Text>
              </div>
            </div>
            <Switch
              checked={settings.journalReminderEnabled}
              onChange={(checked) =>
                setSettings((prev) => ({ ...prev, journalReminderEnabled: checked }))
              }
            />
          </div>

          {/* Thông báo broadcast hệ thống */}
          <div className="flex items-start justify-between gap-4 p-3 rounded-xl bg-slate-50 border border-slate-100 hover:border-teal-200 transition-colors">
            <div className="flex gap-3">
              <div className="p-2.5 rounded-lg bg-blue-100 text-blue-600 text-lg flex items-center justify-center">
                <BellOutlined />
              </div>
              <div>
                <Text strong className="text-slate-800 text-sm block">
                  Thông báo hệ thống & tin y khoa
                </Text>
                <Text type="secondary" className="text-xs">
                  Nhận các thông báo chung từ quản trị viên, tin tức y khoa và lịch bảo trì hệ thống.
                </Text>
              </div>
            </div>
            <Switch
              checked={settings.systemBroadcastEnabled}
              onChange={(checked) =>
                setSettings((prev) => ({ ...prev, systemBroadcastEnabled: checked }))
              }
            />
          </div>

          {/* Cảnh báo an toàn & bảo mật */}
          <div className="flex items-start justify-between gap-4 p-3 rounded-xl bg-slate-50 border border-slate-100 hover:border-teal-200 transition-colors">
            <div className="flex gap-3">
              <div className="p-2.5 rounded-lg bg-amber-100 text-amber-600 text-lg flex items-center justify-center">
                <SafetyCertificateOutlined />
              </div>
              <div>
                <Text strong className="text-slate-800 text-sm block">
                  Cảnh báo an toàn & bảo mật
                </Text>
                <Text type="secondary" className="text-xs">
                  Cảnh báo bảo mật khi phát hiện đăng nhập mới hoặc thay đổi thông tin tài khoản.
                </Text>
              </div>
            </div>
            <Switch
              checked={settings.securityAlertEnabled}
              onChange={(checked) =>
                setSettings((prev) => ({ ...prev, securityAlertEnabled: checked }))
              }
            />
          </div>
        </div>
      )}
    </Modal>
  )
}
