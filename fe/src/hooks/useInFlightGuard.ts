import { useCallback, useRef, useState } from 'react'

/**
 * Serializes a row action while it is being sent to the API.
 *
 * A ref is used in addition to state so two clicks in the same render frame
 * cannot start duplicate requests before React has rendered the disabled UI.
 */
export function useInFlightGuard<Id>() {
  const inFlightIdRef = useRef<Id | null>(null)
  const [inFlightId, setInFlightId] = useState<Id | null>(null)

  const run = useCallback(async <Result,>(id: Id, action: () => Promise<Result>): Promise<Result | undefined> => {
    if (inFlightIdRef.current !== null) return undefined

    inFlightIdRef.current = id
    setInFlightId(id)
    try {
      return await action()
    } finally {
      inFlightIdRef.current = null
      setInFlightId(null)
    }
  }, [])

  return { inFlightId, run }
}
