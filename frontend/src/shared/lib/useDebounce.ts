import { useEffect, useState } from 'react'

/** O valor só "assenta" depois de `atrasoMs` sem mudar: evita uma consulta por tecla digitada. */
export function useDebounce<T>(valor: T, atrasoMs = 300): T {
  const [assentado, setAssentado] = useState(valor)
  useEffect(() => {
    const id = setTimeout(() => setAssentado(valor), atrasoMs)
    return () => clearTimeout(id)
  }, [valor, atrasoMs])
  return assentado
}
