import { createContext, useContext, useState, useEffect, useCallback } from 'react';
import authService from '../services/authService';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [isLoading, setIsLoading] = useState(true);

  const checkAuth = useCallback(async () => {
    try {
      setIsLoading(true);
      const statusResponse = await authService.checkAuthStatus();
      console.log('Status Response:', statusResponse);
      
      // Make the check more robust in case it's a string or truthy value
      if (statusResponse && statusResponse.success && statusResponse.data) {
        console.log('Status is authenticated, fetching user profile...');
        const userResponse = await authService.getUserProfile();
        console.log('User Response:', userResponse);
        if (userResponse && userResponse.success) {
          setUser(userResponse.data);
          setIsAuthenticated(true);
        } else {
          console.warn('User profile fetch failed:', userResponse);
          setUser(null);
          setIsAuthenticated(false);
        }
      } else {
        console.warn('Status check returned unauthenticated or failed:', statusResponse);
        setUser(null);
        setIsAuthenticated(false);
      }
    } catch (error) {
      console.error('Auth check failed:', error);
      setUser(null);
      setIsAuthenticated(false);
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    checkAuth();
  }, [checkAuth]);

  const login = () => {
    window.location.href = authService.getLoginUrl();
  };

  const logout = async () => {
    try {
      await authService.logout();
    } catch (error) {
      console.error('Logout failed:', error);
    } finally {
      setUser(null);
      setIsAuthenticated(false);
    }
  };

  const value = {
    user,
    isAuthenticated,
    isLoading,
    login,
    logout,
    checkAuth,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}

export default AuthContext;
