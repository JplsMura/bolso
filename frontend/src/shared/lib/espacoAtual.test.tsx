import { renderHook } from '@testing-library/react'
import type { ReactNode } from 'react'
import { describe, expect, it, vi } from 'vitest'
import { type EspacoAtual, EspacoAtualContext, useEspacoAtual } from './espacoAtual'

const ESPACO: EspacoAtual = { id: '019a0000-0000-7000-8000-000000000012', tipo: 'COMPANY', nome: 'Empresa' }

describe('useEspacoAtual', () => {
  it('devolve o espaço que o layout colocou no contexto', () => {
    const wrapper = ({ children }: { children: ReactNode }) => (
      <EspacoAtualContext.Provider value={ESPACO}>{children}</EspacoAtualContext.Provider>
    )

    const { result } = renderHook(() => useEspacoAtual(), { wrapper })

    expect(result.current).toEqual(ESPACO)
  })

  it('lança um erro claro fora do layout de um espaço', () => {
    const consoleError = vi.spyOn(console, 'error').mockImplementation(() => {})

    expect(() => renderHook(() => useEspacoAtual())).toThrow(/dentro do layout de um espaço/)

    consoleError.mockRestore()
  })
})
