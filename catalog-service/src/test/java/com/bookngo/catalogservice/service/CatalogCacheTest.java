package com.bookngo.catalogservice.service;

import com.bookngo.catalogservice.config.CacheConfig;
import com.bookngo.catalogservice.config.CacheNames;
import com.bookngo.catalogservice.dto.request.ActivityRequest;
import com.bookngo.catalogservice.dto.request.CategoryRequest;
import com.bookngo.catalogservice.dto.request.LocationPinUpdateRequest;
import com.bookngo.catalogservice.dto.request.LocationRequest;
import com.bookngo.catalogservice.dto.response.ActivityResponse;
import com.bookngo.catalogservice.dto.response.CategoryResponse;
import com.bookngo.catalogservice.dto.response.LocationResponse;
import com.bookngo.catalogservice.exception.InformationNotFoundException;
import com.bookngo.catalogservice.model.Activities;
import com.bookngo.catalogservice.model.Category;
import com.bookngo.catalogservice.model.Location;
import com.bookngo.catalogservice.model.enums.ActivityStatus;
import com.bookngo.catalogservice.repository.ActivitiesRepository;
import com.bookngo.catalogservice.repository.CategoryRepository;
import com.bookngo.catalogservice.repository.LocationRepository;
import com.bookngo.catalogservice.security.AuthenticatedUser;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.cache.autoconfigure.metrics.CacheMetricsAutoConfiguration;
import org.springframework.boot.micrometer.metrics.autoconfigure.MetricsAutoConfiguration;
import org.springframework.boot.micrometer.metrics.autoconfigure.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = {CacheConfig.class, CategoryService.class, ActivitiesService.class, LocationService.class})
@ImportAutoConfiguration({MetricsAutoConfiguration.class, SimpleMetricsExportAutoConfiguration.class,
        CacheMetricsAutoConfiguration.class})
class CatalogCacheTest {

    @Autowired
    private CategoryService categoryService;
    @Autowired
    private ActivitiesService activitiesService;
    @Autowired
    private LocationService locationService;
    @Autowired
    private CacheManager cacheManager;
    @Autowired
    private MeterRegistry meterRegistry;

    @MockitoBean
    private CategoryRepository categoryRepository;
    @MockitoBean
    private ActivitiesRepository activitiesRepository;
    @MockitoBean
    private LocationRepository locationRepository;
    @MockitoBean
    private CurrentUserService currentUserService;
    @MockitoBean
    private GeocodingService geocodingService;

    private final AuthenticatedUser provider = new AuthenticatedUser(7L, "provider@test.com", "PROVIDER");
    private final AuthenticatedUser admin = new AuthenticatedUser(1L, "admin@test.com", "ADMIN");

    @BeforeEach
    void clearCaches() {
        CacheNames.ALL.forEach(name -> cacheManager.getCache(name).clear());
    }

    @Test
    @DisplayName("Cache: every configured cache exists with stats enabled")
    void cachesAreConfigured() {
        for (String name : CacheNames.ALL) {
            com.github.benmanes.caffeine.cache.Cache<?, ?> nativeCache =
                    (com.github.benmanes.caffeine.cache.Cache<?, ?>) cacheManager.getCache(name).getNativeCache();
            assertTrue(nativeCache.policy().isRecordingStats(), name);
            assertEquals(500L, nativeCache.policy().eviction().orElseThrow().getMaximum(), name);
        }
    }

    @Test
    @DisplayName("Category list: second read is served from cache")
    void categoryListIsCached() {
        when(categoryRepository.findAll()).thenReturn(List.of(category(1L, "Sports")));

        List<CategoryResponse> first = categoryService.getAllCategories();
        List<CategoryResponse> second = categoryService.getAllCategories();

        assertEquals(first, second);
        verify(categoryRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Category list: creating a category is visible on the next read")
    void categoryCreateEvictsList() {
        Category sports = category(1L, "Sports");
        Category arts = category(2L, "Arts");
        when(categoryRepository.findAll()).thenReturn(List.of(sports)).thenReturn(List.of(sports, arts));
        when(categoryRepository.save(any(Category.class))).thenReturn(arts);
        categoryService.getAllCategories();

        categoryService.createCategory(categoryRequest("Arts"));

        assertEquals(2, categoryService.getAllCategories().size());
        verify(categoryRepository, times(2)).findAll();
    }

    @Test
    @DisplayName("Category by id: update evicts only that id and the next read is fresh")
    void categoryUpdateEvictsOnlyAffectedKey() {
        Category one = category(1L, "Sports");
        Category two = category(2L, "Arts");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(one));
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(two));
        categoryService.getCategoryById(1L);
        categoryService.getCategoryById(2L);
        verify(categoryRepository, times(1)).findById(1L);

        categoryService.updateCategory(1L, categoryRequest("Water Sports"));

        assertEquals("Water Sports", categoryService.getCategoryById(1L).getCategory_name());
        assertEquals("Arts", categoryService.getCategoryById(2L).getCategory_name());
        verify(categoryRepository, times(3)).findById(1L);
        verify(categoryRepository, times(1)).findById(2L);
    }

    @Test
    @DisplayName("Activity by id: second read is served from cache")
    void activityByIdIsCached() {
        when(activitiesRepository.findByIdAndStatus(5L, ActivityStatus.ACTIVE))
                .thenReturn(Optional.of(activity(5L, "Kayaking")));

        ActivityResponse first = activitiesService.getById(5L);
        ActivityResponse second = activitiesService.getById(5L);

        assertSame(first, second);
        verify(activitiesRepository, times(1)).findByIdAndStatus(5L, ActivityStatus.ACTIVE);
    }

    @Test
    @DisplayName("Activity by id: update is visible on the very next read, other ids stay cached")
    void activityUpdateIsVisibleOnNextRead() {
        Activities kayaking = activity(5L, "Kayaking");
        Activities diving = activity(6L, "Diving");
        when(activitiesRepository.findByIdAndStatus(5L, ActivityStatus.ACTIVE)).thenReturn(Optional.of(kayaking));
        when(activitiesRepository.findByIdAndStatus(6L, ActivityStatus.ACTIVE)).thenReturn(Optional.of(diving));
        when(activitiesRepository.findById(5L)).thenReturn(Optional.of(kayaking));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(kayaking.getCategory()));
        when(locationRepository.findById(10L)).thenReturn(Optional.of(kayaking.getLocation()));
        when(activitiesRepository.save(any(Activities.class))).thenAnswer(inv -> inv.getArgument(0));
        when(currentUserService.getCurrentUser()).thenReturn(provider);
        activitiesService.getById(5L);
        activitiesService.getById(6L);

        activitiesService.updateActivity(5L, activityRequest("Sunset Kayaking"));

        assertEquals("Sunset Kayaking", activitiesService.getById(5L).getTitle());
        assertEquals("Diving", activitiesService.getById(6L).getTitle());
        verify(activitiesRepository, times(2)).findByIdAndStatus(5L, ActivityStatus.ACTIVE);
        verify(activitiesRepository, times(1)).findByIdAndStatus(6L, ActivityStatus.ACTIVE);
    }

    @Test
    @DisplayName("Activity by id: delete evicts, so the next read is 404 instead of stale data")
    void activityDeleteEvicts() {
        Activities kayaking = activity(5L, "Kayaking");
        when(activitiesRepository.findByIdAndStatus(5L, ActivityStatus.ACTIVE))
                .thenReturn(Optional.of(kayaking)).thenReturn(Optional.empty());
        when(activitiesRepository.findById(5L)).thenReturn(Optional.of(kayaking));
        when(currentUserService.getCurrentUser()).thenReturn(provider);
        activitiesService.getById(5L);

        activitiesService.deleteActivity(5L);

        assertThrows(InformationNotFoundException.class, () -> activitiesService.getById(5L));
    }

    @Test
    @DisplayName("Activity by id: failed write does not evict")
    void failedWriteDoesNotEvict() {
        Activities kayaking = activity(5L, "Kayaking");
        when(activitiesRepository.findByIdAndStatus(5L, ActivityStatus.ACTIVE)).thenReturn(Optional.of(kayaking));
        when(activitiesRepository.findById(5L)).thenReturn(Optional.of(kayaking));
        when(currentUserService.getCurrentUser()).thenReturn(new AuthenticatedUser(99L, "other@test.com", "PROVIDER"));
        activitiesService.getById(5L);

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> activitiesService.deleteActivity(5L));

        assertNotNull(cacheManager.getCache(CacheNames.ACTIVITY_BY_ID).get(5L));
    }

    @Test
    @DisplayName("Deactivating a category hides its activities on the next read")
    void categoryDeleteEvictsActivities() {
        Activities kayaking = activity(5L, "Kayaking");
        when(activitiesRepository.findByIdAndStatus(5L, ActivityStatus.ACTIVE)).thenReturn(Optional.of(kayaking));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(kayaking.getCategory()));
        when(currentUserService.getCurrentUser()).thenReturn(admin);
        activitiesService.getById(5L);

        categoryService.deleteCategory(1L);

        assertThrows(InformationNotFoundException.class, () -> activitiesService.getById(5L));
        verify(activitiesRepository, times(2)).findByIdAndStatus(5L, ActivityStatus.ACTIVE);
    }

    @Test
    @DisplayName("Deactivating a location hides its activities and refreshes location reads")
    void locationDeleteEvictsLocationAndActivities() {
        Activities kayaking = activity(5L, "Kayaking");
        Location location = kayaking.getLocation();
        when(activitiesRepository.findByIdAndStatus(5L, ActivityStatus.ACTIVE)).thenReturn(Optional.of(kayaking));
        when(locationRepository.findById(10L)).thenReturn(Optional.of(location));
        when(locationRepository.findAll()).thenReturn(List.of(location));
        when(currentUserService.getCurrentUser()).thenReturn(provider);
        activitiesService.getById(5L);
        locationService.getById(10L);
        locationService.getAll();

        locationService.delete(10L);

        assertFalse(locationService.getById(10L).getActive());
        assertFalse(locationService.getAll().get(0).getActive());
        assertThrows(InformationNotFoundException.class, () -> activitiesService.getById(5L));
        verify(locationRepository, times(2)).findAll();
    }

    @Test
    @DisplayName("Location pin change is visible on the very next read")
    void locationPinUpdateIsVisibleOnNextRead() {
        Location location = location(10L);
        when(locationRepository.findById(10L)).thenReturn(Optional.of(location));
        when(locationRepository.save(any(Location.class))).thenAnswer(inv -> inv.getArgument(0));
        when(currentUserService.getCurrentUser()).thenReturn(provider);
        assertEquals(26.0, locationService.getById(10L).getLatitude());

        LocationPinUpdateRequest pin = new LocationPinUpdateRequest();
        pin.setLatitude(26.5);
        pin.setLongitude(50.5);
        locationService.updatePin(10L, pin);

        LocationResponse after = locationService.getById(10L);
        assertEquals(26.5, after.getLatitude());
        assertEquals(50.5, after.getLongitude());
    }

    @Test
    @DisplayName("Location list: creating a location is visible on the next read")
    void locationCreateEvictsList() {
        Location existing = location(10L);
        Location created = location(11L);
        when(locationRepository.findAll()).thenReturn(List.of(existing)).thenReturn(List.of(existing, created));
        when(locationRepository.save(any(Location.class))).thenReturn(created);
        when(currentUserService.getCurrentUser()).thenReturn(provider);
        when(geocodingService.geocodeAddress(any())).thenReturn(new GeocodingResult(26.0, 50.0, "pid"));
        locationService.getAll();

        LocationRequest request = new LocationRequest();
        request.setName("Marina");
        request.setAddress("Road 1");
        request.setCity("Manama");
        locationService.create(request);

        assertEquals(2, locationService.getAll().size());
    }

    @Test
    @DisplayName("Activity search is intentionally not cached")
    void searchIsNotCached() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Activities> page = new PageImpl<>(List.of(activity(5L, "Kayaking")), pageable, 1);
        when(activitiesRepository.search(null, null, pageable)).thenReturn(page);

        activitiesService.search(null, null, pageable);
        activitiesService.search(null, null, pageable);

        verify(activitiesRepository, times(2)).search(null, null, pageable);
    }

    @Test
    @DisplayName("Cache hits and misses are published as Micrometer metrics")
    void cacheMetricsAreRecorded() {
        when(activitiesRepository.findByIdAndStatus(5L, ActivityStatus.ACTIVE))
                .thenReturn(Optional.of(activity(5L, "Kayaking")));
        double hitsBefore = cacheGets("hit");
        double missesBefore = cacheGets("miss");

        activitiesService.getById(5L);
        activitiesService.getById(5L);

        assertEquals(1.0, cacheGets("miss") - missesBefore);
        assertEquals(1.0, cacheGets("hit") - hitsBefore);
    }

    private double cacheGets(String result) {
        return meterRegistry.get("cache.gets")
                .tags("cache", CacheNames.ACTIVITY_BY_ID, "result", result)
                .functionCounter().count();
    }

    private Category category(Long id, String name) {
        Category category = new Category();
        category.setId(id);
        category.setCategoryName(name);
        category.setDescription(name + " activities");
        category.setActive(true);
        return category;
    }

    private Location location(Long id) {
        Location location = new Location();
        location.setId(id);
        location.setName("Harbour");
        location.setAddress("Road 1");
        location.setCity("Manama");
        location.setLatitude(26.0);
        location.setLongitude(50.0);
        location.setActive(true);
        location.setProviderId(provider.id());
        return location;
    }

    private Activities activity(Long id, String title) {
        Activities activity = new Activities();
        activity.setId(id);
        activity.setTitle(title);
        activity.setDescription(title + " trip");
        activity.setPricePerPerson(new BigDecimal("20.00"));
        activity.setDurationMinutes(60);
        activity.setStatus(ActivityStatus.ACTIVE);
        activity.setProviderId(provider.id());
        activity.setCategory(category(1L, "Sports"));
        activity.setLocation(location(10L));
        return activity;
    }

    private CategoryRequest categoryRequest(String name) {
        CategoryRequest request = new CategoryRequest();
        request.setCategory_name(name);
        request.setDescription(name + " activities");
        return request;
    }

    private ActivityRequest activityRequest(String title) {
        ActivityRequest request = new ActivityRequest();
        request.setTitle(title);
        request.setDescription(title + " trip");
        request.setPricePerPerson(new BigDecimal("25.00"));
        request.setDurationMinutes(90);
        request.setCategoryId(1L);
        request.setLocationId(10L);
        return request;
    }
}
