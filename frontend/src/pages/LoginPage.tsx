import { lazy, Suspense, useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import { Eye, EyeOff, Lock, LogIn, Mail, Moon, Sun } from 'react-feather'
import { authApi } from '../api/auth'
import { errorMessage } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import type { LoginTheme } from '../components/ParticleBackground'
import './LoginPage.css'

// Three.js is large; load it only for the login screen, after the form has rendered.
const ParticleBackground = lazy(() =>
  import('../components/ParticleBackground').then((m) => ({ default: m.ParticleBackground })),
)

const schema = z.object({
  email: z.string().min(1, 'Email is required').email('Email must be valid'),
  password: z.string().min(1, 'Password is required'),
})
type Form = z.infer<typeof schema>

const THEME_KEY = 'mindhr.loginTheme'

function initialTheme(): LoginTheme {
  try {
    const saved = localStorage.getItem(THEME_KEY)
    if (saved === 'light' || saved === 'dark') return saved
  } catch {
    // storage blocked: fall back to the system setting
  }
  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
}

export default function LoginPage() {
  const { session, signIn } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [error, setError] = useState<string | null>(null)
  const [theme, setTheme] = useState<LoginTheme>(initialTheme)
  const [showPassword, setShowPassword] = useState(false)
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<Form>({ resolver: zodResolver(schema) })

  if (session) return <Navigate to="/" replace />

  const from = (location.state as { from?: { pathname: string } } | null)?.from?.pathname ?? '/'

  const onSubmit = async (values: Form) => {
    setError(null)
    try {
      const res = await authApi.login(values)
      signIn(res.accessToken, res.fullName)
      navigate(from, { replace: true })
    } catch (e) {
      setError(errorMessage(e))
    }
  }

  const toggleTheme = () => {
    const next = theme === 'dark' ? 'light' : 'dark'
    setTheme(next)
    try {
      localStorage.setItem(THEME_KEY, next)
    } catch {
      // not saved; the toggle still works for this visit
    }
  }

  return (
    <div className="login-page" data-theme={theme}>
      <Suspense fallback={null}>
        <ParticleBackground theme={theme} />
      </Suspense>

      <main className="login-card">
        <div className="login-content">
          <div className="login-brand">
            <img
              src={theme === 'dark' ? '/brand/mindhr-lockup-dark.png' : '/brand/mindhr-lockup.png'}
              alt="mindHR"
              className="login-brand-logo"
            />
          </div>

          <header className="login-header">
            <h1>Welcome back</h1>
            <button
              type="button"
              className="login-theme-toggle"
              onClick={toggleTheme}
              aria-label={theme === 'dark' ? 'Switch to light mode' : 'Switch to dark mode'}
            >
              {theme === 'dark' ? <Sun size={18} /> : <Moon size={18} />}
            </button>
          </header>
          <p className="login-welcome">Sign in to continue to your workspace.</p>

          <form onSubmit={handleSubmit(onSubmit)} noValidate>
            {error && (
              <div className="login-alert" role="alert">
                {error}
              </div>
            )}

            <div className="login-field">
              <label htmlFor="email">Email address</label>
              <div className="login-input-wrapper">
                <Mail className="login-field-icon" aria-hidden="true" />
                <input
                  id="email"
                  type="email"
                  autoComplete="username"
                  placeholder="hello@example.com"
                  aria-invalid={errors.email ? 'true' : 'false'}
                  {...register('email')}
                />
              </div>
              {errors.email && <div className="login-field-error">{errors.email.message}</div>}
            </div>

            <div className="login-field">
              <label htmlFor="password">Password</label>
              <div className="login-input-wrapper">
                <Lock className="login-field-icon" aria-hidden="true" />
                <input
                  id="password"
                  type={showPassword ? 'text' : 'password'}
                  autoComplete="current-password"
                  placeholder="••••••••"
                  aria-invalid={errors.password ? 'true' : 'false'}
                  {...register('password')}
                />
                <button
                  type="button"
                  className="login-toggle-password"
                  onClick={() => setShowPassword((v) => !v)}
                  aria-label={showPassword ? 'Hide password' : 'Show password'}
                  aria-pressed={showPassword}
                >
                  {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              </div>
              {errors.password && <div className="login-field-error">{errors.password.message}</div>}
            </div>

            <button type="submit" className="login-btn" disabled={isSubmitting}>
              <LogIn size={17} aria-hidden="true" />
              {isSubmitting ? 'Signing in...' : 'Log in'}
            </button>
          </form>

          <p className="login-footnote">Accounts are created by your administrator.</p>
        </div>
      </main>
    </div>
  )
}
