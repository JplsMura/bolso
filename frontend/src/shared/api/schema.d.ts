// Gerado por `npm run gen:api` a partir de /v3/api-docs. Não editar à mão.
// Escrito à mão na spec 002 para espelhar o contrato; rode `npm run gen:api` com a API no ar e confira que a diferença é nula.

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
}
