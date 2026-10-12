import { useCallback, useEffect, useRef, useState } from 'react'

export interface PagedListResponse<Item> {
  items: Item[]
  totalElements: number
}

interface UsePagedListOptions<Item> {
  fetchPage: (page: number, size: number) => Promise<PagedListResponse<Item>>
  pageSize: number
  loadErrorMessage: string
}

/** Loads a paged list and ignores an obsolete response after a page change. */
export function usePagedList<Item>({
  fetchPage,
  pageSize,
  loadErrorMessage,
}: UsePagedListOptions<Item>) {
  const [items, setItems] = useState<Item[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [page, setPage] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const requestId = useRef(0)

  const reload = useCallback(async () => {
    const currentRequestId = ++requestId.current
    setLoading(true)
    setError(null)
    try {
      const response = await fetchPage(page, pageSize)
      if (currentRequestId !== requestId.current) return
      setItems(response.items)
      setTotalElements(response.totalElements)
    } catch (err) {
      if (currentRequestId === requestId.current) {
        setError(err instanceof Error ? err.message : loadErrorMessage)
      }
    } finally {
      if (currentRequestId === requestId.current) setLoading(false)
    }
  }, [fetchPage, loadErrorMessage, page, pageSize])

  useEffect(() => {
    void reload()
  }, [reload])

  return {
    items,
    loading,
    error,
    setError,
    page,
    setPage,
    pageSize,
    totalElements,
    reload,
  }
}
