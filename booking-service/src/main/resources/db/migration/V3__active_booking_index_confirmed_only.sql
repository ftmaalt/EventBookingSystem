DROP INDEX ux_bookings_active;

CREATE UNIQUE INDEX ux_bookings_active ON bookings(user_id, session_id) WHERE status = 'CONFIRMED';
