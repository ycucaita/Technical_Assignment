// Base URL provided in the requirements[cite: 1]
const BASE_URL = 'https://itx-frontend-test.onrender.com/api';

/**
 * Generic fetch function with 1-hour caching logic[cite: 1]
 * @param {string} key - The localStorage key
 * @param {string} endpoint - The API endpoint to fetch
 * @returns {Promise<any>} - The data from cache or API
 */
const fetchWithCache = async (key, endpoint) => {
  const cachedItem = localStorage.getItem(key);
  const ONE_HOUR_IN_MS = 60 * 60 * 1000;

  if (cachedItem) {
    try {
      const { data, timestamp } = JSON.parse(cachedItem);
      // Check if the cache is still valid (less than 1 hour old)[cite: 1]
      if (Date.now() - timestamp < ONE_HOUR_IN_MS) {
        console.log(`Serving ${key} from cache.`);
        return data;
      }
    } catch (e) {
      console.warn("Failed to parse cached data, fetching new data.");
    }
  }

  // If no cache or cache expired, fetch new data
  console.log(`Fetching ${key} from API.`);
  const response = await fetch(`${BASE_URL}${endpoint}`);
  
  if (!response.ok) {
    throw new Error(`API call failed: ${response.statusText}`);
  }

  const data = await response.json();
  
  // Save the new data and the current timestamp to localStorage[cite: 1]
  localStorage.setItem(key, JSON.stringify({
    data,
    timestamp: Date.now()
  }));

  return data;
};

// --- Exported API Methods ---

// Get the list of all products[cite: 1]
export const getProducts = () => {
  return fetchWithCache('products_list', '/product');
};

// Get the details of a specific product by ID[cite: 1]
export const getProductDetails = (id) => {
  return fetchWithCache(`product_${id}`, `/product/${id}`);
};

// Add a product to the cart (This doesn't need caching, just a POST request)[cite: 1]
export const addToCart = async (id, colorCode, storageCode) => {
  const response = await fetch(`${BASE_URL}/cart`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ id, colorCode, storageCode })
  });

  if (!response.ok) {
    throw new Error('Failed to add item to cart');
  }

  return response.json(); // Returns { count: number }[cite: 1]
};