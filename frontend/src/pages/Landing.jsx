import { Link } from "react-router-dom";

import Layout from "../components/Layout";
import { SectionHeader } from "../components/ui";

const STEPS = [
  ["01", "Explore", "Browse activities and filter them by category and location."],
  ["02", "Choose", "Open an activity and view its available sessions."],
  ["03", "Book", "Select participants and booking type, then confirm."],
  ["04", "Manage", "Track your bookings and cancel them when necessary."],
];

export default function Landing() {
  return (
    <Layout>
      <section className="hero">
        <div className="hero-content">
          <span className="eyebrow">BOOK · EXPLORE · EXPERIENCE</span>
          <h1>
            Find your next
            <span> experience.</span>
          </h1>
          <p>Discover activities, explore available sessions and reserve your spot through one simple platform.</p>
          <div className="hero-actions">
            <Link className="primary-button" to="/register">
              Get Started
            </Link>
            <Link className="secondary-button" to="/activities">
              Browse activities
            </Link>
          </div>
        </div>

        <div className="hero-card">
          <div className="hero-card-icon">✦</div>
          <h3>Everything in one place</h3>
          <p>Activities, sessions, bookings and real-time notifications, all in one account.</p>
        </div>
      </section>

      <section className="section">
        <SectionHeader title="How it works" text="From discovery to confirmation in four steps." />
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
    </Layout>
  );
}
