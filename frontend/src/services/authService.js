import api from './axiosConfig';

export const authService = {
  // Get the Salesforce OAuth login URL from the backend
  getLoginUrl: () => {
    return `${api.defaults.baseURL}/api/auth/login`;
  },

  // Check if the user is authenticated
  checkAuthStatus: async () => {
    const response = await api.get('/api/auth/status');
    return response.data;
  },

  // Get the authenticated user's profile
  getUserProfile: async () => {
    const response = await api.get('/api/auth/user');
    return response.data;
  },

  // Logout the user (revokes Salesforce token + kills session)
  logout: async () => {
    const response = await api.post('/api/auth/logout');
    return response.data;
  },
};

export default authService;
