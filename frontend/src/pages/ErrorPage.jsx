import { useSearchParams, Link } from 'react-router-dom';
import './ErrorPage.css';

export default function ErrorPage() {
  const [searchParams] = useSearchParams();
  const errorMessage = searchParams.get('message') || 'An unexpected error occurred';

  return (
    <div className="error-page" id="error-page">
      <div className="error-card">
        <div className="error-icon">
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
            <circle cx="12" cy="12" r="10" />
            <line x1="15" y1="9" x2="9" y2="15" />
            <line x1="9" y1="9" x2="15" y2="15" />
          </svg>
        </div>
        <h1 className="error-title">Something went wrong</h1>
        <p className="error-message">{errorMessage}</p>
        <Link to="/" className="error-back-btn" id="error-back-button">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <line x1="19" y1="12" x2="5" y2="12" /><polyline points="12 19 5 12 12 5" />
          </svg>
          Back to Login
        </Link>
      </div>
    </div>
  );
}
