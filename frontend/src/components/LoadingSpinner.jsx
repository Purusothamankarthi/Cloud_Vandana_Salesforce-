import './LoadingSpinner.css';

export default function LoadingSpinner({ message = 'Loading...', fullScreen = true }) {
  return (
    <div className={`spinner-container ${fullScreen ? 'spinner-fullscreen' : ''}`}>
      <div className={fullScreen ? 'spinner-card' : ''}>
        <div className="spinner-content">
          <div className="spinner-ring">
            <div className="spinner-ring-inner"></div>
          </div>
          <p className="spinner-message">{message}</p>
        </div>
      </div>
    </div>
  );
}
