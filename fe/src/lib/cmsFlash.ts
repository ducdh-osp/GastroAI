// Thông báo hiện 1 lần ở trang CMS kế tiếp; sessionStorage vì có lúc phải tải lại cả trang.
const FLASH_KEY = 'gastroai.cms.flash'

export const CMS_FORBIDDEN_MESSAGE = 'Bạn không có quyền truy cập chức năng này'

export function setCmsFlash(text: string) {
  try {
    sessionStorage.setItem(FLASH_KEY, text)
  } catch {
    // ignore
  }
}

export function takeCmsFlash(): string | null {
  try {
    const text = sessionStorage.getItem(FLASH_KEY)
    sessionStorage.removeItem(FLASH_KEY)
    return text
  } catch {
    return null
  }
}
