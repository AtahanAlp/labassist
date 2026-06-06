import AppBar from '@mui/material/AppBar';
import Toolbar from '@mui/material/Toolbar';
import Typography from '@mui/material/Typography';
import Button from '@mui/material/Button';
import Box from '@mui/material/Box';
import Container from '@mui/material/Container';
import Chip from '@mui/material/Chip';
import ScienceIcon from '@mui/icons-material/Science';
import LogoutIcon from '@mui/icons-material/Logout';
import { Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';

export function AppLayout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login', { replace: true });
  };

  return (
    <Box sx={{ minHeight: '100vh', bgcolor: 'background.default' }}>
      <AppBar position="sticky" color="default">
        <Toolbar>
          <ScienceIcon color="primary" sx={{ mr: 1 }} />
          <Typography
            variant="h6"
            sx={{ cursor: 'pointer', mr: 2 }}
            onClick={() => navigate('/reports')}
          >
            LabAssist
          </Typography>
          {user?.role === 'ADMIN' && (
            <>
              <Button color="inherit" size="small" onClick={() => navigate('/users')}>
                Users
              </Button>
              <Button color="inherit" size="small" onClick={() => navigate('/audit')}>
                Audit log
              </Button>
            </>
          )}
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
