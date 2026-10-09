import { describe, expect, it } from 'vitest'
import { ESPACOS_PADRAO } from '@/test/msw/handlers'
import { acharEspaco, nomesDosEspacos, TIPO_DO_ESPACO } from './espacos'

const [CASA, EMPRESA] = ESPACOS_PADRAO

describe('espaço da URL', () => {
  it('casa é o tipo HOME e empresa é o tipo COMPANY', () => {
    expect(TIPO_DO_ESPACO).toEqual({ casa: 'HOME', empresa: 'COMPANY' })
  })

  it('acha o espaço do tipo da URL, com o id da API', () => {
    expect(acharEspaco(ESPACOS_PADRAO, 'casa')?.id).toBe(CASA.id)
    expect(acharEspaco(ESPACOS_PADRAO, 'empresa')?.id).toBe(EMPRESA.id)
  })

  it('não acha quando a API não devolveu aquele tipo', () => {
    expect(acharEspaco([CASA], 'empresa')).toBeUndefined()
  })

  it('usa os nomes da API e o rótulo de reserva para o tipo que faltou', () => {
    expect(nomesDosEspacos([{ ...CASA, nome: 'Minha casa' }])).toEqual({
      casa: 'Minha casa',
      empresa: 'Empresa',
    })
  })
})
