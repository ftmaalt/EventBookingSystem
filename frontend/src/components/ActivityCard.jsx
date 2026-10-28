import { Link } from "react-router-dom";
import { formatMoney } from "../utils/format";
import { StatusPill } from "./ui";

export default function ActivityCard({ activity, categoryName, locationName }) {
  return (
    <article className="activity-card">
      <Link className="activity-image" to={`/activities/${activity.activity_id}`} tabIndex={-1} aria-hidden="true">✦</Link>
      <div className="activity-content">
        <div className="activity-labels">
          {categoryName && <span className="category-badge">{categoryName}</span>}
          <StatusPill status={activity.status} />
        </div>
        <h3><Link to={`/activities/${activity.activity_id}`}>{activity.title}</Link></h3>
        {locationName && <p className="activity-location"><span aria-hidden="true">⌖</span> {locationName}</p>}
        <p className="activity-description">{activity.description}</p>
        {activity.durationMinutes != null && <span className="activity-duration">{activity.durationMinutes} min · View sessions for dates & availability</span>}
        <div className="activity-footer">
          <div><strong>{formatMoney(activity.pricePerPerson)}</strong><small>per person</small></div>
          <Link className="experience-link" to={`/activities/${activity.activity_id}`} aria-label={`Explore ${activity.title}`}>Explore <span aria-hidden="true">↗</span></Link>
        </div>
      </div>
    </article>
  );
}
