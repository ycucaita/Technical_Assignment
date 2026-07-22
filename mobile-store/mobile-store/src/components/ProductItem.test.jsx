import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { BrowserRouter } from 'react-router-dom';
import ProductItem from './ProductItem';

describe('ProductItem UI Component', () => {
  // 1. Arrange: Define our mock input data (just like testing a backend service)
  const mockProduct = {
    id: '123',
    brand: 'Apple',
    model: 'iPhone 13',
    price: 900,
    imgUrl: 'https://example.com/iphone.jpg'
  };

  it('renders the product details correctly onto the screen', () => {
    // 2. Act: Render the component into our virtual DOM (jsdom)
    // Note: We wrap it in BrowserRouter because our component contains a React Router <Link>
    render(
      <BrowserRouter>
        <ProductItem product={mockProduct} />
      </BrowserRouter>
    );

    // 3. Assert: Query the screen exactly how a user would look for text
    expect(screen.getByText('Apple')).toBeTruthy();
    expect(screen.getByText('iPhone 13')).toBeTruthy();
    expect(screen.getByText('900 €')).toBeTruthy();
    
    // We can also query elements by their semantic HTML roles
    const image = screen.getByRole('img');
    expect(image.getAttribute('src')).toBe('https://example.com/iphone.jpg');
    expect(image.getAttribute('alt')).toBe('iPhone 13');
  });
});