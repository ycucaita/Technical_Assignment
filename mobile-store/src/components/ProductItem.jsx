import { Card, CardActionArea, CardContent, CardMedia, Typography, Box } from '@mui/material';
import { Link } from 'react-router-dom';

const ProductItem = ({ product }) => {
  return (
    <Card sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      {/* Acts as a clickable area to navigate to details[cite: 1] */}
      <CardActionArea 
        component={Link} 
        to={`/product/${product.id}`} 
        sx={{ flexGrow: 1, display: 'flex', flexDirection: 'column', justifyContent: 'flex-start' }}
      >
        <Box sx={{ p: 2, bgcolor: 'white', width: '100%', display: 'flex', justifyContent: 'center' }}>
          {/* Display the image of the product[cite: 1] */}
          <CardMedia
            component="img"
            image={product.imgUrl}
            alt={product.model}
            sx={{ objectFit: 'contain', height: 160 }}
          />
        </Box>
        <CardContent sx={{ flexGrow: 1, width: '100%' }}>
          {/* Display Brand[cite: 1] */}
          <Typography variant="caption" color="text.secondary" sx={{ textTransform: 'uppercase', letterSpacing: 1 }}>
            {product.brand}
          </Typography>
          {/* Display Model[cite: 1] */}
          <Typography variant="h6" component="div" sx={{ lineHeight: 1.2, mt: 0.5, mb: 1 }}>
            {product.model}
          </Typography>
          {/* Display Price[cite: 1] */}
          <Typography variant="body1" color="primary" sx={{ fontWeight: 'bold' }}>
            {product.price ? `${product.price} €` : 'Price not available'}
          </Typography>
        </CardContent>
      </CardActionArea>
    </Card>
  );
};

export default ProductItem;