import { createContext, useContext, useEffect, useState, type ReactNode } from 'react'

export type ThemeMode = 'light' | 'dark'

interface ThemeState {
  mode: ThemeMode
  toggle: () => void
}

const STORAGE_KEY = 'gastroai.theme'

const ThemeContext = createContext<ThemeState | null>(null)

function loadStored(): ThemeMode {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw === 'dark' ? 'dark' : 'light'
  } catch {
    return 'light'
  }
}

export function ThemeProvider({ children }: { children: ReactNode }) {
  const [mode, setMode] = useState<ThemeMode>(loadStored)

  useEffect(() => {
    // Tailwind doc class "dark" tren <html> (xem @custom-variant trong index.css) de cac
    // class dark:... co tac dung; antd ConfigProvider (main.tsx) doc rieng "mode" nay de
    // doi algorithm - 2 co che chay song song, cung dong bo tu 1 state nay.
    document.documentElement.classList.toggle('dark', mode === 'dark')
    try {
      localStorage.setItem(STORAGE_KEY, mode)
    } catch {
      // best-effort - khong chan UI neu localStorage bi chan (vd private browsing)
    }
  }, [mode])

  function toggle() {
    setMode((current) => (current === 'dark' ? 'light' : 'dark'))
  }

  return <ThemeContext.Provider value={{ mode, toggle }}>{children}</ThemeContext.Provider>
}

export function useTheme() {
  const ctx = useContext(ThemeContext)
  if (!ctx) throw new Error('useTheme phải dùng bên trong ThemeProvider')
  return ctx
}
