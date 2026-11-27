import { Tooltip, Typography } from 'antd'

const { Text } = Typography

const IP_LOCATION_HINT = 'Vị trí ước tính theo địa chỉ IP, có thể là vị trí của nhà mạng'

/** Vị trí ước tính hiện dưới địa chỉ IP (chữ nhỏ, màu phụ). null/rỗng -> không hiện gì. */
export function IpLocationText({ location }: { location: string | null | undefined }) {
  if (!location) return null
  return (
    <Tooltip title={IP_LOCATION_HINT} placement="topLeft">
      <Text type="secondary" className="block cursor-help text-xs">{location}</Text>
    </Tooltip>
  )
}
