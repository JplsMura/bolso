import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { delay, http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { violacoesDeAcessibilidade } from '@/test/axe'
import { ESPACOS_PADRAO, URL_ESPACOS } from '@/test/msw/handlers'
import { server } from '@/test/msw/server'
import { renderizarRota } from '@/test/renderizarRota'

const NOMES_DA_API = [
  { ...ESPACOS_PADRAO[0], nome: 'Minha casa' },
  { ...ESPACOS_PADRAO[1], nome: 'Minha firma' },
]

describe('espaços vindos da API', () => {
  it('mostra no seletor e no início os nomes que a API devolveu', async () => {
    server.use(http.get(URL_ESPACOS, () => HttpResponse.json(NOMES_DA_API)))
    const usuario = userEvent.setup()
    renderizarRota('/casa')
    const menu = await screen.findByRole('navigation', { name: 'Principal' })

    expect(screen.getByRole('main')).toHaveTextContent('Minha casa')
    await usuario.click(within(menu).getByRole('button', { name: 'Trocar de espaço, Minha casa' }))
    expect(within(menu).getByRole('link', { name: 'Minha casa' })).toBeInTheDocument()
    expect(within(menu).getByRole('link', { name: 'Minha firma' })).toBeInTheDocument()
  })

  it('escolher a Empresa no seletor leva a /empresa e mostra o nome da API', async () => {
    server.use(http.get(URL_ESPACOS, () => HttpResponse.json(NOMES_DA_API)))
    const usuario = userEvent.setup()
    const { router } = renderizarRota('/casa')
    const menu = await screen.findByRole('navigation', { name: 'Principal' })

    await usuario.click(within(menu).getByRole('button', { name: /Trocar de espaço/ }))
    await usuario.click(within(menu).getByRole('link', { name: 'Minha firma' }))

    await waitFor(() => expect(router.state.location.pathname).toBe('/empresa'))
    expect(within(menu).getByRole('button', { name: 'Trocar de espaço, Minha firma' })).toBeInTheDocument()
    expect(screen.getByRole('main')).toHaveTextContent('Minha firma')
  })

  it('mostra o aviso de carregando até a API responder', async () => {
    server.use(
      http.get(URL_ESPACOS, async () => {
        await delay(150)
        return HttpResponse.json(ESPACOS_PADRAO)
      }),
    )
    renderizarRota('/casa')

    expect(await screen.findByRole('status')).toHaveTextContent('Carregando')
    expect(await screen.findByRole('navigation', { name: 'Principal' })).toBeInTheDocument()
    expect(screen.queryByRole('status')).not.toBeInTheDocument()
  })

  it('mostra o alerta quando a API falha e "Tentar de novo" refaz a chamada', async () => {
    const usuario = userEvent.setup()
    server.use(http.get(URL_ESPACOS, () => new HttpResponse(null, { status: 500 }), { once: true }))
    renderizarRota('/casa')

    expect(await screen.findByRole('alert')).toHaveTextContent('Confira se a API está no ar')
    await usuario.click(screen.getByRole('button', { name: 'Tentar de novo' }))

    expect(await screen.findByRole('navigation', { name: 'Principal' })).toBeInTheDocument()
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })

  it('se a API devolve só a Casa, /empresa dá erro e /casa continua funcionando', async () => {
    server.use(http.get(URL_ESPACOS, () => HttpResponse.json([ESPACOS_PADRAO[0]])))

    const empresa = renderizarRota('/empresa')
    expect(await screen.findByRole('alert')).toBeInTheDocument()
    expect(screen.queryByRole('navigation', { name: 'Principal' })).not.toBeInTheDocument()
    empresa.unmount()

    renderizarRota('/casa')
    expect(await screen.findByRole('navigation', { name: 'Principal' })).toBeInTheDocument()
  })

  it('não tem violações de acessibilidade nos estados de carregando e de erro', async () => {
    server.use(
      http.get(URL_ESPACOS, async () => {
        await delay(150)
        return new HttpResponse(null, { status: 500 })
      }),
    )
    const { container } = renderizarRota('/casa')

    await screen.findByRole('status')
    expect(await violacoesDeAcessibilidade(container)).toEqual([])

    await screen.findByRole('alert')
    expect(await violacoesDeAcessibilidade(container)).toEqual([])
  })
})
