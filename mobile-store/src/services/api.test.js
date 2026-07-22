import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { getProducts } from './api';

// 1. Mock the global fetch function
global.fetch = vi.fn();

// 2. Explicitly mock localStorage to bypass Node.js experimental warnings
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

describe('API Service - Caching Logic', () => {
  const mockProducts = [{ id: '1', brand: 'Acer', model: 'Iconia' }];
  const CACHE_KEY = 'products_list';

  beforeEach(() => {
    // Clear our mocked localStorage and reset fetch mocks before each test
    localStorage.clear();
    fetch.mockReset();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('should fetch data from the API if localStorage is empty', async () => {
    // Arrange
    fetch.mockResolvedValueOnce({
      ok: true,
      json: async () => mockProducts,
    });

    // Act
    const data = await getProducts();

    // Assert
    expect(fetch).toHaveBeenCalledTimes(1);
    expect(data).toEqual(mockProducts);
    
    const cachedItem = JSON.parse(localStorage.getItem(CACHE_KEY));
    expect(cachedItem.data).toEqual(mockProducts);
    expect(cachedItem.timestamp).toBeDefined();
  });

  it('should return cached data and NOT call fetch if cache is valid (under 1 hour)', async () => {
    // Arrange
    const validTimestamp = Date.now() - (5 * 60 * 1000); 
    localStorage.setItem(CACHE_KEY, JSON.stringify({
      data: mockProducts,
      timestamp: validTimestamp
    }));

    // Act
    const data = await getProducts();

    // Assert
    expect(fetch).not.toHaveBeenCalled();
    expect(data).toEqual(mockProducts);
  });

  it('should call fetch if the cache is expired (over 1 hour)', async () => {
    // Arrange
    const expiredTimestamp = Date.now() - (2 * 60 * 60 * 1000); 
    localStorage.setItem(CACHE_KEY, JSON.stringify({
      data: [{ id: 'old_data' }],
      timestamp: expiredTimestamp
    }));

    fetch.mockResolvedValueOnce({
      ok: true,
      json: async () => mockProducts,
    });

    // Act
    const data = await getProducts();

    // Assert
    expect(fetch).toHaveBeenCalledTimes(1);
    expect(data).toEqual(mockProducts);
  });
});