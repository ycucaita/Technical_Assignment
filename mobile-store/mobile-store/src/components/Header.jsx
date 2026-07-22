import { AppBar, Toolbar, Typography, Badge, Breadcrumbs, Link as MuiLink } from '@mui/material';
import ShoppingCartIcon from '@mui/icons-material/ShoppingCart';
import { Link, useLocation } from 'react-router-dom';
import { useCart } from '../hooks/CartContext';

const Header = () => {
  const location = useLocation();
  const { cartCount } = useCart(); // Getting real count from our global state

  const isDetailsPage = location.pathname.includes('/product/');

  return (
    <AppBar position="sticky" color="primary">
      <Toolbar style={{ display: 'flex', justifyContent: 'space-between' }}>
        
        <div style={{ display: 'flex', alignItems: 'center', gap: '2rem' }}>
          <Typography 
            variant="h6" 
            component={Link} 
            to="/" 
            style={{ textDecoration: 'none', color: 'inherit', fontWeight: 'bold' }}
          >
            📱 MobileStore
          </Typography>

          <Breadcrumbs aria-label="breadcrumb" style={{ color: 'inherit' }}>
            <MuiLink component={Link} to="/" color="inherit" underline="hover">
              Home
            </MuiLink>
            {isDetailsPage && (
              <Typography color="text.secondary">Product Details</Typography>
            )}
          </Breadcrumbs>
        </div>

        <Badge badgeContent={cartCount} color="error" showZero>
          <ShoppingCartIcon />
        </Badge>
        
      </Toolbar>
    </AppBar>
  );
};

export default Header;