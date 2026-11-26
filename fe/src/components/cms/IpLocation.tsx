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

/** Ghi nguồn dữ liệu vị trí IP theo giấy phép CC BY 4.0 của DB-IP (xem be/src/main/resources/geoip/NOTICE.md). */
export function DbIpAttribution() {
  return (
    <Text type="secondary" className="block text-xs">
      <a href="https://db-ip.com" target="_blank" rel="noopener">IP Geolocation by DB-IP</a>
    </Text>
  )
}
