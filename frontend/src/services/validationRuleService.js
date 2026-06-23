import api from './axiosConfig';

export const validationRuleService = {
  // Get all validation rules for the Account object
  getAllRules: async () => {
    const response = await api.get('/api/validation-rules');
    return response.data;
  },

  // Toggle a single validation rule's active status
  toggleRule: async (ruleName, active) => {
    const response = await api.patch(`/api/validation-rules/${ruleName}/toggle`, null, {
      params: { active },
    });
    return response.data;
  },

  // Toggle multiple validation rules at once
  toggleMultipleRules: async (ruleUpdates) => {
    const response = await api.patch('/api/validation-rules/toggle-multiple', ruleUpdates);
    return response.data;
  },

  // Deploy pending changes to Salesforce
  deployChanges: async (ruleNames) => {
    const response = await api.patch('/api/validation-rules/toggle-multiple', ruleNames);
    return response.data;
  },

  // Check the status of a deployment
  checkDeployStatus: async (deploymentId) => {
    const response = await api.get(`/api/validation-rules/${deploymentId}/status`);
    return response.data;
  },
};

export default validationRuleService;
