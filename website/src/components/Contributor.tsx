type ContributorProfile = {
  label: string
  username: string
  profileUrl: string
  profileIcon: string
}

// To use a local icon:
// 1. Add the PNG to src/assets/images/.
// 2. Import it here, for example: import contributor1Icon from '../assets/images/contributor1Icon.png'
// 3. Set that contributor's profileIcon to contributor1Icon. Leave it blank to use GitHub.

//The demo contributor1Icon json belike:

//import contributor1Icon from '../assets/images/contributor1Icon.png'
// {
//   label: 'Contributor 1',
//   username: 'Contributor 1',
//   profileUrl: 'https://github.com/Contributor1',
//   profileIcon: contributor1Icon, //don't need the '' 
// },

const contributors = [
  {
    label: 'Harry Man',
    username: 'HarryManHK',
    profileUrl: 'https://github.com/HarryManHK',
    profileIcon: '',
  },
  {
    label: 'Steve Wong',
    username: 'qaqlllll',
    profileUrl: 'https://github.com/qaqlllll',
    profileIcon: '',
  },
  {
    label: 'Gavin Wong',
    username: 'SE1AGavin',
    profileUrl: 'https://github.com/SE1AGavin',
    profileIcon: '',
  },
  {
    label: 'Cosmo Wong',
    username: 'cosmoumadd',
    profileUrl: 'https://github.com/cosmoumadd',
    profileIcon: '',
  },
] satisfies ContributorProfile[]

function Contributor() {
  return (
    <section
      className="bg-gradient-to-br from-sky-100 via-blue-100 to-slate-200 px-6 py-16 sm:py-20"
      aria-labelledby="contributors-title"
    >
      <div className="mx-auto max-w-6xl">
        <div className="text-center">
          <p className="text-sm font-bold tracking-[0.2em] text-blue-700 uppercase">
            The team behind VISTA
          </p>
          <h2
            id="contributors-title"
            className="mt-3 text-3xl font-black text-slate-950 sm:text-4xl"
          >
            Contributors
          </h2>
        </div>

        <div className="mt-10 grid grid-cols-2 gap-6 sm:grid-cols-4 sm:gap-8">
          {contributors.map((contributor) => (
            <a
              className="contributor-card group rounded-2xl p-4 text-center transition duration-300 hover:-translate-y-2 hover:bg-white hover:shadow-xl focus-visible:outline-3 focus-visible:outline-offset-4 focus-visible:outline-blue-600"
              href={contributor.profileUrl}
              key={contributor.username}
              target="_blank"
              rel="noreferrer"
              aria-label={`${contributor.label} - ${contributor.username} on GitHub`}
            >
              <img
                className="mx-auto aspect-square w-full max-w-40 rounded-full border-4 border-white object-cover shadow-lg transition duration-300 group-hover:border-blue-200"
                src={
                  contributor.profileIcon ||
                  `${contributor.profileUrl}.png?size=200`
                }
                onError={(event) => {
                  const githubIcon = `${contributor.profileUrl}.png?size=200`
                  if (event.currentTarget.src !== githubIcon) {
                    event.currentTarget.src = githubIcon
                  }
                }}
                alt={`${contributor.username} GitHub avatar`}
                loading="lazy"
                width="200"
                height="200"
              />
              <h3 className="mt-5 text-lg font-black text-slate-950 sm:text-xl">
                {contributor.label}
              </h3>
            </a>
          ))}
        </div>
      </div>
    </section>
  )
}

export default Contributor
