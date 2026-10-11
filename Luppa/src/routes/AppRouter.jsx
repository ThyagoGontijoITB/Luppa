import { BrowserRouter, Route, Routes } from 'react-router-dom'
import { AuthProvider } from '../auth/AuthProvider'
import CadastroEventoLuppa from '../pages/CadastroEventoLuppa/CadastroEventoLuppa'
import CategoryEventsLuppa from '../pages/CategoryEventsLuppa/CategoryEventsLuppa'
import ExploreEventsLuppa from '../pages/ExploreEventsLuppa/ExploreEventsLuppa'
import HomeLuppa from '../pages/HomeLuppa/HomeLuppa'
import LoginCadastroLuppa from '../pages/LoginCadastroLuppa/LoginCadastroLuppa'
import AccountActionPage from '../pages/LoginCadastroLuppa/AccountActionPage'
import LegalPage from '../pages/MenuLuppa/LegalPage'
import SupportPage from '../pages/MenuLuppa/SupportPage'
import FaqPage from '../pages/MenuLuppa/FaqPage'
import AdminShell from '../pages/AdminLuppa/AdminShell'
import AdminOverview from '../pages/AdminLuppa/AdminOverview'
import AdminInsights from '../pages/AdminLuppa/AdminInsights'
import AdminUsers from '../pages/AdminLuppa/AdminUsers'
import AdminEvents from '../pages/AdminLuppa/AdminEvents'
import AdminModeration from '../pages/AdminLuppa/AdminModeration'
import AdminReports from '../pages/AdminLuppa/AdminReports'
import AdminAuditLogs from '../pages/AdminLuppa/AdminAuditLogs'
import MyEventsLuppa from '../pages/MyEventsLuppa/MyEventsLuppa'
import CreatorInsightsLuppa from '../pages/MyEventsLuppa/CreatorInsightsLuppa'
import ProfileLuppa from '../pages/ProfileLuppa/ProfileLuppa'
import ProtectedRoute from './ProtectedRoute'
import { userRoles } from '../constants/users'

const AppRouter = () => {
  return (
    <BrowserRouter basename={import.meta.env.BASE_URL}>
      <AuthProvider>
        <Routes>
          <Route path="/" element={<HomeLuppa />} />
          <Route path="/events" element={<ExploreEventsLuppa />} />
          <Route path="/events/category/:slug" element={<CategoryEventsLuppa />} />
          <Route path="/login-cadastro" element={<LoginCadastroLuppa />} />
          <Route path="/forgot-password" element={<AccountActionPage type="forgot" />} />
          <Route path="/reset-password" element={<AccountActionPage type="reset" />} />
          <Route path="/verify-email" element={<AccountActionPage type="verify" />} />
          <Route path="/resend-verification" element={<AccountActionPage type="resend" />} />
          <Route path="/support" element={<SupportPage />} />
          <Route path="/faq" element={<FaqPage />} />
          <Route path="/terms" element={<LegalPage type="terms" />} />
          <Route path="/privacy" element={<LegalPage type="privacy" />} />
          <Route
            path="/profile"
            element={(
              <ProtectedRoute>
                <ProfileLuppa />
              </ProtectedRoute>
            )}
          />
          <Route
            path="/communication-preferences"
            element={(
              <ProtectedRoute>
                <ProfileLuppa />
              </ProtectedRoute>
            )}
          />
          <Route
            path="/cadastrar-evento"
            element={(
              <ProtectedRoute roles={[userRoles.CREATOR, userRoles.ADMIN]}>
                <CadastroEventoLuppa />
              </ProtectedRoute>
            )}
          />
          <Route
            path="/my-events"
            element={(
              <ProtectedRoute roles={[userRoles.CREATOR, userRoles.ADMIN]}>
                <MyEventsLuppa />
              </ProtectedRoute>
            )}
          />
          <Route
            path="/creator/insights"
            element={(
              <ProtectedRoute roles={[userRoles.CREATOR, userRoles.ADMIN]}>
                <CreatorInsightsLuppa />
              </ProtectedRoute>
            )}
          />
          <Route
            path="/admin"
            element={(
              <ProtectedRoute roles={[userRoles.ADMIN]}>
                <AdminShell />
              </ProtectedRoute>
            )}
          >
            <Route index element={<AdminOverview />} />
            <Route path="insights" element={<AdminInsights />} />
            <Route path="users" element={<AdminUsers />} />
            <Route path="events" element={<AdminEvents />} />
            <Route path="review" element={<AdminModeration />} />
            <Route path="reports" element={<AdminReports />} />
            <Route path="audit" element={<AdminAuditLogs />} />
          </Route>
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  )
}

export default AppRouter
