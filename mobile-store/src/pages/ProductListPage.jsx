import { useState, useEffect } from 'react';
import { Container, Grid, TextField, Typography, Box, CircularProgress } from '@mui/material';
import { getProducts } from '../services/api';
import ProductItem from '../components/ProductItem';

const ProductListPage = () => {
  const [products, setProducts] = useState([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [loading, setLoading] = useState(true);

  // Fetch the list of products when the component mounts[cite: 1]
  useEffect(() => {
    getProducts()
      .then(data => {
        setProducts(data);
        setLoading(false);
      })
      .catch(error => {
        console.error("Failed to load products:", error);
        setLoading(false);
      });
  }, []);

  // Filter in real-time based on the user's input compared with Brand and Model[cite: 1]
  const filteredProducts = products.filter(product => {
    const searchLower = searchQuery.toLowerCase();
    return (
      product.brand.toLowerCase().includes(searchLower) ||
      product.model.toLowerCase().includes(searchLower)
    );
  });

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', mt: 10 }}>
        <CircularProgress color="primary" />
      </Box>
    );
  }

  return (
    <Container sx={{ mt: 4, mb: 6 }}>
      <Box sx={{ display: 'flex', flexWrap: 'wrap', justifyContent: 'space-between', alignItems: 'center', mb: 4, gap: 2 }}>
        <Typography variant="h5" component="h1" sx={{ fontWeight: 'bold' }}>
          Explore Devices
        </Typography>
        
        {/* Input for the user to introduce a text string[cite: 1] */}
        <TextField 
          label="Search brand or model..." 
          variant="outlined"
          size="small"
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          sx={{ minWidth: { xs: '100%', sm: '300px' }, bgcolor: 'white' }}
        />
      </Box>

      <Grid container spacing={3}>
        {/* Adaptive grid showing a max of 4 elements per row (lg={3} means 12/3 = 4 cols)[cite: 1] */}
        {filteredProducts.map(product => (
          <Grid item xs={12} sm={6} md={4} lg={3} key={product.id}>
            <ProductItem product={product} />
          </Grid>
        ))}

        {/* Fallback state when the search yields no results */}
        {filteredProducts.length === 0 && (
          <Grid item xs={12}>
            <Typography variant="body1" color="text.secondary" align="center" sx={{ mt: 4 }}>
              No products found matching "{searchQuery}".
            </Typography>
          </Grid>
        )}
      </Grid>
    </Container>
  );
};

export default ProductListPage;