import type { EspacoApi } from '@/features/identidade'
import type { TipoEspaco } from '@/shared/lib/espacoAtual'

export const ESPACOS = ['casa', 'empresa'] as const
export type Espaco = (typeof ESPACOS)[number]

export function ehEspaco(valor: string): valor is Espaco {
  return (ESPACOS as readonly string[]).includes(valor)
}

/** Rotas que o menu já sabe abrir. Cada feature acrescenta a sua quando a tela existe. */
export type RotaDoMenu = '/$espaco' | '/$espaco/lancamentos'

export type ItemMenu = {
  rotulo: string
  /** Sem rota, o item aparece apagado ("em breve"): a feature dele ainda não existe. */
  rota?: RotaDoMenu
  /** Aparece na barra inferior do celular. */
  noCelular?: boolean
}

/** Rótulo de reserva, usado só enquanto a API não devolveu o nome do espaço. */
export const ROTULO_ESPACO: Record<Espaco, string> = { casa: 'Casa', empresa: 'Empresa' }

/** O endereço usa casa/empresa; a API usa o tipo do espaço. */
export const TIPO_DO_ESPACO: Record<Espaco, TipoEspaco> = { casa: 'HOME', empresa: 'COMPANY' }

/** O espaço do usuário que corresponde ao trecho da URL, se a API devolveu um. */
export function acharEspaco(espacos: readonly EspacoApi[], espaco: Espaco): EspacoApi | undefined {
  return espacos.find((e) => e.tipo === TIPO_DO_ESPACO[espaco])
}

/** Nome de cada espaço para o seletor: o da API, ou o rótulo de reserva se o tipo não veio. */
export function nomesDosEspacos(espacos: readonly EspacoApi[]): Record<Espaco, string> {
  return {
    casa: acharEspaco(espacos, 'casa')?.nome ?? ROTULO_ESPACO.casa,
    empresa: acharEspaco(espacos, 'empresa')?.nome ?? ROTULO_ESPACO.empresa,
  }
}

export const MENU: Record<Espaco, ItemMenu[]> = {
  casa: [
    { rotulo: 'Início', rota: '/$espaco', noCelular: true },
    { rotulo: 'Lançamentos', rota: '/$espaco/lancamentos', noCelular: true },
    { rotulo: 'Presets' },
    { rotulo: 'Cartões', noCelular: true },
    { rotulo: 'Mês', noCelular: true },
    { rotulo: 'Tags' },
    { rotulo: 'Configurações' },
  ],
  empresa: [
    { rotulo: 'Início', rota: '/$espaco', noCelular: true },
    { rotulo: 'Teto do MEI', noCelular: true },
    { rotulo: 'Notas', noCelular: true },
    { rotulo: 'Custos e repasse', noCelular: true },
    { rotulo: 'Configurações' },
  ],
}
