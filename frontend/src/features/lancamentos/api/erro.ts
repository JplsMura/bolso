/** Resposta de erro da API (problem+json): o status decide o que mostrar; `detalhe` vem pronto do servidor. */
export class ErroDaApi extends Error {
  readonly status: number

  constructor(status: number, mensagem: string) {
    super(mensagem)
    this.name = 'ErroDaApi'
    this.status = status
  }
}

export const ehConflito = (erro: unknown) => erro instanceof ErroDaApi && erro.status === 409
export const ehNaoEncontrado = (erro: unknown) => erro instanceof ErroDaApi && erro.status === 404

/** A mensagem para o usuário: o `detail` do servidor nas regras de negócio (422), um texto fixo no resto. */
export function mensagemDoErro(erro: unknown, padrao: string): string {
  return erro instanceof ErroDaApi && erro.status === 422 ? erro.message : padrao
}

/** Transforma uma resposta que não foi 2xx em ErroDaApi. */
export function falhar(response: Response, corpo: unknown, padrao: string): never {
  const detalhe = (corpo as { detail?: unknown } | null | undefined)?.detail
  throw new ErroDaApi(response.status, typeof detalhe === 'string' && detalhe ? detalhe : padrao)
}
