import { Link, useNavigate } from "react-router-dom";

import { useAuth } from "../auth";
import Layout from "../components/Layout";
import SearchBar from "../components/SearchBar";
import { SectionHeader } from "../components/ui";
import { useLookups } from "../utils/useLookups";

const STEPS = [
  ["01", "Explore", "Browse activities and filter them by category and location."],
  ["02", "Choose", "Open an activity and view its available sessions."],
  ["03", "Book", "Select participants and booking type, then confirm."],
  ["04", "Manage", "Track your bookings and cancel them when necessary."],
];

export default function Landing() {
  const navigate = useNavigate();
  const { token } = useAuth();
  const { categories } = useLookups();

  return (
    <Layout>
      <section className="hero editorial-hero" aria-labelledby="hero-title">
        <div className="hero-content">
          <h1 id="hero-title">Make room for <span>memorable</span> moments.</h1>
          <p>Discover experiences worth stepping out for.</p>
          <p className="hero-description">Something new to try. Someone to share it with. A story to take home.</p>
          <div className="hero-actions">
            <Link className="primary-button" to="/activities">Explore Experiences <span aria-hidden="true">↗</span></Link>
            <a className="text-link" href="#how-it-works">How it works <span aria-hidden="true">↓</span></a>
          </div>
        </div>

        <div className="hero-visual">
          <div className="adventure-quote">
            <figure className="quote-paper">
              <blockquote><p>“The purpose of life is to live it, to taste experience to the utmost, to reach out eagerly and without fear for newer and richer experience.”</p></blockquote>
              <figcaption>— <strong>Eleanor Roosevelt</strong></figcaption>
            </figure>
          </div>
        </div>
      </section>

      <section className="section discovery-search-section" aria-label="Find an experience">
        <SearchBar onSearch={(query) => navigate(query ? `/activities?q=${encodeURIComponent(query)}` : "/activities")} />
      </section>

      {categories.length > 0 && <section className="section category-section" aria-labelledby="category-title">
        <div className="section-header"><div><span className="eyebrow">FOLLOW YOUR CURIOSITY</span>
          <h2 id="category-title">Find your kind of moment.</h2></div>
          <Link className="text-link" to="/activities">Explore all <span aria-hidden="true">↗</span></Link></div>
        <div className="category-grid">{categories.slice(0, 6).map((category, index) =>
          <Link className="category-card" key={category.category_id} to={`/activities?category=${category.category_id}`}>
            <span className="category-card-symbol" aria-hidden="true">{["✳", "◇", "✧", "○", "✦", "⌁"][index % 6]}</span>
            <span>{category.category_name}</span><span className="category-arrow" aria-hidden="true">↗</span>
          </Link>)}</div>
      </section>}

      <section className="section how-section" id="how-it-works">
        <SectionHeader eyebrow="FROM MAYBE TO MEMORY" title="Make a little plan." text="From discovery to confirmation in four simple steps." />
        <div className="feature-grid">
          {STEPS.map(([number, title, text]) => (
            <div className="feature-card" key={number}>
              <span>{number}</span>
              <h3>{title}</h3>
              <p>{text}</p>
            </div>
          ))}
        </div>
      </section>

      <section className="community-note section">
        <span className="community-symbol" aria-hidden="true">✦</span>
        <div><span className="eyebrow">YOUR NEXT CHAPTER</span><h2>Good moments deserve<br /><em>a place on your calendar.</em></h2>
          <p>Find something that feels like you. Then make it happen.</p></div>
        <Link className="primary-button" to={token ? "/dashboard" : "/register"}>{token ? "Visit your dashboard" : "Find your next moment"} <span aria-hidden="true">↗</span></Link>
      </section>
    </Layout>
  );
}
