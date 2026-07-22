import { createContext, useState, useContext } from 'react';
import { addToCart } from '../services/api';

const CartContext = createContext();

export const CartProvider = ({ children }) => {
  // Initialize state from localStorage to persist the data
  const [cartCount, setCartCount] = useState(() => {
    return parseInt(localStorage.getItem('cart_count') || '0', 10);
  });

  const addProductToCart = async (id, colorCode, storageCode) => {
    try {
      // API call requires id, colorCode, and storageCode
      const response = await addToCart(id, colorCode, storageCode);
      const newCount = cartCount + response.count; // The response returns { count: 1 }[cite: 1]
      
      setCartCount(newCount);
      localStorage.setItem('cart_count', newCount); // Persist the data[cite: 1]
    } catch (error) {
      console.error("Failed to add to cart", error);
    }
  };

  return (
    <CartContext.Provider value={{ cartCount, addProductToCart }}>
      {children}
    </CartContext.Provider>
  );
};

// Custom hook to cleanly consume the context
export const useCart = () => useContext(CartContext);