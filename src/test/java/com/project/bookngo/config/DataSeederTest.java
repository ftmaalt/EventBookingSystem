package com.project.bookngo.config;

import com.project.bookngo.model.*;
import com.project.bookngo.model.enums.*;
import com.project.bookngo.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DataSeederTest {
    @Test
    void createsConsistentScenariosAndPreservesUsedDataOnRestart() {
        UsersRepository users = mock(UsersRepository.class);
        CategoryRepository categories = mock(CategoryRepository.class);
        LocationRepository locations = mock(LocationRepository.class);
        ActivitiesRepository activities = mock(ActivitiesRepository.class);
        SessionsRepository sessions = mock(SessionsRepository.class);
        BookingsRepository bookings = mock(BookingsRepository.class);
        ProviderProfileRepository profiles = mock(ProviderProfileRepository.class);
        WaitingListRepository waiting = mock(WaitingListRepository.class);
        AuditLogRepository audits = mock(AuditLogRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        Map<String, User> userStore = new LinkedHashMap<>();
        Map<String, Category> categoryStore = new HashMap<>();
        Map<String, Location> locationStore = new HashMap<>();
        Map<String, Activities> activityStore = new HashMap<>();
        Map<Long, Sessions> sessionStore = new HashMap<>();
        Map<Long, ProviderProfile> profileStore = new HashMap<>();
        Map<String, AuditLog> markers = new HashMap<>();
        List<Bookings> bookingStore = new ArrayList<>();
        List<WaitlistEntry> waitStore = new ArrayList<>();
        AtomicLong ids = new AtomicLong();
        when(encoder.encode(anyString())).thenReturn("encoded-demo-password");
        when(users.findUserByEmail(anyString())).thenAnswer(i -> userStore.get(i.getArgument(0)));
        when(users.save(any())).thenAnswer(i -> {
            User u = i.getArgument(0); u.setId(ids.incrementAndGet()); userStore.put(u.getEmail(), u); return u;
        });
        when(categories.findByCategoryName(anyString())).thenAnswer(i -> categoryStore.get(i.getArgument(0)));
        when(categories.save(any())).thenAnswer(i -> {
            Category c = i.getArgument(0); c.setId(ids.incrementAndGet()); categoryStore.put(c.getCategoryName(), c); return c;
        });
        when(locations.findByProviderIdAndName(anyLong(), anyString())).thenAnswer(i -> Optional.ofNullable(locationStore.get(i.getArgument(0) + ":" + i.getArgument(1))));
        when(locations.save(any())).thenAnswer(i -> {
            Location l = i.getArgument(0); l.setId(ids.incrementAndGet()); locationStore.put(l.getProvider().getId() + ":" + l.getName(), l); return l;
        });
        when(activities.findByProviderIdAndTitle(anyLong(), anyString())).thenAnswer(i -> Optional.ofNullable(activityStore.get(i.getArgument(0) + ":" + i.getArgument(1))));
        when(activities.save(any())).thenAnswer(i -> {
            Activities a = i.getArgument(0); a.setId(ids.incrementAndGet()); activityStore.put(a.getProvider().getId() + ":" + a.getTitle(), a); return a;
        });
        when(profiles.findByUserId(anyLong())).thenAnswer(i -> Optional.ofNullable(profileStore.get(i.getArgument(0))));
        when(profiles.save(any())).thenAnswer(i -> {
            ProviderProfile p = i.getArgument(0); profileStore.put(p.getUser().getId(), p); return p;
        });
        when(sessions.save(any())).thenAnswer(i -> {
            Sessions s = i.getArgument(0); if (s.getId() == null) s.setId(ids.incrementAndGet()); sessionStore.put(s.getId(), s); return s;
        });
        when(sessions.findById(anyLong())).thenAnswer(i -> Optional.ofNullable(sessionStore.get(i.getArgument(0))));
        when(bookings.save(any())).thenAnswer(i -> {
            Bookings b = i.getArgument(0); b.setBookingId(ids.incrementAndGet()); bookingStore.add(b); return b;
        });
        when(bookings.findBySessionId(anyLong())).thenAnswer(i -> bookingStore.stream().filter(b -> b.getSession().getId().equals(i.getArgument(0))).toList());
        when(waiting.saveAndFlush(any())).thenAnswer(i -> {
            WaitlistEntry e = i.getArgument(0); e.setEntryId(ids.incrementAndGet()); waitStore.add(e); return e;
        });
        when(waiting.findBySessionIdAndStatusOrderByEntryIdAsc(anyLong(), any())).thenAnswer(i -> waitStore.stream()
                .filter(e -> e.getSession().getId().equals(i.getArgument(0)) && e.getStatus() == i.getArgument(1)).toList());
        when(audits.findFirstByActionAndDetails(anyString(), anyString())).thenAnswer(i -> Optional.ofNullable(markers.get(i.getArgument(1))));
        when(audits.save(any())).thenAnswer(i -> { AuditLog a = i.getArgument(0); markers.put(a.getDetails(), a); return a; });

        // An original account exists, with its own credentials and status. It must survive unchanged.
        User original = new User(); original.setId(999L); original.setEmail("customer@bookngo.com");
        original.setPasswordHash("existing-hash"); original.setStatus(UserStatus.DEACTIVATED);
        userStore.put(original.getEmail(), original);
        DataSeeder seeder = new DataSeeder(users, categories, locations, activities, sessions, bookings, profiles, waiting, audits, encoder);
        ReflectionTestUtils.setField(seeder, "demoPassword", "test-password");
        seeder.run();
        assertEquals(21, userStore.size());
        assertEquals(5, activityStore.size());
        assertEquals(13, sessionStore.size());
        assertEquals(7, bookingStore.size());
        assertEquals(5, waitStore.size());
        for (Sessions s : sessionStore.values()) {
            assertEquals(2026, s.getStartTime().getYear()); assertEquals(11, s.getStartTime().getMonthValue());
            assertTrue(s.getStartTime().getDayOfMonth() >= 5 && s.getStartTime().getDayOfMonth() <= 25);
            assertTrue(s.getEndTime().isAfter(s.getStartTime()));
            int reserved = bookingStore.stream().filter(b -> b.getSession() == s).mapToInt(Bookings::getParticipants).sum();
            assertEquals(s.getCapacity() - reserved, s.getSpotsLeft());
            assertEquals(s.getSpotsLeft() == 0 ? SessionStatus.FULL : SessionStatus.SCHEDULED, s.getStatus());
        }
        for (Bookings b : bookingStore) {
            assertEquals(b.getSubtotal(), b.getSession().getActivity().getPricePerPerson().multiply(java.math.BigDecimal.valueOf(b.getParticipants())));
            assertEquals(b.getSubtotal(), b.getTotalPrice()); assertNotNull(b.getCreatedBy()); assertNotNull(b.getUpdatedBy());
            assertEquals(BookingStatus.CONFIRMED, b.getStatus()); assertEquals(PaymentStatus.PENDING_PAYMENT, b.getPaymentStatus());
            assertEquals(UserStatus.ACTIVE, b.getUser().getStatus());
        }
        for (String key : List.of("JMETER_RACE_01", "JMETER_RACE_02", "JMETER_RACE_03")) {
            Sessions s = sessionStore.get(markers.get(key).getTargetId());
            assertEquals(1, s.getCapacity()); assertEquals(1, s.getSpotsLeft());
            assertTrue(bookingStore.stream().noneMatch(b -> b.getSession() == s));
            assertTrue(waitStore.stream().noneMatch(e -> e.getSession() == s));
        }
        Sessions fifo = sessionStore.get(markers.get("FIFO_WAITLIST").getTargetId());
        assertEquals(List.of("waitlist1@bookngo.com", "waitlist2@bookngo.com", "waitlist3@bookngo.com"),
                waitStore.stream().filter(e -> e.getSession() == fifo).sorted(Comparator.comparing(WaitlistEntry::getEntryId)).map(e -> e.getUser().getEmail()).toList());
        // Simulate a used race, cancelled booking, rescheduled scenario, and dragged map pin.
        Sessions race = sessionStore.get(markers.get("JMETER_RACE_01").getTargetId());
        race.setSpotsLeft(0); race.setStatus(SessionStatus.FULL);
        fifo.setStartTime(fifo.getStartTime().plusDays(1));
        bookingStore.get(0).setStatus(BookingStatus.CANCELLED);
        Location moved = locationStore.values().iterator().next(); moved.setLatitude(26.123456);
        clearInvocations(users, categories, locations, activities, sessions, bookings, profiles, waiting, audits, encoder);
        seeder.run();
        for (Object repository : List.of(users, categories, locations, activities, sessions, bookings, profiles, waiting, audits)) {
            assertTrue(mockingDetails(repository).getInvocations().stream().noneMatch(i -> i.getMethod().getName().startsWith("save")));
        }
        assertEquals(0, race.getSpotsLeft()); assertEquals(BookingStatus.CANCELLED, bookingStore.get(0).getStatus());
        assertEquals(26.123456, moved.getLatitude()); assertEquals("existing-hash", original.getPasswordHash());
        assertEquals(UserStatus.DEACTIVATED, original.getStatus()); verifyNoInteractions(encoder);
    }
}
