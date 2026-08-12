import { useEffect, useRef, useState, type ReactNode } from 'react'

type ScrollRevealProps = {
  children: ReactNode
  direction: 'left' | 'right'
  className?: string
  onReveal?: () => void
}

function ScrollReveal({
  children,
  direction,
  className = '',
  onReveal,
}: ScrollRevealProps) {
  const elementRef = useRef<HTMLDivElement>(null)
  const [isVisible, setIsVisible] = useState(() => {
    const prefersReducedMotion = window.matchMedia?.(
      '(prefers-reduced-motion: reduce)',
    ).matches
    return prefersReducedMotion || !('IntersectionObserver' in window)
  })

  useEffect(() => {
    const element = elementRef.current
    if (!element) return

    if (isVisible) return

    const observer = new IntersectionObserver(
      ([entry]) => {
        if (!entry.isIntersecting) return
        setIsVisible(true)
        onReveal?.()
        observer.unobserve(entry.target)
      },
      { threshold: 0.2 },
    )

    observer.observe(element)
    return () => observer.disconnect()
  }, [isVisible, onReveal])

  return (
    <div
      ref={elementRef}
      className={`reveal reveal-${direction}${isVisible ? ' is-visible' : ''} ${className}`}
    >
      {children}
    </div>
  )
}

export default ScrollReveal
