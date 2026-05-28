import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { AccountBar } from './components/AccountBar';
import { AppNav } from './components/AppNav';
import { RcAuthGuard } from './components/RcAuthGuard';
import { DashboardPage } from './pages/DashboardPage';
import { DirectoryConfigurationPage } from './pages/DirectoryConfigurationPage';
import { DirectoryOAuthCallbackPage } from './pages/DirectoryOAuthCallbackPage';
import { OAuthCallbackPage } from './pages/OAuthCallbackPage';
import { RuleFormPage } from './pages/RuleFormPage';

export default function App() {
  return (
    <BrowserRouter>
      <AccountBar />
      <AppNav />
      <RcAuthGuard>
        <Routes>
          <Route path="/" element={<Navigate to="/directory-integration" replace />} />
          <Route path="/directory-integration" element={<DashboardPage />} />
          <Route path="/directory-integration/configuration" element={<DirectoryConfigurationPage />} />
          <Route path="/directory-integration/rules/new" element={<RuleFormPage />} />
          <Route path="/directory-integration/oauth/callback" element={<DirectoryOAuthCallbackPage />} />
          <Route path="/oauth/callback" element={<OAuthCallbackPage />} />
          <Route path="/mobile/oauthredirect" element={<OAuthCallbackPage />} />
        </Routes>
      </RcAuthGuard>
    </BrowserRouter>
  );
}
