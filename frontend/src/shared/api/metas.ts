import { useQuery } from '@tanstack/react-query'
import { api } from './client'
import { falhar } from './erro'
import type { components } from './schema'

export type MetaApi = components['schemas']['MetaResponse']

/** As 6 metas da Casa (lançamentos e cartões mostram). `ativo` falso não consulta (a Empresa não tem metas). */
export function useMetas(espacoId: string, ativo = true) {
  return useQuery({
    queryKey: ['metas', espacoId] as const,
    enabled: ativo,
    queryFn: async (): Promise<MetaApi[]> => {
      const { data, error, response } = await api.GET('/api/v1/espacos/{espacoId}/metas', {
        params: { path: { espacoId } },
      })
      if (!response.ok || !data) falhar(response, error, 'Não foi possível carregar as metas.')
      return data
    },
  })
}
