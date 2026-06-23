import './RuleToggleSwitch.css';

export default function RuleToggleSwitch({ isActive, onToggle, disabled = false, ruleId }) {
  return (
    <button
      className={`toggle-switch ${isActive ? 'toggle-active' : 'toggle-inactive'} ${disabled ? 'toggle-disabled' : ''}`}
      onClick={() => !disabled && onToggle(!isActive)}
      disabled={disabled}
      role="switch"
      aria-checked={isActive}
      aria-label={`Toggle rule ${ruleId}`}
      id={`toggle-${ruleId}`}
    >
      <span className="toggle-track">
        <span className="toggle-thumb">
          {isActive ? (
            <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round">
              <polyline points="20 6 9 17 4 12" />
            </svg>
          ) : (
            <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round">
              <line x1="18" y1="6" x2="6" y2="18" />
              <line x1="6" y1="6" x2="18" y2="18" />
            </svg>
          )}
        </span>
      </span>
    </button>
  );
}
