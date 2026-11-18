package com.bookngo.catalogservice.config;

import java.util.List;

public final class CacheNames {

    public static final String CATEGORIES = "categories";
    public static final String CATEGORY_BY_ID = "categoryById";
    public static final String ACTIVITY_BY_ID = "activityById";
    public static final String LOCATIONS = "locations";
    public static final String LOCATION_BY_ID = "locationById";

    public static final List<String> ALL = List.of(
            CATEGORIES, CATEGORY_BY_ID, ACTIVITY_BY_ID, LOCATIONS, LOCATION_BY_ID);

    private CacheNames() {
    }
}
