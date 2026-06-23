// API base URL — points to the Spring Boot backend
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

// App metadata
export const APP_NAME = 'SF Rule Manager';
export const APP_DESCRIPTION = 'Salesforce Validation Rule Manager';

// Routes
export const ROUTES = {
  HOME: '/',
  DASHBOARD: '/dashboard',
  CALLBACK: '/callback',
  ERROR: '/error',
};

// API endpoints
export const API = {
  AUTH: {
    LOGIN: `${API_BASE_URL}/api/auth/login`,
    CALLBACK: `${API_BASE_URL}/api/auth/callback`,
    STATUS: `${API_BASE_URL}/api/auth/status`,
    USER: `${API_BASE_URL}/api/auth/user`,
    LOGOUT: `${API_BASE_URL}/api/auth/logout`,
  },
  RULES: {
    LIST: `${API_BASE_URL}/api/validation-rules`,
    TOGGLE: (name) => `${API_BASE_URL}/api/validation-rules/${name}/toggle`,
    TOGGLE_MULTIPLE: `${API_BASE_URL}/api/validation-rules/toggle-multiple`,
    DEPLOY: `${API_BASE_URL}/api/validation-rules/deploy`,
    DEPLOY_STATUS: (id) => `${API_BASE_URL}/api/validation-rules/${id}/status`,
  },
};
