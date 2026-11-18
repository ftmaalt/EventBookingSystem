#!/usr/bin/env node
// Seeds demo catalog and booking data through the gateway, using the accounts created by the auth-service seeder.
// Usage: SEED_PASSWORD=... [API_URL=http://localhost:8080] node scripts/seed-demo-data.mjs

const API_URL = (process.env.API_URL || "http://localhost:8080").replace(/\/+$/, "");
const SEED_PASSWORD = process.env.SEED_PASSWORD;

const DAY = 24 * 60 * 60 * 1000;
const summary = { locationsCreated: 0, locationsReused: 0, activitiesCreated: 0, activitiesSkipped: 0,
  sessionsCreated: 0, bookingsCreated: 0, waitlistEntriesCreated: 0 };

const LOCATIONS = [
  { key: "amwaj", provider: "provider1", name: "Amwaj Lagoon Launch Point", address: "Amwaj Islands", city: "Muharraq, Bahrain" },
  { key: "bay", provider: "provider1", name: "Bahrain Bay Watersports Dock", address: "Bahrain Bay", city: "Manama, Bahrain" },
  { key: "adliya", provider: "provider2", name: "Manama Clay Studio Adliya", address: "Adliya", city: "Manama, Bahrain" },
];

const ACTIVITIES = [
  { key: "sunsetKayak", provider: "provider1", location: "amwaj", category: "Outdoor Adventures",
    title: "Sunset Kayak Tour - Amwaj Lagoon", price: 18, minutes: 90,
    description: "A guided kayak tour through the Amwaj lagoons at sunset. Kayaks and life jackets included." },
  { key: "paddleboard", provider: "provider1", location: "amwaj", category: "Fitness & Wellness",
    title: "Beginner Stand-Up Paddleboarding", price: 15, minutes: 60,
    description: "Learn balance, paddling and safety basics on calm water with a certified instructor." },
  { key: "nightKayak", provider: "provider1", location: "bay", category: "Outdoor Adventures",
    title: "Bahrain Bay Night Kayak", price: 22, minutes: 90,
    description: "Paddle past the Manama skyline at night on illuminated kayaks." },
  { key: "wheel", provider: "provider2", location: "adliya", category: "Creative Workshops",
    title: "Wheel-Throwing Pottery Workshop", price: 25, minutes: 120,
    description: "Throw your first bowls on the potter's wheel. Pieces are glazed and ready to collect a week later." },
  { key: "family", provider: "provider2", location: "adliya", category: "Creative Workshops",
    title: "Family Clay Hand-Building", price: 12, minutes: 75,
    description: "A relaxed hand-building class for parents and children aged 6 and up." },
  { key: "karak", provider: "provider2", location: "adliya", category: "Food & Cooking",
    title: "Pottery and Karak Evening", price: 20, minutes: 120,
    description: "Decorate a karak cup in clay, then enjoy fresh karak tea and Bahraini sweets." },
];

const SESSIONS = {
  sunsetKayak: [{ day: 3, hour: 15, capacity: 2 }, { day: 6, hour: 15, capacity: 8 }, { day: 10, hour: 15, capacity: 8 }],
  paddleboard: [{ day: 4, hour: 7, capacity: 6 }, { day: 11, hour: 7, capacity: 6 }],
  nightKayak: [{ day: 5, hour: 18, capacity: 10 }, { day: 12, hour: 18, capacity: 10 }],
  wheel: [{ day: 4, hour: 16, capacity: 6 }, { day: 9, hour: 16, capacity: 6 }, { day: 16, hour: 16, capacity: 6 }],
  family: [{ day: 7, hour: 10, capacity: 10 }, { day: 14, hour: 10, capacity: 10 }],
  karak: [{ day: 8, hour: 17, capacity: 8 }, { day: 15, hour: 17, capacity: 8 }],
};

const ACCOUNTS = ["provider1", "provider2", "customer1", "customer2", "customer3"];

function fail(message) {
  console.error(`Error: ${message}`);
  process.exit(1);
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

async function request(method, path, { token, body } = {}) {
  const url = `${API_URL}${path}`;
  const headers = { Accept: "application/json" };
  if (token) headers.Authorization = `Bearer ${token}`;
  if (body !== undefined) headers["Content-Type"] = "application/json";
  let response;
  try {
    response = await fetch(url, { method, headers, body: body === undefined ? undefined : JSON.stringify(body) });
  } catch (error) {
    throw new Error(`${method} ${url} failed: ${error.cause?.message || error.message}. Is the gateway running?`);
  }
  const text = await response.text();
  if (!response.ok) {
    const error = new Error(`${method} ${url} -> ${response.status}: ${text}`);
    error.status = response.status;
    throw error;
  }
  return text ? JSON.parse(text) : null;
}

// Login is limited to 5 attempts per minute per client IP, so a quick re-run waits for the window to pass.
async function login(account) {
  const email = `${account}@bookngo.demo`;
  for (let attempt = 1; ; attempt++) {
    try {
      const { token } = await request("POST", "/api/auth/login", { body: { email, password: SEED_PASSWORD } });
      const me = await request("GET", "/api/users/me", { token });
      return { token, id: me.id };
    } catch (error) {
      if (error.status === 429 && attempt <= 6) {
        console.log(`Login rate limit reached, waiting 15 s before retrying ${email}...`);
        await sleep(15_000);
        continue;
      }
      if (error.status === 401) {
        throw new Error(`Login failed for ${email}. Start auth-service once with SEED_ENABLED=true and the same SEED_PASSWORD. (${error.message})`);
      }
      throw error;
    }
  }
}

async function getAllActivities() {
  const activities = [];
  for (let page = 0; ; page++) {
    const result = await request("GET", `/api/activities?page=${page}&size=100`);
    activities.push(...result.content);
    if (page + 1 >= result.totalPages) return activities;
  }
}

function startTime(day, hour) {
  const date = new Date(Date.now() + day * DAY);
  date.setUTCHours(hour, 0, 0, 0);
  return date;
}

function localDateTime(date) {
  return date.toISOString().slice(0, 19);
}

async function ensureLocations(users) {
  const existing = await request("GET", "/api/locations");
  const ids = {};
  for (const location of LOCATIONS) {
    const owner = users[location.provider];
    const match = existing.find((l) => l.name === location.name && l.providerId === owner.id);
    if (match) {
      ids[location.key] = match.id;
      summary.locationsReused++;
      continue;
    }
    try {
      const created = await request("POST", "/api/locations", { token: owner.token,
        body: { name: location.name, address: location.address, city: location.city } });
      ids[location.key] = created.id;
      summary.locationsCreated++;
    } catch (error) {
      if (error.status >= 500) {
        throw new Error(`Could not create location "${location.name}". Creating a location geocodes its address, `
          + `so catalog-service needs a valid GOOGLE_MAPS_API_KEY. (${error.message})`);
      }
      throw error;
    }
  }
  return ids;
}

async function createActivities(users, locationIds) {
  const categories = await request("GET", "/api/categories");
  const categoryId = (name) => {
    const category = categories.find((c) => c.category_name === name);
    if (!category) fail(`Category "${name}" not found. Is the catalog-service category migration applied?`);
    return category.category_id;
  };
  const existingTitles = new Set((await getAllActivities()).map((a) => a.title));
  const created = {};
  for (const activity of ACTIVITIES) {
    if (existingTitles.has(activity.title)) {
      summary.activitiesSkipped++;
      continue;
    }
    const response = await request("POST", "/api/activities", { token: users[activity.provider].token, body: {
      title: activity.title, description: activity.description, pricePerPerson: activity.price,
      durationMinutes: activity.minutes, categoryId: categoryId(activity.category), locationId: locationIds[activity.location],
    } });
    created[activity.key] = { id: response.activity_id, provider: activity.provider, minutes: activity.minutes };
    summary.activitiesCreated++;
  }
  return created;
}

async function createSessions(users, activities) {
  const sessions = {};
  for (const [key, activity] of Object.entries(activities)) {
    sessions[key] = [];
    for (const slot of SESSIONS[key]) {
      const start = startTime(slot.day, slot.hour);
      const end = new Date(start.getTime() + activity.minutes * 60 * 1000);
      const session = await request("POST", "/api/sessions", { token: users[activity.provider].token, body: {
        activity_id: activity.id, startTime: localDateTime(start), endTime: localDateTime(end), capacity: slot.capacity,
      } });
      sessions[key].push(session.id);
      summary.sessionsCreated++;
    }
  }
  return sessions;
}

async function book(user, sessionId, participants = 1) {
  await request("POST", "/api/bookings", { token: user.token,
    body: { sessionId, participants, bookingType: participants > 1 ? "GROUP" : "INDIVIDUAL" } });
  summary.bookingsCreated++;
}

async function createBookings(users, sessions) {
  if (sessions.sunsetKayak) {
    const fullSession = sessions.sunsetKayak[0];
    await book(users.customer1, fullSession);
    await book(users.customer2, fullSession);
    const session = await request("GET", `/api/sessions/${fullSession}`);
    if (session.status !== "FULL") fail(`Session ${fullSession} should be FULL but is ${session.status}.`);
    await request("POST", `/api/sessions/${fullSession}/waitlist`, { token: users.customer3.token });
    summary.waitlistEntriesCreated++;
    await book(users.customer1, sessions.sunsetKayak[1], 2);
  }
  if (sessions.paddleboard) await book(users.customer3, sessions.paddleboard[0]);
  if (sessions.nightKayak) await book(users.customer2, sessions.nightKayak[0], 3);
  if (sessions.wheel) {
    await book(users.customer1, sessions.wheel[0]);
    await book(users.customer3, sessions.wheel[1], 2);
  }
  if (sessions.family) await book(users.customer2, sessions.family[0], 4);
  if (sessions.karak) {
    await book(users.customer1, sessions.karak[0]);
    await book(users.customer3, sessions.karak[1]);
  }
}

async function main() {
  if (!SEED_PASSWORD) fail("SEED_PASSWORD is not set. Use the same value auth-service was seeded with.");
  console.log(`Seeding demo data through ${API_URL}`);

  const users = {};
  for (const account of ACCOUNTS) users[account] = await login(account);

  const locationIds = await ensureLocations(users);
  const activities = await createActivities(users, locationIds);
  const sessions = await createSessions(users, activities);
  await createBookings(users, sessions);

  console.log("Done.");
  console.table(summary);
}

main().catch((error) => {
  console.error(`Seeding failed: ${error.message}`);
  process.exit(1);
});
