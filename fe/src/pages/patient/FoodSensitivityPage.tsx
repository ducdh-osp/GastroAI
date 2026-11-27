import { Typography } from 'antd'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { FoodSensitivityTab } from '../../components/foodDiary/FoodSensitivityTab'
import { AppShell } from '../../components/layout/AppShell'

const { Title, Text } = Typography

export default function FoodSensitivityPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const navigate = useNavigate()
  const requestedDays = Number(searchParams.get('sensitivityDays'))
  const days = requestedDays === 30 || requestedDays === 180 ? requestedDays : 90

  return (
    <AppShell>
      <div className="w-full">
        <div className="mb-6">
          <Text type="secondary">Xem lại & phân tích</Text>
          <Title level={2} className="mb-1! mt-1!">Nhạy cảm thực phẩm</Title>
          <Text type="secondary">Đối chiếu bữa ăn, triệu chứng tự ghi và nhật ký Bristol.</Text>
        </div>
        <FoodSensitivityTab
          days={days}
          onDaysChange={(nextDays) => setSearchParams({ sensitivityDays: String(nextDays) }, { replace: true })}
          onViewEvidence={(keyword, keywordDays) => {
            const params = new URLSearchParams({ keyword, keywordDays: String(keywordDays) })
            navigate(`/food-diary?${params}`)
          }}
        />
      </div>
    </AppShell>
  )
}
