package com.furniture.rental.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PricingTest {

    @Test
    void taxIsFivePercentRoundedToPaise() {
        assertEquals(new BigDecimal("29.95"), Pricing.tax(new BigDecimal("599.00")));
        assertEquals(new BigDecimal("0.03"), Pricing.tax(new BigDecimal("0.50")));
    }

    @Test
    void grandTotalAddsTax() {
        assertEquals(new BigDecimal("628.95"), Pricing.grandTotal(new BigDecimal("599.00")));
        assertEquals(new BigDecimal("1099.35"), Pricing.grandTotal(new BigDecimal("1047")));
    }

    @Test
    void taxPercentLabelMatchesTheRate() {
        assertEquals("5", Pricing.taxPercentLabel());
    }

    @Test
    void lateFeeIsOneDaysRentPerUnitPerDayLate() {
        assertEquals(new BigDecimal("59.90"), Pricing.lateFee(new BigDecimal("599"), 1, 3));
        assertEquals(new BigDecimal("116.33"), Pricing.lateFee(new BigDecimal("349"), 2, 5));
    }

    @Test
    void noLateFeeWhenNotLate() {
        assertEquals(new BigDecimal("0.00"), Pricing.lateFee(new BigDecimal("599"), 1, 0));
        assertEquals(new BigDecimal("0.00"), Pricing.lateFee(new BigDecimal("599"), 1, -2));
    }
}