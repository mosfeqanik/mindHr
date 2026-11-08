import { Route, Routes } from 'react-router-dom'
import { RequireAuth } from './auth/RequireAuth'
import { AppShell } from './layouts/AppShell'
import LoginPage from './pages/LoginPage'
import DashboardPage from './pages/DashboardPage'
import EmployeesPage from './pages/EmployeesPage'
import BranchesPage from './pages/BranchesPage'
import DepartmentsPage from './pages/DepartmentsPage'
import AttendancePage from './pages/AttendancePage'
import LeavePage from './pages/leave/LeavePage'
import ShiftsPage from './pages/shifts/ShiftsPage'
import PayrollPage from './pages/payroll/PayrollPage'
import PayrollRunPage from './pages/payroll/PayrollRunPage'
import AssetsPage from './pages/assets/AssetsPage'
import OrganizationsPage from './pages/platform/OrganizationsPage'
import UsersPage from './pages/platform/UsersPage'
import SubscriptionPlansPage from './pages/platform/SubscriptionPlansPage'
import SubscriptionsPage from './pages/platform/SubscriptionsPage'
import BillingPage from './pages/platform/BillingPage'
import ContractsPage from './pages/platform/ContractsPage'
import SupportTicketsPage from './pages/platform/SupportTicketsPage'
import SystemSettingsPage from './pages/platform/SystemSettingsPage'
import NotFoundPage from './pages/NotFoundPage'

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route element={<RequireAuth />}>
        <Route element={<AppShell />}>
          <Route index element={<DashboardPage />} />
          <Route path="organizations" element={<OrganizationsPage />} />
          <Route path="users" element={<UsersPage />} />
          <Route path="plans" element={<SubscriptionPlansPage />} />
          <Route path="system-settings" element={<SystemSettingsPage />} />
          <Route path="subscriptions" element={<SubscriptionsPage />} />
          <Route path="billing" element={<BillingPage />} />
          <Route path="contracts" element={<ContractsPage />} />
          <Route path="support-tickets" element={<SupportTicketsPage />} />
          <Route path="employees" element={<EmployeesPage />} />
          <Route path="branches" element={<BranchesPage />} />
          <Route path="departments" element={<DepartmentsPage />} />
          <Route path="attendance" element={<AttendancePage />} />
          <Route path="shifts" element={<ShiftsPage />} />
          <Route path="leave" element={<LeavePage />} />
          <Route path="payroll" element={<PayrollPage />} />
          <Route path="payroll/:id" element={<PayrollRunPage />} />
          <Route path="assets" element={<AssetsPage />} />
          <Route path="*" element={<NotFoundPage />} />
        </Route>
      </Route>
    </Routes>
  )
}
