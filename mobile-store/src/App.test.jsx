import { render, screen } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import App from './App';

// 1. Mock the Page Components
// We mock these because we already tested them in their own files. 
// Here, we only care if the Router successfully navigates to them.
vi.mock('./pages/ProductListPage', () => ({
  default: () => <div data-testid="home-view">Mocked Home Page</div>,
}));
vi.mock('./pages/ProductDetailsPage', () => ({
  default: () => <div data-testid="details-view">Mocked Details Page</div>,
}));

// 2. Explicitly mock localStorage (Since App renders the CartProvider)
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

describe('App Routing Configuration', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
  });

  it('renders the Product List Page on the root path ("/")', () => {
    // Arrange: Ensure the browser is at the root URL
    window.history.pushState({}, '', '/');

    // Act
    render(<App />);

    // Assert: The Home View should be mounted
    expect(screen.getByTestId('home-view')).toBeTruthy();
  });

  it('renders the Product Details Page on the "/product/:id" path', () => {
    // Arrange: Simulate the user entering a valid product URL
    window.history.pushState({}, '', '/product/12345');

    // Act
    render(<App />);

    // Assert: The Details View should be mounted
    expect(screen.getByTestId('details-view')).toBeTruthy();
  });

  it('redirects to the root path ("/") if an unknown URL is accessed', () => {
    // Arrange: Simulate the user entering a completely invalid URL
    window.history.pushState({}, '', '/invalid/url/that/does/not/exist');

    // Act
    render(<App />);

    // Assert: The router's catch-all (*) should have triggered a redirect back to Home
    expect(screen.getByTestId('home-view')).toBeTruthy();
    
    // We can also verify that the browser's URL was forcefully changed back to "/"
    expect(window.location.pathname).toBe('/');
  });
});