import dayjs, { type Dayjs } from 'dayjs'

export function disableFutureTime(value: Dayjs | null) {
  const now = dayjs()
  if (!value || !value.isSame(now, 'day')) return {}

  return {
    disabledHours: () => Array.from({ length: 24 }, (_, hour) => hour).filter((hour) => hour > now.hour()),
    disabledMinutes: (hour: number) => hour === now.hour()
      ? Array.from({ length: 60 }, (_, minute) => minute).filter((minute) => minute > now.minute())
      : [],
  }
}

export function isFutureDateTime(value: Dayjs | null | undefined) {
  return Boolean(value?.isAfter(dayjs()))
}
