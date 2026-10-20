import { FileSearchOutlined } from '@ant-design/icons'
import { Alert, Card, Input, List, Typography, message } from 'antd'
import { useState } from 'react'
import { Link } from 'react-router-dom'
import { searchCmsDocumentContent, type CmsDocumentSearchHit } from '../../api/cmsDocuments'
import { CmsAppShell } from '../../components/cms/CmsAppShell'

const { Title, Text } = Typography

function errorMessageOf(err: unknown, fallback: string): string {
  return (err as { response?: { data?: { message?: string } } })?.response?.data?.message || fallback
}

// To sang tu khoa trong doan trich. Dung JSX (khong ghep chuoi HTML) nen noi dung tai lieu
// khong the chen ma doc vao trang.
function highlight(text: string, q: string) {
  const index = text.toLowerCase().indexOf(q.toLowerCase())
  if (!q || index < 0) return text
  return (
    <>
      {text.slice(0, index)}
      <mark>{text.slice(index, index + q.length)}</mark>
      {text.slice(index + q.length)}
    </>
  )
}

/** UC0055 - tim cum tu trong noi dung cac tai lieu RAG dang duoc chatbot dung. */
export default function CmsDocumentSearchPage() {
  const [query, setQuery] = useState('')
  // Tu khoa cua lan tim gan nhat - de to sang dung chu da tim, ke ca khi nguoi dung
  // da go tiep vao o tim kiem ma chua bam Tim.
  const [searchedQuery, setSearchedQuery] = useState('')
  const [hits, setHits] = useState<CmsDocumentSearchHit[] | null>(null)
  const [truncated, setTruncated] = useState(false)
  const [searching, setSearching] = useState(false)

  async function handleSearch(value: string) {
    const q = value.trim()
    if (q.length < 2) {
      message.warning('Nhập ít nhất 2 ký tự để tìm trong nội dung')
      return
    }
    setSearching(true)
    try {
      const result = await searchCmsDocumentContent(q)
      setHits(result.items)
      setTruncated(result.truncated)
      setSearchedQuery(q)
    } catch (err) {
      message.error(errorMessageOf(err, 'Tìm kiếm thất bại'))
    } finally {
      setSearching(false)
    }
  }

  return (
    <CmsAppShell>
      <div className="mx-auto max-w-5xl">
        <div className="mb-6">
          <Text type="secondary">Kho tri thức</Text>
          <Title level={2} className="mb-1! mt-1!">
            <FileSearchOutlined className="mr-2" />
            Tìm trong nội dung tài liệu
          </Title>
          <Text type="secondary">
            Tra cứu một cụm từ xuất hiện ở đoạn nào trong các tài liệu chatbot đang dùng. Muốn tải lên hoặc
            quản lý tài liệu, vào <Link to="/cms/documents">Tài liệu y khoa</Link>.
          </Text>
        </div>

        <Card styles={{ body: { padding: 16 } }}>
          <Input.Search
            placeholder="VD: Helicobacter pylori, liều PPI..."
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            onSearch={handleSearch}
            loading={searching}
            enterButton="Tìm"
            allowClear
            maxLength={200}
          />
          <Text type="secondary" className="text-xs mt-1 block">
            Chỉ tìm trong phiên bản đang được chatbot dùng. Tìm có phân biệt dấu (gõ "dạ dày", không gõ "da day").
          </Text>

          {hits !== null && truncated && (
            <Alert
              type="warning"
              showIcon
              className="mt-3 text-xs"
              title="Chỉ hiển thị 50 kết quả đầu tiên. Hãy nhập từ khóa cụ thể hơn để thu hẹp kết quả."
            />
          )}

          {hits !== null && (
            <List
              className="mt-4"
              dataSource={hits}
              locale={{ emptyText: 'Không tìm thấy đoạn nào chứa cụm từ này' }}
              renderItem={(hit) => (
                <List.Item>
                  <List.Item.Meta
                    title={<span>{hit.title} <Text type="secondary" className="text-xs">· Đoạn #{hit.chunkIndex}</Text></span>}
                    description={highlight(hit.snippet, searchedQuery)}
                  />
                </List.Item>
              )}
            />
          )}
        </Card>
      </div>
    </CmsAppShell>
  )
}