import { useState, useEffect, useCallback } from 'react';
import Navbar from '../components/Navbar';
import ValidationRuleTable from '../components/ValidationRuleTable';
import LoadingSpinner from '../components/LoadingSpinner';
import Toast from '../components/Toast';
import validationRuleService from '../services/validationRuleService';
import './DashboardPage.css';

export default function DashboardPage() {
  const [rules, setRules] = useState([]);
  const [pendingChanges, setPendingChanges] = useState({});
  const [isLoading, setIsLoading] = useState(true);
  const [isDeploying, setIsDeploying] = useState(false);
  const [toasts, setToasts] = useState([]);
  const [error, setError] = useState(null);

  const addToast = useCallback((message, type = 'success') => {
    const id = Date.now();
    setToasts((prev) => [...prev, { id, message, type }]);
  }, []);

  const removeToast = useCallback((id) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  const fetchRules = useCallback(async () => {
    try {
      setIsLoading(true);
      setError(null);
      const response = await validationRuleService.getAllRules();
      if (response.success) {
        setRules(response.data || []);
      } else {
        setError(response.message || 'Failed to fetch rules');
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to fetch validation rules');
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchRules();
  }, [fetchRules]);

  const handleToggleRule = useCallback((ruleName, newActive) => {
    setPendingChanges((prev) => {
      const originalRule = rules.find((r) => r.fullName === ruleName);
      if (originalRule && originalRule.active === newActive) {
        // Reverting to original state — remove from pending
        const updated = { ...prev };
        delete updated[ruleName];
        return updated;
      }
      return { ...prev, [ruleName]: newActive };
    });
  }, [rules]);

  const handleToggleAll = useCallback((active) => {
    const changes = {};
    rules.forEach((rule) => {
      if (rule.active !== active) {
        changes[rule.fullName] = active;
      }
    });
    setPendingChanges(changes);
  }, [rules]);

  const handleDeploy = useCallback(async () => {
    if (Object.keys(pendingChanges).length === 0) {
      addToast('No changes to deploy', 'info');
      return;
    }

    try {
      setIsDeploying(true);
      // The backend toggle-multiple endpoint expects an array of rule names
      const ruleNames = Object.keys(pendingChanges);

      const response = await validationRuleService.deployChanges(ruleNames);

      if (response.success) {
        // Poll for deployment status
        const deploymentId = response.data?.deploymentId;
        if (deploymentId) {
          addToast('Deployment started — checking status...', 'info');
          await pollDeployStatus(deploymentId);
        } else {
          addToast('Changes deployed successfully!', 'success');
          setPendingChanges({});
          await fetchRules();
        }
      } else {
        addToast(response.message || 'Deployment failed', 'error');
      }
    } catch (err) {
      addToast(err.response?.data?.message || 'Deployment failed', 'error');
    } finally {
      setIsDeploying(false);
    }
  }, [pendingChanges, addToast, fetchRules]);

  const pollDeployStatus = async (deploymentId) => {
    const maxAttempts = 20;
    const interval = 3000;

    for (let i = 0; i < maxAttempts; i++) {
      await new Promise((resolve) => setTimeout(resolve, interval));
      try {
        const statusResponse = await validationRuleService.checkDeployStatus(deploymentId);
        if (statusResponse.success && statusResponse.data) {
          const { status, success: deploySuccess } = statusResponse.data;

          if (status === 'Succeeded' || status === 'Completed') {
            addToast('Changes deployed to Salesforce successfully!', 'success');
            setPendingChanges({});
            await fetchRules();
            return;
          } else if (status === 'Failed' || status === 'Error') {
            addToast(`Deployment failed: ${statusResponse.data.errorMessage || 'Unknown error'}`, 'error');
            return;
          }
          // Still in progress — continue polling
        }
      } catch (err) {
        console.error('Deploy status check failed:', err);
      }
    }
    addToast('Deployment timed out — check Salesforce Setup for status', 'error');
  };

  const changedCount = Object.keys(pendingChanges).length;

  if (isLoading) {
    return (
      <div className="dashboard-page" id="dashboard-page">
        <main className="dashboard-main">
          <Navbar />
          <div className="dashboard-content" style={{ minHeight: '400px', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <LoadingSpinner message="Fetching validation rules from Salesforce..." fullScreen={false} />
          </div>
        </main>
      </div>
    );
  }

  return (
    <div className="dashboard-page" id="dashboard-page">
      <main className="dashboard-main">
        <Navbar />

        <div className="dashboard-content">
          {/* Header */}
        <div className="dashboard-header">
          <div className="dashboard-header-text">
            <h1 className="dashboard-title">Validation Rules</h1>
            <p className="dashboard-subtitle">
              Manage Account validation rules on your Salesforce org
            </p>
          </div>

          <div className="dashboard-actions">
            <button
              className="btn-refresh"
              onClick={fetchRules}
              disabled={isDeploying}
              id="refresh-rules-button"
            >
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <polyline points="23 4 23 10 17 10" />
                <polyline points="1 20 1 14 7 14" />
                <path d="M3.51 9a9 9 0 0114.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0020.49 15" />
              </svg>
              Refresh
            </button>

            <button
              className={`btn-deploy ${changedCount > 0 ? 'btn-deploy-active' : ''}`}
              onClick={handleDeploy}
              disabled={changedCount === 0 || isDeploying}
              id="deploy-button"
            >
              {isDeploying ? (
                <>
                  <div className="btn-deploy-spinner"></div>
                  Deploying...
                </>
              ) : (
                <>
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M22 2L11 13" /><path d="M22 2L15 22L11 13L2 9L22 2Z" />
                  </svg>
                  Deploy {changedCount > 0 ? `(${changedCount})` : ''}
                </>
              )}
            </button>
          </div>
        </div>

        {/* Error */}
        {error && (
          <div className="dashboard-error">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <circle cx="12" cy="12" r="10" /><line x1="15" y1="9" x2="9" y2="15" /><line x1="9" y1="9" x2="15" y2="15" />
            </svg>
            <span>{error}</span>
            <button onClick={fetchRules}>Retry</button>
          </div>
        )}

        {/* Stats */}
        <div className="dashboard-stats">
          <div className="stat-card">
            <span className="stat-number">{rules.length}</span>
            <span className="stat-label">Total Rules</span>
          </div>
          <div className="stat-card stat-card-active">
            <span className="stat-number">{rules.filter((r) => r.active).length}</span>
            <span className="stat-label">Active</span>
          </div>
          <div className="stat-card stat-card-inactive">
            <span className="stat-number">{rules.filter((r) => !r.active).length}</span>
            <span className="stat-label">Inactive</span>
          </div>
          <div className="stat-card stat-card-pending">
            <span className="stat-number">{changedCount}</span>
            <span className="stat-label">Pending Changes</span>
          </div>
        </div>

        {/* Rule Table */}
          <ValidationRuleTable
            rules={rules}
            onToggleRule={handleToggleRule}
            onToggleAll={handleToggleAll}
            pendingChanges={pendingChanges}
            isDeploying={isDeploying}
          />
        </div>
      </main>

      {/* Toast container */}
      <div className="toast-container">
        {toasts.map((toast) => (
          <Toast
            key={toast.id}
            message={toast.message}
            type={toast.type}
            onClose={() => removeToast(toast.id)}
          />
        ))}
      </div>
    </div>
  );
}
