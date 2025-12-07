import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { ThemeProvider, createTheme } from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';
import Box from '@mui/material/Box';
import { AuthProvider } from './contexts/AuthContext';
import { NotificationProvider } from './contexts/NotificationContext';

// Layout
import DashboardLayout from './components/layout/DashboardLayout';

// Pages
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import Billing from './pages/Billing';
import Invoices from './pages/Invoices';
import Analytics from './pages/Analytics';
import Customers from './pages/Customers';
import Settings from './pages/Settings';

// Protected Route
import ProtectedRoute from './components/auth/ProtectedRoute';

const theme = createTheme({
  palette: {
    mode: 'light',
    primary: {
      main: '#1976d2',
    },
    secondary: {
      main: '#dc004e',
    },
    background: {
      default: '#f5f5f5',
    },
  },
  typography: {
    fontFamily: '"Roboto", "Helvetica", "Arial", sans-serif',
  },
});

function App() {
  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <NotificationProvider>
        <AuthProvider>
          <Router>
            <Box sx={{ display: 'flex' }}>
              <Routes>
                <Route path="/login" element={<Login />} />
                <Route
                  path="/"
                  element={
                    <ProtectedRoute>
                      <DashboardLayout />
                    </ProtectedRoute>
                  }
                >
                  <Route index element={<Navigate to="/dashboard" />} />
                  <Route path="dashboard" element={<Dashboard />} />
                  <Route path="billing" element={<Billing />} />
                  <Route path="invoices" element={<Invoices />} />
                  <Route path="analytics" element={<Analytics />} />
                  <Route path="customers" element={<Customers />} />
                  <Route path="settings" element={<Settings />} />
                </Route>
              </Routes>
            </Box>
          </Router>
        </AuthProvider>
      </NotificationProvider>
    </ThemeProvider>
  );
}

export default App;
