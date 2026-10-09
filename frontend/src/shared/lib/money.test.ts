import { describe, expect, it } from 'vitest'
import { formatarBRL, paraCentavos, paraValorApi } from './money'

describe('paraCentavos', () => {
  it.each([
    ['110.00', 11000],
    ['0.10', 10],
    ['0.1', 10],
    ['1234567.89', 123456789],
    ['7', 700],
    ['-15.50', -1550],
    ['-0.00', 0],
  ])('%s → %i', (entrada, esperado) => {
    expect(paraCentavos(entrada)).toBe(esperado)
  })

  it.each(['', 'abc', '1,50', '1.234', '10.', '.50', 'R$ 10,00', '1e3', 'NaN', '99999999999999999'])(
    'rejeita "%s"',
    (entrada) => {
      expect(() => paraCentavos(entrada)).toThrow()
    },
  )

  it('não sofre com a soma em ponto flutuante', () => {
    // 0.1 + 0.2 em float dá 0.30000000000000004
    expect(paraCentavos('0.10') + paraCentavos('0.20')).toBe(paraCentavos('0.30'))
  })
})

describe('paraValorApi', () => {
  it.each([
    [11000, '110.00'],
    [10, '0.10'],
    [5, '0.05'],
    [-1550, '-15.50'],
    [0, '0.00'],
  ])('%i → %s', (entrada, esperado) => {
    expect(paraValorApi(entrada)).toBe(esperado)
  })

  it('faz o caminho de volta', () => {
    expect(paraCentavos(paraValorApi(123456789))).toBe(123456789)
  })

  it('rejeita centavos fracionados', () => {
    expect(() => paraValorApi(10.5)).toThrow()
  })
})

describe('formatarBRL', () => {
  it('formata em pt-BR', () => {
    expect(formatarBRL(11000)).toBe('R$ 110,00')
    expect(formatarBRL(123456789)).toBe('R$ 1.234.567,89')
    expect(formatarBRL(-1550)).toBe('-R$ 15,50')
  })
})
