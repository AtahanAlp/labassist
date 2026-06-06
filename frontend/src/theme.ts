import { createTheme } from '@mui/material/styles';

/** Calm clinical palette: teal primary, clear semantic colors for flags. */
export const theme = createTheme({
  palette: {
    primary: { main: '#00838f' },
    secondary: { main: '#5c6bc0' },
    error: { main: '#c62828' },
    warning: { main: '#ef6c00' },
    success: { main: '#2e7d32' },
    background: { default: '#f4f6f8' },
  },
  shape: { borderRadius: 8 },
  typography: {
    fontFamily: 'Inter, Roboto, "Helvetica Neue", Arial, sans-serif',
    h5: { fontWeight: 600 },
    h6: { fontWeight: 600 },
  },
  components: {
    MuiAppBar: { defaultProps: { elevation: 0 }, styleOverrides: { root: { borderBottom: '1px solid rgba(0,0,0,0.08)' } } },
    MuiPaper: { defaultProps: { elevation: 0 }, styleOverrides: { root: { border: '1px solid rgba(0,0,0,0.08)' } } },
  },
});
