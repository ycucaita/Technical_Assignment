import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { CartProvider, useCart } from './CartContext';
import * as api from '../services/api';

// 1. Mock the API service
vi.mock('../services/api', () => ({
  addToCart: vi.fn(),
}));

// 2. Mock localStorage (Required since CartContext reads/writes to it)
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

// 3. Create a fake component to consume our hook during tests
const DummyCartConsumer = () => {
  const { cartCount, addProductToCart } = useCart();
  
  return (
    <div>
      <span data-testid="count-display">Count: {cartCount}</span>
      <button onClick={() => addProductToCart('123', 'red', '64gb')}>
        Add Item
      </button>
    </div>
  );
};

describe('CartContext & useCart Hook', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
  });

  it('initializes the cart count from localStorage if it exists', () => {
    // Arrange: Pre-populate the mock localStorage before the provider mounts
    localStorage.setItem('cart_count', '5');

    // Act
    render(
      <CartProvider>
        <DummyCartConsumer />
      </CartProvider>
    );

    // Assert: The component should read '5' from our mocked storage
    expect(screen.getByTestId('count-display').textContent).toBe('Count: 5');
  });

  it('defaults to 0 if localStorage is empty', () => {
    // Arrange (localStorage is already cleared in beforeEach)

    // Act
    render(
      <CartProvider>
        <DummyCartConsumer />
      </CartProvider>
    );

    // Assert
    expect(screen.getByTestId('count-display').textContent).toBe('Count: 0');
  });

  it('calls the API, updates the state, and saves to localStorage when an item is added', async () => {
    // Arrange: Tell the mocked API to return a count of 1 when called
    api.addToCart.mockResolvedValue({ count: 1 });

    render(
      <CartProvider>
        <DummyCartConsumer />
      </CartProvider>
    );

    // Verify initial state is 0
    expect(screen.getByTestId('count-display').textContent).toBe('Count: 0');

    // Act: Simulate a user clicking the Add Item button
    fireEvent.click(screen.getByText('Add Item'));

    // Assert: Verify the API was called with the exact parameters from our Dummy Component
    expect(api.addToCart).toHaveBeenCalledWith('123', 'red', '64gb');

    // Assert: Wait for React to update the state and DOM
    await waitFor(() => {
      expect(screen.getByTestId('count-display').textContent).toBe('Count: 1');
    });

    // Assert: Verify the new count was persisted to localStorage
    expect(localStorage.setItem).toHaveBeenCalledWith('cart_count', 1);
  });
});