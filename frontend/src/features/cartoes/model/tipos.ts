import type { components } from '@/shared/api/schema'

export type CartaoApi = components['schemas']['CartaoResponse']
export type NovoCartaoApi = components['schemas']['NovoCartaoRequest']
export type EditarCartaoApi = components['schemas']['EditarCartaoRequest']
export type FaturasDoMesApi = components['schemas']['FaturasDoMesResponse']
export type FaturaDoCartaoApi = components['schemas']['FaturaDoCartaoResponse']
export type FaturaApi = components['schemas']['FaturaResponse']
export type ItemDaFaturaApi = components['schemas']['ItemDaFaturaResponse']
export type StatusDaFatura = FaturaApi['status']
export type MetaApi = components['schemas']['MetaResponse']

/** As mudanças de status, com o caminho da API. */
export type AcaoDaFatura = 'fechar' | 'pagar' | 'reabrir' | 'desfazer-pagamento'
