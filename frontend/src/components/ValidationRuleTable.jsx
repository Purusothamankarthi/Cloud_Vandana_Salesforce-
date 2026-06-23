import { useState, useMemo } from 'react';
import RuleToggleSwitch from './RuleToggleSwitch';
import './ValidationRuleTable.css';

export default function ValidationRuleTable({ rules, onToggleRule, onToggleAll, pendingChanges, isDeploying }) {
  const [searchTerm, setSearchTerm] = useState('');
  const [filterStatus, setFilterStatus] = useState('all'); // 'all' | 'active' | 'inactive'
  const [expandedRule, setExpandedRule] = useState(null);

  const filteredRules = useMemo(() => {
    return rules.filter((rule) => {
      const matchesSearch =
        rule.fullName?.toLowerCase().includes(searchTerm.toLowerCase()) ||
        rule.description?.toLowerCase().includes(searchTerm.toLowerCase()) ||
        rule.errorMessage?.toLowerCase().includes(searchTerm.toLowerCase());

      const matchesFilter =
        filterStatus === 'all' ||
        (filterStatus === 'active' && rule.active) ||
        (filterStatus === 'inactive' && !rule.active);

      return matchesSearch && matchesFilter;
    });
  }, [rules, searchTerm, filterStatus]);

  const activeCount = rules.filter((r) => r.active).length;
  const inactiveCount = rules.length - activeCount;
  const changedCount = Object.keys(pendingChanges).length;

  return (
    <div className="rule-table-container">
      {/* Toolbar */}
      <div className="rule-toolbar">
        <div className="rule-search-wrapper">
          <svg className="rule-search-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <circle cx="11" cy="11" r="8" /><line x1="21" y1="21" x2="16.65" y2="16.65" />
          </svg>
          <input
            type="text"
            className="rule-search-input"
            placeholder="Search rules..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            id="search-rules-input"
          />
        </div>

        <div className="rule-filters">
          <button
            className={`rule-filter-btn ${filterStatus === 'all' ? 'filter-active' : ''}`}
            onClick={() => setFilterStatus('all')}
          >
            All <span className="filter-count">{rules.length}</span>
          </button>
          <button
            className={`rule-filter-btn filter-btn-active ${filterStatus === 'active' ? 'filter-active' : ''}`}
            onClick={() => setFilterStatus('active')}
          >
            Active <span className="filter-count">{activeCount}</span>
          </button>
          <button
            className={`rule-filter-btn filter-btn-inactive ${filterStatus === 'inactive' ? 'filter-active' : ''}`}
            onClick={() => setFilterStatus('inactive')}
          >
            Inactive <span className="filter-count">{inactiveCount}</span>
          </button>
        </div>

        {changedCount > 0 && (
          <div className="rule-changes-badge">
            <span className="changes-dot"></span>
            {changedCount} pending {changedCount === 1 ? 'change' : 'changes'}
          </div>
        )}
      </div>

      {/* Select All */}
      {filteredRules.length > 0 && (
        <div className="rule-bulk-actions">
          <button
            className="bulk-action-btn"
            onClick={() => onToggleAll(true)}
            disabled={isDeploying}
            id="enable-all-button"
          >
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <polyline points="20 6 9 17 4 12" />
            </svg>
            Enable All
          </button>
          <button
            className="bulk-action-btn bulk-action-disable"
            onClick={() => onToggleAll(false)}
            disabled={isDeploying}
            id="disable-all-button"
          >
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" />
            </svg>
            Disable All
          </button>
        </div>
      )}

      {/* Table */}
      <div className="rule-table-wrapper">
        {filteredRules.length === 0 ? (
          <div className="rule-empty">
            <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
              <circle cx="12" cy="12" r="10" />
              <line x1="12" y1="8" x2="12" y2="12" />
              <line x1="12" y1="16" x2="12.01" y2="16" />
            </svg>
            <p>No validation rules found</p>
            <span>{searchTerm ? 'Try a different search term' : 'No rules exist on the Account object'}</span>
          </div>
        ) : (
          <table className="rule-table" id="validation-rules-table">
            <thead>
              <tr>
                <th>Rule Name</th>
                <th>Description</th>
                <th>Error Message</th>
                <th>Status</th>
                <th>Toggle</th>
              </tr>
            </thead>
            <tbody>
              {filteredRules.map((rule) => {
                const isPending = pendingChanges[rule.fullName] !== undefined;
                const displayActive = isPending ? pendingChanges[rule.fullName] : rule.active;

                return (
                  <tr
                    key={rule.id || rule.fullName}
                    className={`rule-row ${isPending ? 'rule-row-changed' : ''} ${expandedRule === rule.fullName ? 'rule-row-expanded' : ''}`}
                    onClick={() => setExpandedRule(expandedRule === rule.fullName ? null : rule.fullName)}
                  >
                    <td className="rule-name-cell">
                      <div className="rule-name">
                        <span className="rule-fullname">{rule.fullName}</span>
                        {isPending && <span className="rule-pending-badge">Modified</span>}
                      </div>
                    </td>
                    <td className="rule-desc-cell">
                      <span className="rule-description">{rule.description || '—'}</span>
                    </td>
                    <td className="rule-error-cell">
                      <span className="rule-error-msg">{rule.errorMessage || '—'}</span>
                    </td>
                    <td className="rule-status-cell">
                      <span className={`rule-status-badge ${displayActive ? 'status-active' : 'status-inactive'}`}>
                        {displayActive ? 'Active' : 'Inactive'}
                      </span>
                    </td>
                    <td className="rule-toggle-cell" onClick={(e) => e.stopPropagation()}>
                      <RuleToggleSwitch
                        isActive={displayActive}
                        onToggle={(newState) => onToggleRule(rule.fullName, newState)}
                        disabled={isDeploying}
                        ruleId={rule.fullName}
                      />
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        )}
      </div>

      {/* Expanded detail */}
      {expandedRule && (
        <div className="rule-detail-panel">
          {(() => {
            const rule = rules.find((r) => r.fullName === expandedRule);
            if (!rule) return null;
            return (
              <>
                <h3 className="detail-title">{rule.fullName}</h3>
                <div className="detail-grid">
                  <div className="detail-item">
                    <span className="detail-label">Formula</span>
                    <code className="detail-formula">{rule.formula || 'N/A'}</code>
                  </div>
                  <div className="detail-item">
                    <span className="detail-label">Error Display Field</span>
                    <span className="detail-value">{rule.errorDisplayField || 'Top of Page'}</span>
                  </div>
                  <div className="detail-item">
                    <span className="detail-label">Error Message</span>
                    <span className="detail-value">{rule.errorMessage || 'N/A'}</span>
                  </div>
                  <div className="detail-item">
                    <span className="detail-label">Description</span>
                    <span className="detail-value">{rule.description || 'N/A'}</span>
                  </div>
                </div>
              </>
            );
          })()}
        </div>
      )}
    </div>
  );
}
