import { render, screen, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { BrowserRouter } from 'react-router-dom';
import ProductDetailsPage from './ProductDetailsPage';
import * as api from '../services/api';
import { CartProvider } from '../hooks/CartContext';

// 1. Mock the API Service
vi.mock('../services/api', () => ({
  getProductDetails: vi.fn(),
}));

// 2. Mock React Router to simulate the URL path parameter
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual, 
    useParams: () => ({ id: '999' }), 
  };
});

// 3. Explicitly mock localStorage (Since CartProvider needs it to mount)
const localStorageMock = (() => {
  let store = {};
  return {
    getItem: vi.fn((key) => store[key] || null),
    setItem: vi.fn((key, value) => {
      store[key] = value.toString();
    }),
    clear: vi.fn(() => {
      store = {};
    }),
  };
})();
global.localStorage = localStorageMock;

describe('ProductDetailsPage UI Component', () => {
  const mockProductDetails = {
    id: '999',
    brand: 'Google',
    model: 'Pixel 6',
    price: 599,
    imgUrl: 'pixel.jpg',
    cpu: 'Google Tensor',
    ram: '8GB',
    os: 'Android 12',
    displayResolution: '1080 x 2400',
    battery: '4614 mAh',
    primaryCamera: '50 MP',
    secondaryCmera: '8 MP',
    dimentions: '158.6 x 74.8 x 8.9 mm',
    weight: '207',
    options: {
      colors: [{ code: '100', name: 'Stormy Black' }], 
      storages: [
        { code: '200', name: '128GB' }, 
        { code: '201', name: '256GB' }
      ] 
    }
  };

  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear(); // Clear the storage before each test
  });

  it('fetches data using the URL parameter and renders specifications', async () => {
    // Arrange
    api.getProductDetails.mockResolvedValue(mockProductDetails);

    // Act
    render(
      <CartProvider>
        <BrowserRouter>
          <ProductDetailsPage />
        </BrowserRouter>
      </CartProvider>
    );

    // Assert
    expect(api.getProductDetails).toHaveBeenCalledWith('999');

    await waitFor(() => {
      expect(screen.getByText('Google Pixel 6')).toBeTruthy();
    });
    
    expect(screen.getByText('599 €')).toBeTruthy();
    expect(screen.getByText(/Google Tensor/i)).toBeTruthy();
    expect(screen.getByText(/Android 12/i)).toBeTruthy();
  });

  it('keeps the Add to Cart button disabled until all options are selected', async () => {
    // Arrange
    api.getProductDetails.mockResolvedValue(mockProductDetails);

    // Act
    render(
      <CartProvider>
        <BrowserRouter>
          <ProductDetailsPage />
        </BrowserRouter>
      </CartProvider>
    );

    await waitFor(() => {
      expect(screen.getByText('Google Pixel 6')).toBeTruthy();
    });

    // Assert
    const addToCartButton = screen.getByRole('button', { name: /Add to Cart/i });
    expect(addToCartButton.disabled).toBe(true);
  });
});