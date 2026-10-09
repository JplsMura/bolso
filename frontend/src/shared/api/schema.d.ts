// Gerado por `npm run gen:api` a partir de /v3/api-docs. Não editar à mão.
// Escrito à mão (specs 002 e 003) para espelhar o contrato; rode `npm run gen:api` com a API no ar e confira que a diferença é nula.

export interface paths {
  '/api/v1/espacos': {
    parameters: {
      query?: never
      header?: never
      path?: never
      cookie?: never
    }
    get: operations['listarEspacos']
    put?: never
    post?: never
    delete?: never
    options?: never
    head?: never
    patch?: never
    trace?: never
  }
  '/api/v1/espacos/{id}': {
    parameters: {
      query?: never
      header?: never
      path?: never
      cookie?: never
    }
    get: operations['buscarEspaco']
    put?: never
    post?: never
    delete?: never
    options?: never
    head?: never
    patch?: never
    trace?: never
  }
  '/api/v1/espacos/{espacoId}/lancamentos': {
    parameters: {
      query?: never
      header?: never
      path?: never
      cookie?: never
    }
    get: operations['listarLancamentos']
    put?: never
    post: operations['criarLancamento']
    delete?: never
    options?: never
    head?: never
    patch?: never
    trace?: never
  }
  '/api/v1/espacos/{espacoId}/lancamentos/{id}': {
    parameters: {
      query?: never
      header?: never
      path?: never
      cookie?: never
    }
    get: operations['buscarLancamento']
    put: operations['editarLancamento']
    post?: never
    delete: operations['excluirLancamento']
    options?: never
    head?: never
    patch?: never
    trace?: never
  }
  '/api/v1/espacos/{espacoId}/tags': {
    parameters: {
      query?: never
      header?: never
      path?: never
      cookie?: never
    }
    get: operations['listarTags']
    put?: never
    post: operations['criarTag']
    delete?: never
    options?: never
    head?: never
    patch?: never
    trace?: never
  }
  '/api/v1/espacos/{espacoId}/metas': {
    parameters: {
      query?: never
      header?: never
      path?: never
      cookie?: never
    }
    get: operations['listarMetas']
    put?: never
    post?: never
    delete?: never
    options?: never
    head?: never
    patch?: never
    trace?: never
  }
}
export type webhooks = Record<string, never>
export interface components {
  schemas: {
    EspacoResponse: {
      /** Format: uuid */
      id: string
      /** @enum {string} */
      tipo: 'HOME' | 'COMPANY'
      nome: string
      /** @enum {string} */
      papel: 'OWNER' | 'EDITOR' | 'VIEWER'
    }
    EditarLancamentoRequest: {
      /** @enum {string} */
      direcao: 'IN' | 'OUT'
      valor: string
      descricao: string
      /** Format: date */
      data: string
      /** Format: uuid */
      metaId?: string
      /** @enum {string} */
      formaPagamento?: 'PIX' | 'DEBIT' | 'CREDIT' | 'CASH' | 'BOLETO' | 'TRANSFER' | 'OTHER'
      tagIds?: string[]
      /** Format: int64 */
      versao: number
    }
    NovoLancamentoRequest: {
      /** @enum {string} */
      direcao: 'IN' | 'OUT'
      valor: string
      descricao: string
      /** Format: date */
      data: string
      /** Format: uuid */
      metaId?: string
      /** @enum {string} */
      formaPagamento?: 'PIX' | 'DEBIT' | 'CREDIT' | 'CASH' | 'BOLETO' | 'TRANSFER' | 'OTHER'
      tagIds?: string[]
    }
    LancamentoResponse: {
      /** Format: uuid */
      id: string
      /** @enum {string} */
      direcao: 'IN' | 'OUT'
      valor: string
      descricao: string
      /** Format: date */
      data: string
      mesReferencia: string
      /** Format: uuid */
      metaId?: string
      /** @enum {string} */
      formaPagamento?: 'PIX' | 'DEBIT' | 'CREDIT' | 'CASH' | 'BOLETO' | 'TRANSFER' | 'OTHER'
      tags: components['schemas']['TagResumoResponse'][]
      /** Format: int64 */
      versao: number
    }
    TagResumoResponse: {
      /** Format: uuid */
      id: string
      nome: string
    }
    ListaDoMesResponse: {
      mes: string
      totais: components['schemas']['TotaisResponse']
      itens: components['schemas']['LancamentoResponse'][]
    }
    TotaisResponse: {
      entradas: string
      saidas: string
      sobra: string
    }
    NovaTagRequest: {
      nome: string
      /** @enum {string} */
      tipoCusto?: 'FIXED' | 'VARIABLE'
    }
    TagResponse: {
      /** Format: uuid */
      id: string
      nome: string
      /** @enum {string} */
      tipoCusto?: 'FIXED' | 'VARIABLE'
    }
    MetaResponse: {
      /** Format: uuid */
      id: string
      nome: string
      cor: string
      /** Format: int32 */
      ordem: number
    }
  }
  responses: never
  parameters: never
  requestBodies: never
  headers: never
  pathItems: never
}
export type $defs = Record<string, never>
export interface operations {
  listarEspacos: {
    parameters: {
      query?: never
      header?: never
      path?: never
      cookie?: never
    }
    requestBody?: never
    responses: {
      /** @description OK */
      200: {
        headers: {
          [name: string]: unknown
        }
        content: {
          'application/json': components['schemas']['EspacoResponse'][]
        }
      }
    }
  }
  buscarEspaco: {
    parameters: {
      query?: never
      header?: never
      path: {
        id: string
      }
      cookie?: never
    }
    requestBody?: never
    responses: {
      /** @description OK */
      200: {
        headers: {
          [name: string]: unknown
        }
        content: {
          'application/json': components['schemas']['EspacoResponse']
        }
      }
    }
  }
  listarLancamentos: {
    parameters: {
      query?: {
        mes?: string
        q?: string
        metaId?: string
        tagId?: string
        pagamento?: 'PIX' | 'DEBIT' | 'CREDIT' | 'CASH' | 'BOLETO' | 'TRANSFER' | 'OTHER'
        direcao?: 'IN' | 'OUT'
      }
      header?: never
      path: {
        espacoId: string
      }
      cookie?: never
    }
    requestBody?: never
    responses: {
      /** @description OK */
      200: {
        headers: {
          [name: string]: unknown
        }
        content: {
          'application/json': components['schemas']['ListaDoMesResponse']
        }
      }
    }
  }
  criarLancamento: {
    parameters: {
      query?: never
      header?: never
      path: {
        espacoId: string
      }
      cookie?: never
    }
    requestBody: {
      content: {
        'application/json': components['schemas']['NovoLancamentoRequest']
      }
    }
    responses: {
      /** @description Created */
      201: {
        headers: {
          [name: string]: unknown
        }
        content: {
          'application/json': components['schemas']['LancamentoResponse']
        }
      }
    }
  }
  buscarLancamento: {
    parameters: {
      query?: never
      header?: never
      path: {
        espacoId: string
        id: string
      }
      cookie?: never
    }
    requestBody?: never
    responses: {
      /** @description OK */
      200: {
        headers: {
          [name: string]: unknown
        }
        content: {
          'application/json': components['schemas']['LancamentoResponse']
        }
      }
    }
  }
  editarLancamento: {
    parameters: {
      query?: never
      header?: never
      path: {
        espacoId: string
        id: string
      }
      cookie?: never
    }
    requestBody: {
      content: {
        'application/json': components['schemas']['EditarLancamentoRequest']
      }
    }
    responses: {
      /** @description OK */
      200: {
        headers: {
          [name: string]: unknown
        }
        content: {
          'application/json': components['schemas']['LancamentoResponse']
        }
      }
    }
  }
  excluirLancamento: {
    parameters: {
      query?: never
      header?: never
      path: {
        espacoId: string
        id: string
      }
      cookie?: never
    }
    requestBody?: never
    responses: {
      /** @description No Content */
      204: {
        headers: {
          [name: string]: unknown
        }
        content?: never
      }
    }
  }
  listarTags: {
    parameters: {
      query?: never
      header?: never
      path: {
        espacoId: string
      }
      cookie?: never
    }
    requestBody?: never
    responses: {
      /** @description OK */
      200: {
        headers: {
          [name: string]: unknown
        }
        content: {
          'application/json': components['schemas']['TagResponse'][]
        }
      }
    }
  }
  criarTag: {
    parameters: {
      query?: never
      header?: never
      path: {
        espacoId: string
      }
      cookie?: never
    }
    requestBody: {
      content: {
        'application/json': components['schemas']['NovaTagRequest']
      }
    }
    responses: {
      /** @description Created */
      201: {
        headers: {
          [name: string]: unknown
        }
        content: {
          'application/json': components['schemas']['TagResponse']
        }
      }
    }
  }
  listarMetas: {
    parameters: {
      query?: never
      header?: never
      path: {
        espacoId: string
      }
      cookie?: never
    }
    requestBody?: never
    responses: {
      /** @description OK */
      200: {
        headers: {
          [name: string]: unknown
        }
        content: {
          'application/json': components['schemas']['MetaResponse'][]
        }
      }
    }
  }
}
