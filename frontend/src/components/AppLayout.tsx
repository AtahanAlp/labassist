import AppBar from '@mui/material/AppBar';
import Toolbar from '@mui/material/Toolbar';
import Typography from '@mui/material/Typography';
import Button from '@mui/material/Button';
import Box from '@mui/material/Box';
import Container from '@mui/material/Container';
import Chip from '@mui/material/Chip';
import Tabs from '@mui/material/Tabs';
import Tab from '@mui/material/Tab';
import ScienceIcon from '@mui/icons-material/Science';
import LogoutIcon from '@mui/icons-material/Logout';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../auth/useAuth';

export function AppLayout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const navItems = [
    { label: 'Reports', path: '/reports' },
    ...(user?.role === 'ADMIN'
      ? [
          { label: 'Users', path: '/users' },
          { label: 'Audit log', path: '/audit' },
        ]
      : []),
  ];

  // Keep "Reports" active on the detail route too.
  const activeItem =
    navItems.find((item) =>
      item.path === '/reports' ? location.pathname.startsWith('/reports') : location.pathname === item.path,
    )?.path ?? false;

  const handleLogout = () => {
    logout();
    navigate('/login', { replace: true });
  };

  return (
    <Box sx={{ minHeight: '100vh', bgcolor: 'background.default' }}>
      <AppBar position="sticky" color="default">
        <Toolbar sx={{ gap: 2 }}>
          <Box
            sx={{ display: 'flex', alignItems: 'center', cursor: 'pointer' }}
            onClick={() => navigate('/reports')}
          >
            <ScienceIcon color="primary" sx={{ mr: 1 }} />
            <Typography variant="h6">LabAssist</Typography>
          </Box>

          <Tabs
            value={activeItem}
            onChange={(_, value) => navigate(value)}
            textColor="primary"
            indicatorColor="primary"
            sx={{ minHeight: 64, '& .MuiTab-root': { minHeight: 64, fontWeight: 600 } }}
          >
            {navItems.map((item) => (
              <Tab key={item.path} label={item.label} value={item.path} />
            ))}
          </Tabs>

          <Box sx={{ flexGrow: 1 }} />
          {user && (
            <>
              <Chip size="small" label={user.role} sx={{ mr: 1 }} />
              <Typography variant="body2" sx={{ mr: 2 }}>
                {user.displayName ?? user.username}
              </Typography>
              <Button color="inherit" size="small" startIcon={<LogoutIcon />} onClick={handleLogout}>
                Log out
              </Button>
            </>
          )}
        </Toolbar>
      </AppBar>
      <Container maxWidth="lg" sx={{ py: 3 }}>
        <Outlet />
      </Container>
    </Box>
  );
}
