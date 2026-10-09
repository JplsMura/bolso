import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterAll, afterEach, beforeAll } from 'vitest'
import { server } from './msw/server'

beforeAll(() => server.listen({ onUnhandledFrame: 'error' }))
afterEach(() => {
  server.resetHandlers()
  cleanup()
})
afterAll(() => server.close())

// jsdom não implementa estes métodos; o router (scroll) e o axe (canvas) chamam.
if (typeof window !== 'undefined') {
  window.scrollTo = () => {}
  HTMLCanvasElement.prototype.getContext = () => null
}
