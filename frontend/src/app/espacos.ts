export const ESPACOS = ['casa', 'empresa'] as const
export type Espaco = (typeof ESPACOS)[number]

export function ehEspaco(valor: string): valor is Espaco {
  return (ESPACOS as readonly string[]).includes(valor)
}

export type ItemMenu = {
  rotulo: string
  /** Só a página inicial existe na 001; os outros itens ganham rota na feature correspondente. */
  disponivel: boolean
  /** Aparece na barra inferior do celular. */
  noCelular?: boolean
}

export const ROTULO_ESPACO: Record<Espaco, string> = { casa: 'Casa', empresa: 'Empresa' }

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
