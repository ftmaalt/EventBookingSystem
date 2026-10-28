import { labelize } from "../utils/format";

export function Loading() {
  return (
    <div className="loading-page" role="status" aria-live="polite">
      <div className="spinner" aria-hidden="true" />
      <p>Loading...</p>
    </div>
  );
}

export function SectionHeader({ title, text, eyebrow, action }) {
  return (
    <div className="section-header">
      <div>
        {eyebrow && <span className="eyebrow">{eyebrow}</span>}
        <h2>{title}</h2>
        {text && <p>{text}</p>}
      </div>
      {action}
    </div>
  );
}

export function EmptyState({ title, text }) {
  return (
    <div className="empty-state">
      <div aria-hidden="true">✦</div>
      <h3>{title}</h3>
      <p>{text}</p>
    </div>
  );
}

/** Text-like input. Extra props (placeholder, maxLength, min, ...) go to the <input>. */
export function Input({ label, type = "text", value, onChange, hint, ...rest }) {
  return (
    <label className="field">
      <span>{label}</span>
      <input type={type} value={value ?? ""} onChange={(event) => onChange(event.target.value)} {...rest} />
      {hint && <small>{hint}</small>}
    </label>
  );
}

export function TextArea({ label, value, onChange, ...rest }) {
  return (
    <label className="field">
      <span>{label}</span>
      <textarea value={value ?? ""} onChange={(event) => onChange(event.target.value)} {...rest} />
    </label>
  );
}

/** options is an array of [value, label] pairs. */
export function Select({ label, value, onChange, options, placeholder = "Select..." }) {
  return (
    <label className="field">
      <span>{label}</span>
      <select value={value ?? ""} onChange={(event) => onChange(event.target.value)}>
        <option value="">{placeholder}</option>
        {options.map(([optionValue, optionLabel]) => (
          <option key={optionValue} value={optionValue}>
            {optionLabel}
          </option>
        ))}
      </select>
    </label>
  );
}

export function StatusPill({ status }) {
  return <span className={`status-pill ${String(status).toLowerCase()}`}>{labelize(status)}</span>;
}

/** Shows an error and/or success message when set. */
export function Feedback({ error, message }) {
  return (
    <>
      {error && <div className="error-box" role="alert">{error}</div>}
      {message && <div className="success-box" role="status">{message}</div>}
    </>
  );
}

export function ActivitySkeletons({ count = 3 }) {
  return <div role="status" aria-label="Loading experiences" className="activity-grid skeleton-grid">
    {Array.from({ length: count }, (_, index) => <div className="activity-card skeleton-card" key={index} aria-hidden="true">
      <div className="skeleton-block skeleton-cover" /><div className="activity-content">
        <div className="skeleton-block" /><div className="skeleton-block" /><div className="skeleton-block short" />
      </div>
    </div>)}
  </div>;
}

export function Pagination({ page, totalPages, onChange }) {
  if (totalPages <= 1) {
    return null;
  }
  return (
    <div className="pagination">
      <button type="button" disabled={page === 0} onClick={() => onChange(page - 1)}>
        ← Previous
      </button>
      <span>
        Page {page + 1} of {totalPages}
      </span>
      <button type="button" disabled={page >= totalPages - 1} onClick={() => onChange(page + 1)}>
        Next →
      </button>
    </div>
  );
}
