import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { BrowserRouter } from 'react-router-dom';
import ProductListPage from './ProductListPage';
import * as api from '../services/api';

// 1. Mock the entire API service module
vi.mock('../services/api', () => ({
  getProducts: vi.fn(),
}));

describe('ProductListPage UI Component', () => {
  // Define a static list of devices to test the filtering logic
  const mockProducts = [
    { id: '1', brand: 'Apple', model: 'iPhone 13', price: 900, imgUrl: '' },
    { id: '2', brand: 'Samsung', model: 'Galaxy S21', price: 800, imgUrl: '' },
    { id: '3', brand: 'Apple', model: 'iPad Air', price: 600, imgUrl: '' },
  ];

  beforeEach(() => {
    // Clear mocks between tests to prevent data leakage
    vi.clearAllMocks();
  });

  it('renders products and filters them in real-time based on search input', async () => {
    // Arrange: Tell our mocked API to return the fake data
    api.getProducts.mockResolvedValue(mockProducts);

    // Act: Render the component
    render(
      <BrowserRouter>
        <ProductListPage />
      </BrowserRouter>
    );

    // Assert Initial State: Wait for the API mock to resolve and check that all items are on screen
    // We use waitFor() because the component starts with a loading spinner
    await waitFor(() => {
      expect(screen.getByText('iPhone 13')).toBeTruthy();
    });
    expect(screen.getByText('Galaxy S21')).toBeTruthy();
    expect(screen.getByText('iPad Air')).toBeTruthy();

    // Act: Find the search input by its label and simulate a user typing "Samsung"
    const searchInput = screen.getByLabelText(/Search brand or model/i);
    fireEvent.change(searchInput, { target: { value: 'Samsung' } });

    // Assert Filtered State: Only Samsung should be visible now[cite: 1]
    expect(screen.getByText('Galaxy S21')).toBeTruthy();
    
    // queryByText returns null if the element is not found, which is exactly what we want for Apple products
    expect(screen.queryByText('iPhone 13')).toBeNull(); 
    expect(screen.queryByText('iPad Air')).toBeNull();
  });

  it('displays a fallback message when no products match the search', async () => {
    api.getProducts.mockResolvedValue(mockProducts);

    render(
      <BrowserRouter>
        <ProductListPage />
      </BrowserRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('iPhone 13')).toBeTruthy();
    });

    // Act: Type a brand that does not exist
    const searchInput = screen.getByLabelText(/Search brand or model/i);
    fireEvent.change(searchInput, { target: { value: 'Motorola' } });

    // Assert: The fallback UI should appear
    expect(screen.getByText(/No products found matching "Motorola"/i)).toBeTruthy();
  });
});