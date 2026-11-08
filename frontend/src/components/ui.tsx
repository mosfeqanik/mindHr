import { forwardRef, useEffect, useMemo, useState } from 'react'
import { Eye, EyeOff } from 'react-feather'
import type {
  ButtonHTMLAttributes,
  InputHTMLAttributes,
  ReactNode,
  SelectHTMLAttributes,
  TextareaHTMLAttributes,
} from 'react'

// Thin wrappers over FreeDash / Bootstrap 5 classes.

export function Button({
  variant = 'primary',
  size,
  className = '',
  ...props
}: ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: 'primary' | 'secondary' | 'danger' | 'dark'
  size?: 'sm'
}) {
  const v = { primary: 'btn-primary', secondary: 'btn-outline-secondary', danger: 'btn-danger', dark: 'btn-dark' }[variant]
  return <button {...props} className={`btn ${v} ${size === 'sm' ? 'btn-sm' : ''} ${className}`} />
}

/** Square icon-only button for table rows; `label` is the tooltip and the accessible name. */
export function IconButton({
  icon,
  label,
  variant = 'secondary',
  className = '',
  ...props
}: ButtonHTMLAttributes<HTMLButtonElement> & { icon: ReactNode; label: string; variant?: 'secondary' | 'danger' }) {
  const v = variant === 'danger' ? 'btn-outline-danger' : 'btn-outline-secondary'
  return (
    <button type="button" title={label} aria-label={label} {...props} className={`btn btn-sm mh-icon-btn ${v} ${className}`}>
      {icon}
    </button>
  )
}

export function Field({ label, error, children }: { label: string; error?: string; children: ReactNode }) {
  return (
    <div className="form-group mb-3">
      <label className="form-label text-dark">{label}</label>
      {children}
      {error && <div className="text-danger small mt-1">{error}</div>}
    </div>
  )
}

export const Input = forwardRef<HTMLInputElement, InputHTMLAttributes<HTMLInputElement>>((props, ref) => (
  <input ref={ref} {...props} className={`form-control ${props.className ?? ''}`} />
))

/** Password field with an eye button that shows or hides the text. */
export const PasswordInput = forwardRef<HTMLInputElement, Omit<InputHTMLAttributes<HTMLInputElement>, 'type'>>(
  (props, ref) => {
    const [visible, setVisible] = useState(false)
    return (
      <div className="input-group">
        <input
          ref={ref}
          {...props}
          type={visible ? 'text' : 'password'}
          className={`form-control ${props.className ?? ''}`}
        />
        <button
          type="button"
          className="btn btn-outline-secondary"
          aria-label={visible ? 'Hide password' : 'Show password'}
          title={visible ? 'Hide password' : 'Show password'}
          onClick={() => setVisible((v) => !v)}
        >
          {visible ? <EyeOff size={16} /> : <Eye size={16} />}
        </button>
      </div>
    )
  },
)

export const Textarea = forwardRef<HTMLTextAreaElement, TextareaHTMLAttributes<HTMLTextAreaElement>>((props, ref) => (
  <textarea ref={ref} rows={3} {...props} className={`form-control ${props.className ?? ''}`} />
))

export const Select = forwardRef<HTMLSelectElement, SelectHTMLAttributes<HTMLSelectElement>>((props, ref) => (
  <select ref={ref} {...props} className={`form-select ${props.className ?? ''}`} />
))

export function ErrorBox({ message }: { message: string }) {
  return (
    <div className="alert alert-danger py-2" role="alert">
      {message}
    </div>
  )
}

export function Modal({ title, onClose, children }: { title: string; onClose: () => void; children: ReactNode }) {
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => e.key === 'Escape' && onClose()
    document.addEventListener('keydown', onKey)
    return () => document.removeEventListener('keydown', onKey)
  }, [onClose])

  return (
    <>
      <div className="modal-backdrop fade show" />
      <div className="modal fade show d-block" tabIndex={-1} role="dialog" aria-modal="true" onClick={onClose}>
        <div className="modal-dialog modal-dialog-scrollable modal-lg modal-fullscreen-sm-down" onClick={(e) => e.stopPropagation()}>
          <div className="modal-content">
            <div className="modal-header">
              <h5 className="modal-title">{title}</h5>
              <button type="button" className="btn-close" aria-label="Close" onClick={onClose} />
            </div>
            {children}
          </div>
        </div>
      </div>
    </>
  )
}

type SortValue = string | number | boolean | null | undefined

export interface Column<T> {
  header: string
  cell: (row: T) => ReactNode
  className?: string
  /** Plain value for search and sorting in `datatable` mode; defaults to the cell when it renders a string or number. */
  value?: (row: T) => SortValue
}

function plainValue<T>(c: Column<T>, row: T): SortValue {
  if (c.value) return c.value(row)
  const v = c.cell(row)
  return typeof v === 'string' || typeof v === 'number' ? v : null
}

function compareValues(a: SortValue, b: SortValue) {
  if (a == null || a === '') return b == null || b === '' ? 0 : 1 // blanks last
  if (b == null || b === '') return -1
  if (typeof a === 'number' && typeof b === 'number') return a === b ? 0 : a < b ? -1 : 1
  return String(a).localeCompare(String(b), undefined, { numeric: true, sensitivity: 'base' })
}

/** Page numbers with gaps, e.g. 1 … 4 5 6 … 12. */
function pageList(page: number, pages: number): (number | null)[] {
  if (pages <= 7) return Array.from({ length: pages }, (_, i) => i + 1)
  const out: (number | null)[] = [1]
  const from = Math.max(2, Math.min(page - 1, pages - 4))
  const to = Math.min(pages - 1, Math.max(page + 1, 5))
  if (from > 2) out.push(null)
  for (let p = from; p <= to; p++) out.push(p)
  if (to < pages - 1) out.push(null)
  out.push(pages)
  return out
}

const PAGE_SIZES = [10, 25, 50, 100]

/**
 * Bootstrap table with loading and empty rows. With `datatable`, it works like the template's
 * DataTables page (table-datatable-basic.html): entries per page, search, sortable headers and
 * pagination, all client-side.
 */
export function DataTable<T extends { id: number }>({
  columns,
  rows,
  loading,
  empty = 'Nothing to show.',
  datatable,
}: {
  columns: Column<T>[]
  rows: T[]
  loading?: boolean
  empty?: string
  datatable?: boolean
}) {
  const [search, setSearch] = useState('')
  const [sort, setSort] = useState<{ col: number; dir: 1 | -1 } | null>(null)
  const [pageSize, setPageSize] = useState(PAGE_SIZES[0])
  const [page, setPage] = useState(1)

  const filtered = useMemo(() => {
    if (!datatable) return rows
    const q = search.trim().toLowerCase()
    let out = q
      ? rows.filter((r) => columns.some((c) => String(plainValue(c, r) ?? '').toLowerCase().includes(q)))
      : rows
    if (sort) {
      const c = columns[sort.col]
      out = [...out].sort((a, b) => sort.dir * compareValues(plainValue(c, a), plainValue(c, b)))
    }
    return out
  }, [datatable, rows, columns, search, sort])

  const pages = Math.max(1, Math.ceil(filtered.length / pageSize))
  const current = Math.min(page, pages) // rows can shrink under us, e.g. after a delete
  const start = (current - 1) * pageSize
  const visible = datatable ? filtered.slice(start, start + pageSize) : rows

  const toggleSort = (col: number) => {
    setSort((s) => (s?.col !== col ? { col, dir: 1 } : s.dir === 1 ? { col, dir: -1 } : null))
    setPage(1)
  }

  const table = (
    <div className="table-responsive">
      <table className={`table align-middle mh-stack ${datatable ? 'border table-striped table-hover mh-datatable' : 'table-hover'}`}>
        <thead>
          <tr>
            {columns.map((c, i) => {
              const sortable = datatable && c.header !== ''
              const dir = sort?.col === i ? sort.dir : 0
              return (
                <th
                  key={i}
                  scope="col"
                  className={`${c.className ?? ''} ${sortable ? `sorting ${dir === 1 ? 'sorting-asc' : dir === -1 ? 'sorting-desc' : ''}` : ''}`}
                  aria-sort={dir === 1 ? 'ascending' : dir === -1 ? 'descending' : undefined}
                  onClick={sortable ? () => toggleSort(i) : undefined}
                >
                  {c.header}
                </th>
              )
            })}
          </tr>
        </thead>
        <tbody>
          {loading && (
            <tr>
              <td colSpan={columns.length} className="text-center text-muted">
                Loading...
              </td>
            </tr>
          )}
          {!loading && visible.length === 0 && (
            <tr>
              <td colSpan={columns.length} className="text-center text-muted">
                {rows.length > 0 ? 'No matching records found.' : empty}
              </td>
            </tr>
          )}
          {visible.map((r) => (
            <tr key={r.id}>
              {columns.map((c, i) => (
                <td key={i} className={c.className} data-label={c.header}>
                  {c.cell(r)}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )

  if (!datatable) return table

  return (
    <div className="mh-datatable-wrapper">
      <div className="row g-2 align-items-center mb-3">
        <div className="col-sm-6">
          <label className="d-inline-flex align-items-center gap-2 mb-0">
            Show
            <select
              className="form-select form-select-sm w-auto"
              value={pageSize}
              onChange={(e) => {
                setPageSize(Number(e.target.value))
                setPage(1)
              }}
            >
              {PAGE_SIZES.map((n) => (
                <option key={n} value={n}>
                  {n}
                </option>
              ))}
            </select>
            entries
          </label>
        </div>
        <div className="col-sm-6 text-sm-end">
          <label className="d-inline-flex align-items-center gap-2 mb-0">
            Search:
            <input
              type="search"
              className="form-control form-control-sm"
              value={search}
              onChange={(e) => {
                setSearch(e.target.value)
                setPage(1)
              }}
            />
          </label>
        </div>
      </div>
      {table}
      <div className="row g-2 align-items-center">
        <div className="col-sm-5 text-muted small" role="status" aria-live="polite">
          Showing {filtered.length === 0 ? 0 : start + 1} to {Math.min(start + pageSize, filtered.length)} of{' '}
          {filtered.length} entries
          {filtered.length !== rows.length && ` (filtered from ${rows.length} total entries)`}
        </div>
        <div className="col-sm-7">
          <ul className="pagination pagination-sm justify-content-sm-end mb-0">
            <li className={`page-item ${current === 1 ? 'disabled' : ''}`}>
              <button type="button" className="page-link" onClick={() => setPage(current - 1)}>
                Previous
              </button>
            </li>
            {pageList(current, pages).map((p, i) =>
              p === null ? (
                <li key={`gap${i}`} className="page-item disabled">
                  <span className="page-link">…</span>
                </li>
              ) : (
                <li key={p} className={`page-item ${p === current ? 'active' : ''}`}>
                  <button type="button" className="page-link" onClick={() => setPage(p)}>
                    {p}
                  </button>
                </li>
              ),
            )}
            <li className={`page-item ${current === pages ? 'disabled' : ''}`}>
              <button type="button" className="page-link" onClick={() => setPage(current + 1)}>
                Next
              </button>
            </li>
          </ul>
        </div>
      </div>
    </div>
  )
}

export function Tabs<K extends string>({
  tabs,
  active,
  onChange,
}: {
  tabs: { key: K; label: string }[]
  active: K
  onChange: (key: K) => void
}) {
  return (
    <ul className="nav nav-tabs mb-3 mh-tabs">
      {tabs.map((t) => (
        <li key={t.key} className="nav-item">
          <a
            role="button"
            className={`nav-link ${t.key === active ? 'active' : ''}`}
            onClick={() => onChange(t.key)}
          >
            {t.label}
          </a>
        </li>
      ))}
    </ul>
  )
}

export function ActiveBadge({ active }: { active: boolean }) {
  return <span className={`badge ${active ? 'bg-success' : 'bg-secondary'}`}>{active ? 'Active' : 'Inactive'}</span>
}

export function ModalFooter({ saving, onClose }: { saving: boolean; onClose: () => void }) {
  return (
    <div className="modal-footer">
      <Button type="button" variant="secondary" onClick={onClose}>
        Cancel
      </Button>
      <Button type="submit" disabled={saving}>
        {saving ? 'Saving...' : 'Save'}
      </Button>
    </div>
  )
}

/** Page title + breadcrumb row, as in the template's .page-breadcrumb block. */
export function PageHeader({ title, trail = [], actions }: { title: string; trail?: string[]; actions?: ReactNode }) {
  return (
    <div className="page-breadcrumb">
      <div className="row">
        <div className="col-12 col-md-7 align-self-center">
          <h4 className="page-title text-truncate text-dark font-weight-medium mb-1">{title}</h4>
          <nav aria-label="breadcrumb">
            <ol className="breadcrumb m-0 p-0">
              <li className="breadcrumb-item text-muted">Home</li>
              {trail.map((t) => (
                <li key={t} className="breadcrumb-item text-muted active" aria-current="page">
                  {t}
                </li>
              ))}
            </ol>
          </nav>
        </div>
        <div className="col-12 col-md-5 align-self-center">
          <div className="mh-page-actions">{actions}</div>
        </div>
      </div>
    </div>
  )
}
