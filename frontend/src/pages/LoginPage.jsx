
import { useAuth } from '../context/AuthContext';
import { Navigate } from 'react-router-dom';
import LoadingSpinner from '../components/LoadingSpinner';
import './LoginPage.css';

export default function LoginPage() {
  const { isAuthenticated, isLoading, login } = useAuth();


  if (isLoading) {
    return <LoadingSpinner message="Checking authentication..." />;
  }

  if (isAuthenticated) {
    return <Navigate to="/dashboard" replace />;
  }

  const handleSalesforceLogin = () => {
    login(); // Trigger Salesforce OAuth
  };

  return (
    <div className="login-page">
      <div className="login-container">
        
        {/* Left Side */}
        <div className="login-left">
          <div className="login-left-content">
            <h1>WELCOME TO</h1>
            <h2>SF RULE MANAGER</h2>
            <p>
              Manage your Salesforce Account validation rules with ease. 
              Seamlessly toggle, deploy, and monitor all your configurations 
              from a single, secure platform.
            </p>
          </div>
        </div>

        {/* Right Side */}
        <div className="login-right">
          <div className="login-right-content">
            <h2>Sign in</h2>
            <p className="subtitle">Securely authenticate with your Salesforce credentials</p>
            
            <div className="login-actions" style={{ marginTop: '40px' }}>
              <button type="button" className="btn-primary" onClick={handleSalesforceLogin} style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '16px' }}>
                <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor" style={{ marginRight: '12px' }}>
                  <path d="M12.5 2C6.81 2 2.18 6.05 1.22 11.32c-.14.77.45 1.49 1.24 1.49.6 0 1.1-.42 1.22-1.01C4.46 7.34 8.09 4 12.5 4c4.97 0 9 4.03 9 9s-4.03 9-9 9c-4.41 0-8.04-3.34-8.82-7.8-.12-.59-.62-1.01-1.22-1.01-.79 0-1.38.72-1.24 1.49C2.18 19.95 6.81 24 12.5 24 18.85 24 24 18.85 24 12.5S18.85 2 12.5 2z"/>
                  <path d="M7 11.5L11.5 16 16 11.5" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
                </svg>
                Login with Salesforce
              </button>
            </div>

            <div className="signup-text">
              Don't have an account? <a href="#" className="signup-link">Sign Up</a>
            </div>
          </div>
        </div>

      </div>
    </div>
  );
}
