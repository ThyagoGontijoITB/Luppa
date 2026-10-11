import { defineConfig, loadEnv } from 'vite'
import react, { reactCompilerPreset } from '@vitejs/plugin-react'
import babel from '@rolldown/plugin-babel'

const normalizeBase = (value) => {
  if (!value || value === '/') return '/'
  const withLeadingSlash = value.startsWith('/') ? value : `/${value}`
  return withLeadingSlash.endsWith('/') ? withLeadingSlash : `${withLeadingSlash}/`
}

export default defineConfig(({ command, mode }) => {
  const env = { ...loadEnv(mode, process.cwd(), ''), ...process.env }
  const appBasePath = env.VITE_APP_BASE_PATH
  const deployTarget = env.VITE_DEPLOY_TARGET
  const base = command === 'build'
    ? normalizeBase(appBasePath || (deployTarget === 'github-pages' ? '/Luppa/' : '/'))
    : '/'

  const apiBaseUrl = env.VITE_API_BASE_URL || ''
  if (mode === 'production' && /localhost|127\.0\.0\.1/i.test(apiBaseUrl)) {
    throw new Error('Unsafe production frontend configuration: VITE_API_BASE_URL must not point to localhost.')
  }

  return {
    base,
    optimizeDeps: {
      exclude: ['maplibre-gl'],
    },
    server: {
      watch: {
        ignored: ['**/.chrome-*/**'],
      },
    },
    plugins: [
      react(),
      babel({ presets: [reactCompilerPreset()] })
    ],
  }
})
