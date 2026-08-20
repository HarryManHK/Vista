import { useEffect, useRef } from 'react'
import { Carousel } from 'bootstrap'

type ScreenshotCarouselProps = {
  images: string[]
  autoPlayInterval?: number
  carouselId?: string
  title?: string
  variant?: 'section' | 'embedded'
}

function ScreenshotCarousel({
  images,
  autoPlayInterval = 5000,
  carouselId = 'screenshotCarousel',
  title = 'Screenshots',
  variant = 'section',
}: ScreenshotCarouselProps) {
  const carouselRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!carouselRef.current || images.length < 2) return

    const prefersReducedMotion = window.matchMedia?.(
      '(prefers-reduced-motion: reduce)',
    ).matches
    const interval =
      autoPlayInterval > 0 && !prefersReducedMotion ? autoPlayInterval : false

    const carousel = new Carousel(carouselRef.current, {
      interval,
      keyboard: true,
      pause: 'hover',
      ride: interval === false ? false : 'carousel',
      touch: true,
      wrap: true,
    })

    return () => carousel.dispose()
  }, [autoPlayInterval, images.length])

  if (images.length === 0) return null

  const carousel = (
    <div
          ref={carouselRef}
          id={carouselId}
          className="carousel slide mt-4"
          role="region"
          aria-roledescription="carousel"
          aria-label={title}
        >
          <div className="carousel-inner">
            {images.map((image, index) => (
              <div
                className={`carousel-item${index === 0 ? ' active' : ''}`}
                key={image}
              >
                <img
                  className="d-block mx-auto w-100"
                  src={image}
                  alt={`VISTA app screenshot ${index + 1} of ${images.length}`}
                />
              </div>
            ))}
          </div>

          {images.length > 1 && (
            <>
              <button
                className="carousel-control-prev"
                type="button"
                data-bs-target={`#${carouselId}`}
                data-bs-slide="prev"
              >
                <span
                  className="carousel-control-prev-icon"
                  aria-hidden="true"
                />
                <span className="visually-hidden">Previous</span>
              </button>
              <button
                className="carousel-control-next"
                type="button"
                data-bs-target={`#${carouselId}`}
                data-bs-slide="next"
              >
                <span
                  className="carousel-control-next-icon"
                  aria-hidden="true"
                />
                <span className="visually-hidden">Next</span>
              </button>
            </>
          )}
    </div>
  )

  if (variant === 'embedded') {
    return (
      <div className="rounded-3xl bg-slate-900 p-4 shadow-2xl sm:p-6">
        <h3 className="text-center text-lg font-bold text-white">{title}</h3>
        {carousel}
      </div>
    )
  }

  return (
    <section
      className="bg-secondary py-5 text-white"
      aria-labelledby={`${carouselId}-title`}
    >
      <div className="container">
        <h2 id={`${carouselId}-title`} className="text-center">
          {title}
        </h2>
        {carousel}
      </div>
    </section>
  )
}

export default ScreenshotCarousel
