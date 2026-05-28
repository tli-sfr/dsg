import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { AccountBar } from './components/AccountBar';
import { DashboardPage } from './pages/DashboardPage';
import { OAuthCallbackPage } from './pages/OAuthCallbackPage';
import { RuleFormPage } from './pages/RuleFormPage';

export default function App() {
  return (
    <BrowserRouter>
      <AccountBar />
      <Routes>
        <Route path="/" element={<Navigate to="/directory-integration" replace />} />
        <Route path="/directory-integration" element={<DashboardPage />} />
        <Route path="/directory-integration/rules/new" element={<RuleFormPage />} />
        <Route path="/oauth/callback" element={<OAuthCallbackPage />} />
      </Routes>
    </BrowserRouter>
  );
}
