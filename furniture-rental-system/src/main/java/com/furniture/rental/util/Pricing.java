package com.furniture.rental.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Single source of truth for the tax rate and how totals are rounded. */
public final class Pricing {

    public static final BigDecimal TAX_RATE = new BigDecimal("0.05");

    private Pricing() {
    }

    public static BigDecimal tax(BigDecimal subtotal) {
        return subtotal.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal grandTotal(BigDecimal subtotal) {
        return subtotal.add(tax(subtotal)).setScale(2, RoundingMode.HALF_UP);
    }

    /** The rate as a percentage string for display, e.g. "5". */
    public static String taxPercentLabel() {
        return TAX_RATE.movePointRight(2).stripTrailingZeros().toPlainString();
    }

    public static final int LATE_FEE_DAYS_PER_MONTH = 30;

    /**
     * Late fee for one item: one day's rent (monthly price / 30) per unit per day
     * late.
     */
    public static BigDecimal lateFee(BigDecimal monthlyPrice, int quantity, long daysLate) {
        if (daysLate <= 0 || quantity <= 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return monthlyPrice
                .multiply(BigDecimal.valueOf(quantity))
                .multiply(BigDecimal.valueOf(daysLate))
                .divide(BigDecimal.valueOf(LATE_FEE_DAYS_PER_MONTH), 2, RoundingMode.HALF_UP);
    }
}