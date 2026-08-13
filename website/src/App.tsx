import vistaLogo from './assets/images/vista_logo.png'
import Contributor from './components/Contributor'
import FeatureShowcase from './components/FeatureShowcase'
import ScreenshotCarousel from './components/ScreenshotCarousel'

const screenshotModules = import.meta.glob<{ default: string }>(
  './assets/images/screen*.png',
  { eager: true },
)

const screenshots = Object.entries(screenshotModules)
  .sort(([firstPath], [secondPath]) => {
    const firstNumber = Number(firstPath.match(/screen(\d+)\.png$/)?.[1])
    const secondNumber = Number(secondPath.match(/screen(\d+)\.png$/)?.[1])
    return firstNumber - secondNumber
  })
  .map(([, module]) => module.default)

const screenshotByNumber = Object.fromEntries(
  Object.entries(screenshotModules).map(([path, module]) => [
    Number(path.match(/screen(\d+)\.png$/)?.[1]),
    module.default,
  ]),
)

const voiceControlScreenshots = [5, 4, 6, 7, 8, 9, 10, 11, 12].map(
  (number) => screenshotByNumber[number],
)
const chatbotScreenshots = [4, 3].map((number) => screenshotByNumber[number])
const busDetectionScreenshots = [6, 12].map(
  (number) => screenshotByNumber[number],
)

const features = [
  {
    title: 'Voice Control',
    description: 'Navigate the app hands-free with intuitive voice commands.',
    accent: 'from-blue-600 to-cyan-500',
    href: '#voice-control',
    image: screenshotByNumber[5],
  },
  {
    title: 'Chatbot Assistance',
    description: 'Get instant help and support through an AI-powered chatbot.',
    accent: 'from-violet-600 to-fuchsia-500',
    href: '#chatbot-assistance',
    image: screenshotByNumber[4],
  },
  {
    title: 'Bus Detection',
    description: 'Detect nearby bus stops and get real-time updates.',
    accent: 'from-emerald-600 to-teal-500',
    href: '#bus-detection',
    image: screenshotByNumber[6],
  },
]

function App() {
  return (
    <div className="min-h-screen">
      <header className="bg-vista-navy text-white">
        <nav
          className="mx-auto flex max-w-6xl items-center justify-between px-6 py-4"
          aria-label="Main navigation"
        >
          <a className="text-xl font-bold text-white no-underline" href="#top">
            VISTA
          </a>
          <a
            className="rounded px-3 py-2 text-white no-underline hover:bg-white/10 focus-visible:outline-2 focus-visible:outline-offset-2"
            href="https://github.com/HarryManHK/Vista"
            target="_blank"
            rel="noreferrer"
          >
            GitHub
          </a>
        </nav>
      </header>

      <main id="top">
        <section className="mx-auto grid max-w-6xl items-center gap-10 px-6 py-16 md:grid-cols-2">
          <div>
            <h1 className="text-4xl font-bold tracking-tight sm:text-5xl">
              VISTA Android App
            </h1>
            <p className="mt-5 max-w-xl text-lg leading-8 text-slate-600">
              Enhance accessibility with voice control, chatbot assistance, and
              real-time bus detection.
            </p>
            <a
              className="bg-vista-blue mt-8 inline-flex rounded-lg px-5 py-3 font-semibold text-white no-underline shadow-sm hover:bg-blue-700 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-blue-600"
              href="/apk/vista.apk"
              download
            >
              Download APK
            </a>
          </div>
          <img
            className="mx-auto w-full max-w-sm rounded-3xl shadow-xl"
            src={vistaLogo}
            alt="VISTA app logo"
          />
        </section>

        <section
          className="bg-white px-6 py-16"
          aria-labelledby="features-title"
        >
          <div className="mx-auto max-w-6xl">
            <h2
              id="features-title"
              className="text-center text-3xl font-black text-slate-950 sm:text-4xl"
            >
              Features
            </h2>
            <div className="mt-10 grid gap-6 md:grid-cols-3">
              {features.map((feature) => (
                <a
                  className="feature-card group relative block overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-lg transition duration-300 hover:-translate-y-2 hover:border-blue-200 hover:shadow-2xl focus-visible:-translate-y-2 focus-visible:outline-3 focus-visible:outline-offset-4 focus-visible:outline-blue-600"
                  href={feature.href}
                  key={feature.title}
                >
                  <div
                    className={`absolute inset-x-0 top-0 z-10 h-1.5 bg-gradient-to-r ${feature.accent}`}
                  />
                  <img
                    className="h-64 w-full bg-slate-900 object-contain p-3 sm:h-72"
                    src={feature.image}
                    alt=""
                  />
                  <div className="p-6 sm:p-7">
                    <h3 className="text-2xl font-black tracking-tight text-slate-950 sm:text-3xl">
                      {feature.title}
                    </h3>
                    <p className="mt-4 hidden leading-7 text-slate-600 sm:block">
                      {feature.description}
                    </p>
                    <span className="mt-6 hidden font-bold text-blue-700 transition group-hover:translate-x-1 sm:inline-flex">
                      Explore feature
                      <span className="ml-2" aria-hidden="true">
                        &rarr;
                      </span>
                    </span>
                  </div>
                </a>
              ))}
            </div>
          </div>
        </section>

        <div id="screenshots">
          <ScreenshotCarousel images={screenshots} />
        </div>

        <FeatureShowcase
          id="voice-control"
          eyebrow="Hands-free navigation"
          title="Control VISTA with your voice"
          description="Move through key app functions with spoken commands, reducing the need for precise touch interaction while travelling."
          points={[
            'Start voice commands from the main interface.',
            'Follow clear prompts through each supported action.',
            'Review the sequence manually or let it advance every three seconds.',
          ]}
          images={voiceControlScreenshots}
          imagePosition="right"
        />

        <div className="bg-secondary">
          <FeatureShowcase
            id="chatbot-assistance"
            eyebrow="Guidance when you need it"
            title="Ask the VISTA chatbot"
            description="Use the conversational assistant to find information and receive support without leaving the app experience."
            points={[
              'Enter questions in a familiar chat interface.',
              'Read clearly separated user and assistant messages.',
              'Keep travel support available in one place.',
            ]}
            images={chatbotScreenshots}
            imagePosition="left"
            tone="dark"
          />
        </div>

        <FeatureShowcase
          id="bus-detection"
          eyebrow="Travel awareness"
          title="Detect buses and nearby stops"
          description="Use VISTA's detection tools to identify buses and support safer, more informed journeys in real-world travel environments."
          points={[
            'Review detected bus and stop information in a clear interface.',
            'Move between results manually or let the gallery advance.',
            'Keep important travel context accessible while on the move.',
          ]}
          images={busDetectionScreenshots}
          imagePosition="right"
        />

        <Contributor />
      </main>

      <footer className="bg-vista-navy px-6 py-6 text-center text-sm text-slate-300">
        <p>&copy; {new Date().getFullYear()} VISTA. All rights reserved.</p>
      </footer>
    </div>
  )
}

export default App
