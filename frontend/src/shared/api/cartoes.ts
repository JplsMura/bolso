import { useQuery } from '@tanstack/react-query'
import { api } from './client'
import { falhar } from './erro'
import type { components } from './schema'

export type CartaoApi = components['schemas']['CartaoResponse']

/**
 * Raiz das chaves de cache dos cartões e faturas. Quem grava um gasto no cartão invalida esta raiz: limite usado e
 * faturas mudam junto.
 */
export const chaveDosCartoes = (espacoId: string) => ['cartoes', espacoId] as const

/** Cartões ativos com limite usado e disponível. `ativo` falso não consulta. */
export function useCartoes(espacoId: string, ativo = true) {
  return useQuery({
    queryKey: [...chaveDosCartoes(espacoId), 'lista'] as const,
    enabled: ativo,
    queryFn: async (): Promise<CartaoApi[]> => {
      const { data, error, response } = await api.GET('/api/v1/espacos/{espacoId}/cartoes', {
        params: { path: { espacoId } },
      })
      if (!response.ok || !data) falhar(response, error, 'Não foi possível carregar os cartões.')
      return data
    },
  })
}
