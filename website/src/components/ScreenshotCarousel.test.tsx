import { cleanup, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it } from 'vitest'
import ScreenshotCarousel from './ScreenshotCarousel'

const images = ['/screen1.png', '/screen2.png', '/screen3.png']

afterEach(cleanup)

describe('ScreenshotCarousel', () => {
  it('renders every screenshot and starts with only the first active', () => {
    const { container } = render(
      <ScreenshotCarousel images={images} autoPlayInterval={0} />,
    )

    expect(screen.getAllByRole('img')).toHaveLength(3)
    expect(container.querySelectorAll('.carousel-item.active')).toHaveLength(1)
    expect(
      container.querySelector('.carousel-item.active img')?.getAttribute('src'),
    ).toBe('/screen1.png')
  })

  it('connects the Bootstrap controls to the carousel', () => {
    render(<ScreenshotCarousel images={images} autoPlayInterval={0} />)

    const previousButton = screen.getByRole('button', { name: 'Previous' })
    const nextButton = screen.getByRole('button', { name: 'Next' })

    expect(previousButton.getAttribute('data-bs-target')).toBe(
      '#screenshotCarousel',
    )
    expect(previousButton.getAttribute('data-bs-slide')).toBe('prev')
    expect(nextButton.getAttribute('data-bs-target')).toBe(
      '#screenshotCarousel',
    )
    expect(nextButton.getAttribute('data-bs-slide')).toBe('next')
  })
})
