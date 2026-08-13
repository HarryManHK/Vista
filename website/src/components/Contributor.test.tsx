import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it } from 'vitest'
import Contributor from './Contributor'

afterEach(cleanup)

describe('Contributor', () => {
  it('falls back to the matching GitHub avatar when an icon fails', () => {
    render(<Contributor />)

    const avatar = screen.getByAltText('contributor1 GitHub avatar')
    avatar.setAttribute('src', '/assets/missing-profile.png')
    fireEvent.error(avatar)

    expect(avatar.getAttribute('src')).toBe(
      'https://github.com/contributor1.png?size=200',
    )
  })
})
