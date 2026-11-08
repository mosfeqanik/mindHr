import { Fragment, useEffect, useState } from 'react'
import { NavLink, Outlet, useLocation } from 'react-router-dom'
import {
  Briefcase,
  Box,
  Calendar,
  ChevronDown,
  Clock,
  CreditCard,
  DollarSign,
  FileText,
  Globe,
  Grid,
  Home,
  Layers,
  LifeBuoy,
  MapPin,
  Power,
  Repeat,
  Settings,
  User,
  Users,
} from 'react-feather'
import type { ComponentType } from 'react'
import { useQuery } from '@tanstack/react-query'
import { subscriptionsApi } from '../api/platform'
import type { PlanFeature } from '../api/types'
import { useAuth } from '../auth/AuthContext'
import { PageHeader } from '../components/ui'

type Section = 'platform' | 'account' | 'modules'

interface NavItem {
  to: string
  label: string
  icon: ComponentType<{ className?: string }>
  section?: Section
  permission?: string
  /** Platform users must pick an organization in the top bar before opening it. */
  orgScoped?: boolean
  superAdminOnly?: boolean
  /** Shown only when the organization's subscription plan includes this feature. */
  feature?: PlanFeature
}

// 'platform' and 'account' entries are for SaaS-side users (SUPER_ADMIN / SAAS_STAFF);
// 'modules' entries are filtered by the user's authorities (backend permission = MODULE_VIEW).
const NAV: NavItem[] = [
  { to: '/', label: 'Dashboard', icon: Home },
  { to: '/organizations', label: 'Organizations', icon: Globe, section: 'platform' },
  { to: '/users', label: 'Users', icon: User, section: 'platform' },
  { to: '/plans', label: 'Subscription Plans', icon: Layers, section: 'platform' },
  { to: '/system-settings', label: 'System Settings', icon: Settings, section: 'platform', superAdminOnly: true },
  { to: '/subscriptions', label: 'Subscriptions', icon: Repeat, section: 'account', orgScoped: true },
  { to: '/billing', label: 'Billing', icon: CreditCard, section: 'account', orgScoped: true },
  { to: '/contracts', label: 'Contracts', icon: FileText, section: 'account', orgScoped: true },
  { to: '/support-tickets', label: 'Support Tickets', icon: LifeBuoy, section: 'account', orgScoped: true },
  { to: '/employees', label: 'Employees', icon: Users, section: 'modules', permission: 'EMPLOYEE_VIEW', orgScoped: true },
  { to: '/branches', label: 'Branches', icon: MapPin, section: 'modules', permission: 'BRANCH_VIEW', orgScoped: true },
  { to: '/departments', label: 'Departments', icon: Grid, section: 'modules', permission: 'DEPARTMENT_VIEW', orgScoped: true },
  { to: '/attendance', label: 'Attendance', icon: Clock, section: 'modules', permission: 'ATTENDANCE_VIEW', orgScoped: true, feature: 'ATTENDANCE' },
  { to: '/shifts', label: 'Shifts', icon: Calendar, section: 'modules', permission: 'SHIFT_VIEW', orgScoped: true, feature: 'SHIFTS' },
  { to: '/leave', label: 'Leave', icon: Briefcase, section: 'modules', permission: 'LEAVE_VIEW', orgScoped: true, feature: 'LEAVE' },
  { to: '/payroll', label: 'Payroll', icon: DollarSign, section: 'modules', permission: 'PAYROLL_VIEW', orgScoped: true, feature: 'PAYROLL' },
  { to: '/assets', label: 'Assets', icon: Box, section: 'modules', permission: 'ASSET_VIEW', orgScoped: true, feature: 'ASSETS' },
]

const SECTION_TITLES: Record<Section, string> = { platform: 'Platform', account: 'Client account', modules: 'HR modules' }

function OrgRequired() {
  return (
    <>
      <PageHeader title="Select an organization" />
      <div className="container-fluid">
        <div className="card">
          <div className="card-body text-muted">
            This page shows one organization's data. Pick the organization in the top bar first.
          </div>
        </div>
      </div>
    </>
  )
}

/** Matches Bootstrap's "below lg" (< 992 px), where the template shows the ☰ toggler. */
function useIsSmallScreen() {
  const query = '(max-width: 991.98px)'
  const [small, setSmall] = useState(() => window.matchMedia(query).matches)
  useEffect(() => {
    const mq = window.matchMedia(query)
    const onChange = () => setSmall(mq.matches)
    mq.addEventListener('change', onChange)
    return () => mq.removeEventListener('change', onChange)
  }, [])
  return small
}

export function AppShell() {
  const { session, signOut, can, isPlatform, orgId } = useAuth()
  const location = useLocation()
  const [sidebarOpen, setSidebarOpen] = useState(false) // mobile only
  const small = useIsSmallScreen()
  const [navOpen, setNavOpen] = useState(false) // mobile topbar collapse
  const [userMenu, setUserMenu] = useState(false)
  const isSuperAdmin = session?.userType === 'SUPER_ADMIN'

  // Close the slide-in menus after navigating, and when the screen grows past the phone/tablet size.
  useEffect(() => {
    setSidebarOpen(false)
    setNavOpen(false)
  }, [location.pathname, small])
  const isActive = (to: string) => (to === '/' ? location.pathname === '/' : location.pathname.startsWith(to))

  // The organization's plan decides which feature modules show. A platform user without an
  // organization has no plan to check, so they see every module.
  const hasOrg = !isPlatform || orgId != null
  const plan = useQuery({
    queryKey: ['current-plan', isPlatform ? orgId : 'own'],
    queryFn: () => subscriptionsApi.current(),
    enabled: hasOrg,
  })
  const planAllows = (f: PlanFeature) => !hasOrg || (plan.data?.features.includes(f) ?? false)

  const visible = (n: NavItem) => {
    if (n.section === 'platform' || n.section === 'account') return isPlatform && (!n.superAdminOnly || isSuperAdmin)
    if (n.feature && !planAllows(n.feature)) return false
    return !n.permission || can(n.permission)
  }
  const items = NAV.filter(visible)
  const sections = (['platform', 'account', 'modules'] as const)
    .map((s) => ({ key: s, items: items.filter((n) => n.section === s) }))
    .filter((s) => s.items.length > 0)

  const current = NAV.find((n) => n.to !== '/' && isActive(n.to))
  const needsOrg = isPlatform && current?.orgScoped && orgId == null

  const renderItem = (n: NavItem) => (
    <li key={n.to} className={`sidebar-item ${isActive(n.to) ? 'selected' : ''}`}>
      <NavLink
        to={n.to}
        end={n.to === '/'}
        className={({ isActive: a }) => `sidebar-link ${a ? 'active' : ''}`}
        onClick={() => setSidebarOpen(false)}
      >
        <n.icon className="feather-icon" />
        <span className="hide-menu">{n.label}</span>
      </NavLink>
    </li>
  )

  const section = (title: string) => (
    <>
      <li className="list-divider" />
      <li className="nav-small-cap">
        <span className="hide-menu">{title}</span>
      </li>
    </>
  )

  return (
    <div
      id="main-wrapper"
      data-theme="light"
      data-layout="vertical"
      data-navbarbg="skin6"
      // The template hides the sidebar only in "mini-sidebar" mode (its own jQuery script switched it;
      // we do not load that script), so pick the mode here. Below lg the ☰ button slides it in.
      data-sidebartype={small ? 'mini-sidebar' : 'full'}
      data-sidebar-position="fixed"
      data-header-position="fixed"
      data-boxed-layout="full"
      className={sidebarOpen ? 'show-sidebar' : ''}
    >
      <header className="topbar" data-navbarbg="skin6">
        <nav className="navbar top-navbar navbar-expand-lg">
          <div className="navbar-header" data-logobg="skin6">
            <a className="nav-toggler waves-effect waves-light d-block d-lg-none" onClick={() => setSidebarOpen((v) => !v)}>
              <i className={`ti-menu ${sidebarOpen ? 'ti-close' : ''}`} />
            </a>
            <div className="navbar-brand">
              <NavLink to="/">
                <img src="/brand/mindhr-lockup.png" alt="mindHR" className="mh-brand-logo" />
              </NavLink>
            </div>
            <a className="topbartoggler d-block d-lg-none waves-effect waves-light" onClick={() => setNavOpen((v) => !v)}>
              <i className="ti-more" />
            </a>
          </div>
          <div className={`navbar-collapse collapse ${navOpen ? 'show' : ''}`} id="navbarSupportedContent">
            <ul className="navbar-nav float-start me-auto ms-3 ps-1" />
            <ul className="navbar-nav float-end">
              <li className={`nav-item dropdown ${userMenu ? 'show' : ''}`}>
                <a className="nav-link dropdown-toggle" role="button" onClick={() => setUserMenu((v) => !v)}>
                  <img src="/template/images/users/profile-pic.jpg" alt="user" className="rounded-circle" width="40" />
                  <span className="ms-2 d-none d-lg-inline-block">
                    <span>Hello,</span> <span className="text-dark">{session?.fullName}</span>{' '}
                    <ChevronDown className="svg-icon" />
                  </span>
                </a>
                <div className={`dropdown-menu dropdown-menu-end user-dd ${userMenu ? 'show' : ''}`}>
                  <a
                    className="dropdown-item"
                    role="button"
                    onClick={() => {
                      setUserMenu(false)
                      signOut()
                    }}
                  >
                    <Power className="svg-icon me-2 ms-1" /> Logout
                  </a>
                </div>
              </li>
            </ul>
          </div>
        </nav>
      </header>

      {small && sidebarOpen && <div className="mh-sidebar-backdrop" onClick={() => setSidebarOpen(false)} />}

      <aside className="left-sidebar" data-sidebarbg="skin6">
        <div className="scroll-sidebar" data-sidebarbg="skin6">
          <nav className="sidebar-nav">
            <ul id="sidebarnav">
              {renderItem(NAV[0])}
              {sections.map((s) => (
                <Fragment key={s.key}>
                  {section(SECTION_TITLES[s.key])}
                  {s.items.map(renderItem)}
                </Fragment>
              ))}
            </ul>
          </nav>
        </div>
      </aside>

      <div
        className="page-wrapper"
        key={`${location.pathname}:${orgId ?? ''}`}
        onClick={() => userMenu && setUserMenu(false)}
      >
        {needsOrg ? <OrgRequired /> : <Outlet />}
        <footer className="footer text-center text-muted">mindHR &middot; Copyright &copy; 2026 Vitality Solution</footer>
      </div>
    </div>
  )
}
