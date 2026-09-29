/** Định dạng ISO datetime -> "DD/MM/YYYY HH:mm" theo giờ Việt Nam, dùng chung nhiều trang. */
export function formatDateTime(iso: string): string {
  return new Date(iso).toLocaleString('vi-VN', {
    day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit',
  })
}
