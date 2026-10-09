import { useQuery } from '@tanstack/react-query'
import { api } from '@/shared/api/client'
import type { components } from '@/shared/api/schema'

export type EspacoApi = components['schemas']['EspacoResponse']

export const CHAVE_ESPACOS = ['espacos'] as const

/** Espaços do usuário atual (Casa primeiro). Quem manda na ordem e no nome é a API. */
export function useEspacos() {
  return useQuery({
    queryKey: CHAVE_ESPACOS,
    queryFn: async (): Promise<EspacoApi[]> => {
      const { data, response } = await api.GET('/api/v1/espacos')
      if (!response.ok || !data) throw new Error(`Não foi possível carregar os espaços (${response.status}).`)
      return data
    },
  })
}
