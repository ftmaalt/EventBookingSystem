import { Link } from "react-router-dom";

import Layout from "../components/Layout";

export default function NotFound() {
  return (
    <Layout>
      <div className="loading-page">
        <h1>404</h1>
        <p>Page not found.</p>
        <Link className="primary-button" to="/">
          Go Home
        </Link>
      </div>
    </Layout>
  );
}
