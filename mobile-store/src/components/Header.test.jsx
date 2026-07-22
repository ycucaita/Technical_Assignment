import { render, screen } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import Header from './Header';
import * as CartContext from '../hooks/CartContext';

// Mock the useCart hook so we can control the cart count output
vi.mock('../hooks/CartContext', () => ({
  useCart: vi.fn(),
}));

describe('Header UI Component', () => {
  it('renders the main title and only the Home breadcrumb on the root path', () => {
    // Arrange: Simulate an empty cart
    CartContext.useCart.mockReturnValue({ cartCount: 0 });

    // Act: Use MemoryRouter to simulate being on the Home page ('/')
    render(
      <MemoryRouter initialEntries={['/']}>
        <Header />
      </MemoryRouter>
    );

    // Assert: Title and Home link exist
    expect(screen.getByText('📱 MobileStore')).toBeTruthy();
    expect(screen.getByText('Home')).toBeTruthy();
    
    // Assert: Product Details breadcrumb should NOT exist on the home page
    expect(screen.queryByText('Product Details')).toBeNull();
  });

  it('renders the Product Details breadcrumb when navigating to a product page', () => {
    // Arrange
    CartContext.useCart.mockReturnValue({ cartCount: 0 });

    // Act: Simulate being on a product details page
    render(
      <MemoryRouter initialEntries={['/product/123']}>
        <Header />
      </MemoryRouter>
    );

    // Assert: The breadcrumb trail should now include 'Product Details'
    expect(screen.getByText('Home')).toBeTruthy();
    expect(screen.getByText('Product Details')).toBeTruthy();
  });

  it('displays the correct cart count received from the global context', () => {
    // Arrange: Simulate a user who has added 7 items to their cart
    CartContext.useCart.mockReturnValue({ cartCount: 7 });

    // Act
    render(
      <MemoryRouter initialEntries={['/']}>
        <Header />
      </MemoryRouter>
    );

    // Assert: Material-UI Badge renders the number inside the DOM
    expect(screen.getByText('7')).toBeTruthy();
  });
});