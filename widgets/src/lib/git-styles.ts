import { css } from 'lit';

export const themeVars = css`
  :host {
    --git-color-text: #333;
    --git-color-text-secondary: #666;
    --git-color-text-muted: #999;
    --git-color-label: #555;
    --git-color-accent: #1565c0;
    --git-color-accent-light: #1976d2;
    --git-color-success: #2e7d32;
    --git-color-error: #c62828;
    --git-color-warning: #e65100;
    --git-color-bg: #fff;
    --git-color-bg-hover: #f5f5f5;
    --git-color-bg-selected: #e3f2fd;
    --git-color-bg-stripe: #fafafa;
    --git-color-bg-success: #e8f5e9;
    --git-color-bg-error: #ffebee;
    --git-color-bg-warning: #fff3e0;
    --git-color-border: #e0e0e0;
    --git-color-border-strong: #ccc;
    --git-color-border-light: #f0f0f0;
    --git-border-radius-sm: 4px;
    --git-border-radius-md: 6px;
    --git-border-radius-lg: 8px;
  }
`;

export const cardStyles = css`
  .card {
    border: 1px solid var(--git-color-border, #e0e0e0);
    border-radius: var(--git-border-radius-lg, 8px);
    padding: var(--git-padding-lg, 24px);
  }
`;

export const formStyles = css`
  .form-grid label {
    display: block;
    margin-bottom: 4px;
    color: var(--git-color-label, #555);
    font-size: 13px;
  }
  .form-grid input,
  .form-grid select,
  .form-grid textarea {
    width: 100%;
    padding: 7px 10px;
    border: 1px solid var(--git-color-border-strong, #ccc);
    border-radius: var(--git-border-radius-sm, 4px);
    font-size: 13px;
    font-family: inherit;
    box-sizing: border-box;
  }
  .form-grid input:focus,
  .form-grid select:focus,
  .form-grid textarea:focus {
    border-color: var(--git-color-accent-light, #1976d2);
    outline: none;
  }
`;

export const statusStyles = css`
  .result { margin-top: 12px; padding: 12px; border-radius: var(--git-border-radius-sm, 4px); }
  .error {
    background: var(--git-color-bg-error, #ffebee);
    color: var(--git-color-error, #c62828);
  }
  .success {
    background: var(--git-color-bg-success, #e8f5e9);
    color: var(--git-color-success, #2e7d32);
  }
  .info {
    background: var(--git-color-bg-selected, #e3f2fd);
    color: var(--git-color-accent, #1565c0);
  }
  .warning {
    background: var(--git-color-bg-warning, #fff3e0);
    border: 1px solid var(--git-color-accent-light, #1976d2);
  }
`;

export const listStyles = css`
  .list {
    border: 1px solid var(--git-color-border, #e0e0e0);
    border-radius: var(--git-border-radius-sm, 4px);
  }
  .row {
    padding: 8px 12px;
    border-bottom: 1px solid var(--git-color-border-light, #f0f0f0);
    display: flex;
    align-items: center;
  }
  .row:last-child { border-bottom: none; }
  .row:hover { background: var(--git-color-bg-hover, #f5f5f5); }
  .row.selected { background: var(--git-color-bg-selected, #e3f2fd); }
`;

export const sectionTitleStyles = css`
  .section-title {
    font-size: 16px;
    font-weight: 600;
    color: var(--git-color-text, #333);
    border-bottom: 1px solid var(--git-color-border, #e0e0e0);
    padding-bottom: 8px;
    margin: 0 0 12px 0;
  }
`;

export const emptyStateStyles = css`
  .empty-state {
    text-align: center;
    padding: 24px;
    color: var(--git-color-text-muted, #999);
  }
`;
