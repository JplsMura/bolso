/**
 * Resposta de erro da API (problem+json): o status decide o que mostrar; `detalhe` vem pronto do servidor.
 * `tipo` é o `type` do problema quando o servidor manda um (por exemplo, fatura fechada).
 */
export class ErroDaApi extends Error {
  readonly status: number
  readonly tipo: string | undefined

  constructor(status: number, mensagem: string, tipo?: string) {
    super(mensagem)
    this.name = 'ErroDaApi'
    this.status = status
    this.tipo = tipo
  }
}

export const ehConflito = (erro: unknown) => erro instanceof ErroDaApi && erro.status === 409
export const ehNaoEncontrado = (erro: unknown) => erro instanceof ErroDaApi && erro.status === 404

/** O gasto mexeria numa fatura fechada ou paga: a tela oferece o atalho para Cartões. */
export const ehFaturaFechada = (erro: unknown) =>
  erro instanceof ErroDaApi && erro.tipo === 'urn:bolso:problema:fatura-fechada'

/** A mensagem para o usuário: o `detail` do servidor nas regras de negócio (422), um texto fixo no resto. */
export function mensagemDoErro(erro: unknown, padrao: string): string {
  return erro instanceof ErroDaApi && erro.status === 422 ? erro.message : padrao
}

/** Transforma uma resposta que não foi 2xx em ErroDaApi. */
export function falhar(response: Response, corpo: unknown, padrao: string): never {
  const problema = corpo as { detail?: unknown; type?: unknown } | null | undefined
  const detalhe = problema?.detail
  const tipo = typeof problema?.type === 'string' ? problema.type : undefined
  throw new ErroDaApi(response.status, typeof detalhe === 'string' && detalhe ? detalhe : padrao, tipo)
}
