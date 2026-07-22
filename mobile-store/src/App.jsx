import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { ThemeProvider, CssBaseline } from '@mui/material';
import { appTheme } from './core/theme';
import Header from './components/Header';
import ProductListPage from './pages/ProductListPage';
import ProductDetailsPage from './pages/ProductDetailsPage';
import { CartProvider } from './hooks/CartContext'; // Import the provider

function App() {
  return (
    <ThemeProvider theme={appTheme}>
      <CssBaseline />
      {/* The CartProvider MUST wrap the Header and Routes so they can access the cart state */}
      <CartProvider>
        <BrowserRouter>
          <Header />
          <Routes>
            <Route path="/" element={<ProductListPage />} />
            <Route path="/product/:id" element={<ProductDetailsPage />} />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </BrowserRouter>
      </CartProvider>
    </ThemeProvider>
  );
}

export default App;