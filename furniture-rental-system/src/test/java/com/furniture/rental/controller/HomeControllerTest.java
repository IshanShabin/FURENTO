package com.furniture.rental.controller;

import com.furniture.rental.dto.FurnitureFilter;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HomeControllerTest {

    @Test
    void pageBaseUrlHasNoFiltersWhenNothingIsSet() {
        FurnitureFilter filter = new FurnitureFilter();
        filter.setQ("   ");
        filter.setSort("");

        assertEquals("/furniture?", HomeController.pageBaseUrl(filter));
    }

    @Test
    void pageBaseUrlKeepsActiveFiltersAndEncodesText() {
        FurnitureFilter filter = new FurnitureFilter();
        filter.setQ("oak & pine");
        filter.setCategory(2L);
        filter.setMinPrice(new BigDecimal("100"));
        filter.setMaxPrice(new BigDecimal("999.50"));
        filter.setAvailableOnly(true);
        filter.setSort("priceAsc");

        assertEquals(
                "/furniture?q=oak+%26+pine&category=2&minPrice=100&maxPrice=999.50&availableOnly=true&sort=priceAsc&",
                HomeController.pageBaseUrl(filter));
    }

    @Test
    void falseAvailabilityIsLeftOut() {
        FurnitureFilter filter = new FurnitureFilter();
        filter.setAvailableOnly(false);

        assertEquals("/furniture?", HomeController.pageBaseUrl(filter));
    }
}
