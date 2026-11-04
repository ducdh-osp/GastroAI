export function isAtMostUtf8Bytes(value: string, maxBytes: number): boolean {
  return new TextEncoder().encode(value).length <= maxBytes
}

export const PASSWORD_REQUIREMENTS_MESSAGE =
  'Mật khẩu cần tối thiểu 8 ký tự, ít nhất 1 chữ in hoa, 1 chữ số và 1 ký tự đặc biệt'

/** Quy tắc mật khẩu dùng chung cho đăng ký, đặt lại và đổi mật khẩu bệnh nhân. */
export function meetsPasswordRequirements(value: string): boolean {
  return value.length >= 8
    && /[A-Z]/.test(value)
    && /[0-9]/.test(value)
    && /[^A-Za-z0-9]/.test(value)
}
