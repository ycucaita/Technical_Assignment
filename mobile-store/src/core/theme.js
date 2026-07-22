import { createTheme } from '@mui/material/styles';

export const appTheme = createTheme({
  palette: {
    primary: {
      main: '#E8A07C', // Your requested main color
      contrastText: '#1A1A1A', // Dark text for better contrast against the light orange
    },
    background: {
      default: '#F8F9FA', // A very soft gray to make the white cards pop
    },
  },
  typography: {
    fontFamily: '"Roboto", "Helvetica", "Arial", sans-serif',
  },
});