import type { InternalAxiosRequestConfig } from 'axios'

/**
 * Platform users (SUPER_ADMIN / SAAS_STAFF) are not bound to an organization, so the backend
 * needs an explicit organizationId on org-scoped calls. The organization picked in the top bar
 * is kept here and added to every request except the global endpoints below.
 */
const ORG_KEY = 'mindhr.orgId'

// Controllers that are not organization-scoped, or treat organizationId as an optional filter.
const GLOBAL_PATHS = [
  '/api/auth',
  '/api/organizations',
  '/api/users',
  '/api/subscription-plans',
  '/api/system-settings',
  '/api/permissions',
]

export function getActiveOrgId(): number | null {
  const v = localStorage.getItem(ORG_KEY)
  return v ? Number(v) : null
}

export function setActiveOrgId(id: number | null) {
  if (id == null) localStorage.removeItem(ORG_KEY)
  else localStorage.setItem(ORG_KEY, String(id))
}

/** Adds organizationId to GET params and to POST/PUT JSON bodies that do not set one. */
export function withActiveOrg(config: InternalAxiosRequestConfig) {
  const orgId = getActiveOrgId()
  const url = config.url ?? ''
  if (orgId == null || GLOBAL_PATHS.some((p) => url.startsWith(p))) return config

  const method = (config.method ?? 'get').toLowerCase()
  if (method === 'get') {
    config.params = { ...config.params, organizationId: config.params?.organizationId ?? orgId }
  } else if ((method === 'post' || method === 'put') && isPlainObject(config.data) && config.data.organizationId == null) {
    config.data = { ...config.data, organizationId: orgId }
  }
  return config
}

function isPlainObject(v: unknown): v is Record<string, unknown> {
  return typeof v === 'object' && v !== null && !Array.isArray(v) && !(v instanceof FormData)
}
