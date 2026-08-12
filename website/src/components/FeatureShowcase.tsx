import { useState } from 'react'
import ScreenshotCarousel from './ScreenshotCarousel'
import ScrollReveal from './ScrollReveal'

type FeatureShowcaseProps = {
  id: string
  eyebrow: string
  title: string
  description: string
  points: string[]
  images: string[]
  imagePosition: 'left' | 'right'
  tone?: 'light' | 'dark'
}

function FeatureShowcase({
  id,
  eyebrow,
  title,
  description,
  points,
  images,
  imagePosition,
  tone = 'light',
}: FeatureShowcaseProps) {
  const [isGalleryVisible, setIsGalleryVisible] = useState(false)
  const textDirection = imagePosition === 'right' ? 'left' : 'right'
  const imageDirection = imagePosition
  const eyebrowClass = tone === 'dark' ? 'text-cyan-200' : 'text-blue-700'
  const titleClass = tone === 'dark' ? 'text-white' : 'text-slate-950'
  const bodyClass = tone === 'dark' ? 'text-slate-100' : 'text-slate-600'
  const listClass = tone === 'dark' ? 'text-slate-100' : 'text-slate-700'
  const dotClass = tone === 'dark' ? 'bg-cyan-300' : 'bg-blue-600'

  const text = (
    <ScrollReveal direction={textDirection} className="self-center">
      <p className={`text-sm font-bold tracking-[0.2em] uppercase ${eyebrowClass}`}>
        {eyebrow}
      </p>
      <h2 id={`${id}-title`} className={`mt-3 text-3xl font-black sm:text-4xl ${titleClass}`}>
        {title}
      </h2>
      <p className={`mt-5 text-lg leading-8 ${bodyClass}`}>{description}</p>
      <ul className={`mt-6 space-y-3 ${listClass}`}>
        {points.map((point) => (
          <li className="flex gap-3" key={point}>
            <span className={`mt-2 size-2 shrink-0 rounded-full ${dotClass}`} aria-hidden="true" />
            <span>{point}</span>
          </li>
        ))}
      </ul>
    </ScrollReveal>
  )

  const gallery = (
    <ScrollReveal
      direction={imageDirection}
      onReveal={() => setIsGalleryVisible(true)}
    >
      <ScreenshotCarousel
        images={images}
        autoPlayInterval={isGalleryVisible ? 3000 : 0}
        carouselId={`${id}Carousel`}
        title={`${title} screens`}
        variant="embedded"
      />
    </ScrollReveal>
  )

  return (
    <section
      id={id}
      className="overflow-hidden px-6 py-20 sm:py-24"
      aria-labelledby={`${id}-title`}
    >
      <div className="mx-auto grid max-w-6xl items-center gap-12 md:grid-cols-2 md:gap-16">
        {imagePosition === 'left' ? gallery : text}
        {imagePosition === 'left' ? text : gallery}
      </div>
    </section>
  )
}

export default FeatureShowcase
