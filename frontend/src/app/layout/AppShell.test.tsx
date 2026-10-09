import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { violacoesDeAcessibilidade } from '@/test/axe'
import { renderizarRota } from '@/test/renderizarRota'

describe('AppShell', () => {
  it('abre na Casa quando a URL é a raiz', async () => {
    const { router } = renderizarRota('/')
    await waitFor(() => expect(router.state.location.pathname).toBe('/casa'))
    expect(await screen.findByRole('heading', { name: 'Início' })).toBeInTheDocument()
  })

  it('troca de Casa para Empresa pelo seletor e muda a URL e o menu', async () => {
    const usuario = userEvent.setup()
    const { router } = renderizarRota('/casa')
    const menu = await screen.findByRole('navigation', { name: 'Principal' })
    expect(within(menu).getByText('Lançamentos')).toBeInTheDocument()

    await usuario.click(within(menu).getByRole('button', { name: 'Trocar de espaço, Casa' }))
    await usuario.click(within(menu).getByRole('link', { name: 'Empresa' }))

    await waitFor(() => expect(router.state.location.pathname).toBe('/empresa'))
    expect(within(menu).getByRole('button', { name: 'Trocar de espaço, Empresa' })).toBeInTheDocument()
    expect(within(menu).getByText('Teto do MEI')).toBeInTheDocument()
    expect(within(menu).queryByText('Lançamentos')).not.toBeInTheDocument()
  })

  it('fecha o seletor com Esc e devolve o foco ao botão', async () => {
    const usuario = userEvent.setup()
    renderizarRota('/casa')
    const menu = await screen.findByRole('navigation', { name: 'Principal' })
    const botao = within(menu).getByRole('button', { name: 'Trocar de espaço, Casa' })

    await usuario.click(botao)
    expect(within(menu).getByRole('link', { name: 'Empresa' })).toBeInTheDocument()
    await usuario.keyboard('{Escape}')

    expect(within(menu).queryByRole('link', { name: 'Empresa' })).not.toBeInTheDocument()
    expect(botao).toHaveFocus()
  })

  it('mostra 404 para espaço que não existe', async () => {
    renderizarRota('/escritorio')
    expect(await screen.findByRole('heading', { name: 'Página não encontrada' })).toBeInTheDocument()
  })

  it('não tem violações de acessibilidade, com o seletor fechado e aberto', async () => {
    const usuario = userEvent.setup()
    const { container } = renderizarRota('/casa')
    const menu = await screen.findByRole('navigation', { name: 'Principal' })
    expect(await violacoesDeAcessibilidade(container)).toEqual([])

    await usuario.click(within(menu).getByRole('button', { name: /Trocar de espaço/ }))
    expect(await violacoesDeAcessibilidade(container)).toEqual([])
  })
})
