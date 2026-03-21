import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import Apply from './pages/merchant/Apply';
import Status from './pages/merchant/Status';
import Credentials from './pages/merchant/Credentials';
import Dashboard from './pages/merchant/Dashboard';

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Apply />} />
        <Route path="/status" element={<Status />} />
        <Route path="/credentials" element={<Credentials />} />
        <Route path="/dashboard" element={<Dashboard />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
