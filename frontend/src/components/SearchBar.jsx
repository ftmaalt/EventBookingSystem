import { useId, useState } from "react";

/** Navigation and homepage searches use the existing activity discovery route. */
export default function SearchBar({ onSearch, compact = false }) {
  const id = useId();
  const [value, setValue] = useState("");
  return (
    <form autoComplete="off" className={`experience-search${compact ? " compact" : ""}`} role="search"
      onSubmit={(event) => { event.preventDefault(); onSearch(value.trim()); }}>
      <label className="sr-only" htmlFor={id}>Search experiences</label>
      <span aria-hidden="true" className="search-symbol">⌕</span>
      <input id={id} type="search" autoComplete="off" placeholder={compact ? "Search experiences" : "What would you love to try?"}
        value={value} onChange={(event) => setValue(event.target.value)} />
      <button type="submit" className={compact ? "search-submit" : "primary-button"} aria-label="Search experiences">
        {compact ? "→" : "Find an experience"}
      </button>
    </form>
  );
}
