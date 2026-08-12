import { cleanup, render } from '@testing-library/react'
import { act } from 'react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import ScrollReveal from './ScrollReveal'

let intersectionCallback: IntersectionObserverCallback

class IntersectionObserverMock implements IntersectionObserver {
  readonly root = null
  readonly rootMargin = '0px'
  readonly scrollMargin = '0px'
  readonly thresholds = [0.2]
  disconnect = vi.fn()
  observe = vi.fn()
  takeRecords = vi.fn(() => [])
  unobserve = vi.fn()

  constructor(callback: IntersectionObserverCallback) {
    intersectionCallback = callback
  }
}

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
})

describe('ScrollReveal', () => {
  it('reveals its content when it enters the viewport', () => {
    vi.stubGlobal('IntersectionObserver', IntersectionObserverMock)

    const { getByText } = render(
      <ScrollReveal direction="left">Voice control</ScrollReveal>,
    )
    const element = getByText('Voice control')

    expect(element.classList.contains('is-visible')).toBe(false)

    act(() => {
      intersectionCallback(
        [
          {
            isIntersecting: true,
            target: element,
          } as unknown as IntersectionObserverEntry,
        ],
        {} as IntersectionObserver,
      )
    })

    expect(element.classList.contains('is-visible')).toBe(true)
  })
})
