import type { EspacoApi } from '@/features/identidade'
import type { TipoEspaco } from '@/shared/lib/espacoAtual'

export const ESPACOS = ['casa', 'empresa'] as const
export type Espaco = (typeof ESPACOS)[number]

export function ehEspaco(valor: string): valor is Espaco {
  return (ESPACOS as readonly string[]).includes(valor)
}

export type ItemMenu = {
  rotulo: string
  /** Só a página inicial existe até aqui; os outros itens ganham rota na feature correspondente. */
  disponivel: boolean
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
    { rotulo: 'Início', disponivel: true, noCelular: true },
    { rotulo: 'Lançamentos', disponivel: false, noCelular: true },
    { rotulo: 'Presets', disponivel: false },
    { rotulo: 'Cartões', disponivel: false, noCelular: true },
    { rotulo: 'Mês', disponivel: false, noCelular: true },
    { rotulo: 'Tags', disponivel: false },
    { rotulo: 'Configurações', disponivel: false },
  ],
  empresa: [
    { rotulo: 'Início', disponivel: true, noCelular: true },
    { rotulo: 'Teto do MEI', disponivel: false, noCelular: true },
    { rotulo: 'Notas', disponivel: false, noCelular: true },
    { rotulo: 'Custos e repasse', disponivel: false, noCelular: true },
    { rotulo: 'Configurações', disponivel: false },
  ],
}
