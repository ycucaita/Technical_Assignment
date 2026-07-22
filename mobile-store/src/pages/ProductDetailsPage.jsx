import { useState, useEffect } from 'react';
import { useParams, Link as RouterLink } from 'react-router-dom';
import { 
  Container, Grid, Typography, Button, CircularProgress, Box, 
  Select, MenuItem, FormControl, InputLabel, Divider, Link 
} from '@mui/material';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import { getProductDetails } from '../services/api';
import { useCart } from '../hooks/CartContext';

const ProductDetailsPage = () => {
  const { id } = useParams();
  const { addProductToCart } = useCart();
  
  const [product, setProduct] = useState(null);
  const [loading, setLoading] = useState(true);
  
  const [selectedColor, setSelectedColor] = useState('');
  const [selectedStorage, setSelectedStorage] = useState('');

  useEffect(() => {
    getProductDetails(id).then(data => {
      setProduct(data);
      
      // If there is only one option, it must be selected by default[cite: 1]
      if (data.options?.colors?.length === 1) {
        setSelectedColor(data.options.colors[0].code);
      }
      if (data.options?.storages?.length === 1) {
        setSelectedStorage(data.options.storages[0].code);
      }
      
      setLoading(false);
    }).catch(() => setLoading(false));
  }, [id]);

  if (loading) return <Box sx={{ display: 'flex', justifyContent: 'center', mt: 10 }}><CircularProgress /></Box>;
  if (!product) return <Typography align="center" sx={{ mt: 5 }}>Product not found</Typography>;

  return (
    <Container sx={{ mt: 4, mb: 6 }}>
      {/* Link to navigate back to the product list[cite: 1] */}
      <Link component={RouterLink} to="/" underline="hover" sx={{ display: 'flex', alignItems: 'center', mb: 3, color: 'text.secondary' }}>
        <ArrowBackIcon sx={{ mr: 1, fontSize: 'small' }} /> Back to list
      </Link>
      
      <Grid container spacing={6}>
        {/* First column: Image[cite: 1] */}
        <Grid item xs={12} md={6} sx={{ display: 'flex', justifyContent: 'center', alignItems: 'flex-start' }}>
          <img src={product.imgUrl} alt={product.model} style={{ maxWidth: '100%', maxHeight: '500px', objectFit: 'contain' }} />
        </Grid>
        
        {/* Second column: Details and Actions[cite: 1] */}
        <Grid item xs={12} md={6}>
          <Typography variant="h4" fontWeight="bold">{product.brand} {product.model}</Typography>
          <Typography variant="h5" color="primary" sx={{ mt: 1, mb: 3, fontWeight: 'bold' }}>{product.price} €</Typography>
          
          {/* Product Description Details[cite: 1] */}
          <Box sx={{ mb: 4 }}>
            <Typography variant="h6" gutterBottom>Specifications</Typography>
            <Typography variant="body2"><strong>CPU:</strong> {product.cpu}</Typography>
            <Typography variant="body2"><strong>RAM:</strong> {product.ram}</Typography>
            <Typography variant="body2"><strong>OS:</strong> {product.os}</Typography>
            <Typography variant="body2"><strong>Resolution:</strong> {product.displayResolution}</Typography>
            <Typography variant="body2"><strong>Battery:</strong> {product.battery}</Typography>
            <Typography variant="body2"><strong>Cameras:</strong> {product.primaryCamera} (Main) / {product.secondaryCmera} (Front)</Typography>
            <Typography variant="body2"><strong>Dimensions:</strong> {product.dimentions}</Typography>
            <Typography variant="body2"><strong>Weight:</strong> {product.weight}g</Typography>
          </Box>
          
          <Divider sx={{ my: 3 }} />

          {/* Product Actions[cite: 1] */}
          <Typography variant="h6" gutterBottom>Options</Typography>
          <Box sx={{ display: 'flex', gap: 2, mb: 4, flexWrap: 'wrap' }}>
            <FormControl sx={{ minWidth: 150 }}>
              <InputLabel>Storage</InputLabel>
              <Select 
                value={selectedStorage} 
                label="Storage"
                onChange={(e) => setSelectedStorage(e.target.value)}
              >
                {product.options?.storages.map(s => (
                  <MenuItem key={s.code} value={s.code}>{s.name}</MenuItem>
                ))}
              </Select>
            </FormControl>
            
            <FormControl sx={{ minWidth: 150 }}>
              <InputLabel>Color</InputLabel>
              <Select 
                value={selectedColor} 
                label="Color"
                onChange={(e) => setSelectedColor(e.target.value)}
              >
                {product.options?.colors.map(c => (
                  <MenuItem key={c.code} value={c.code}>{c.name}</MenuItem>
                ))}
              </Select>
            </FormControl>
          </Box>

          <Button 
            variant="contained" 
            color="primary" 
            size="large"
            fullWidth
            disabled={!selectedColor || !selectedStorage}
            onClick={() => addProductToCart(product.id, selectedColor, selectedStorage)}
          >
            Add to Cart
          </Button>
        </Grid>
      </Grid>
    </Container>
  );
};

export default ProductDetailsPage;