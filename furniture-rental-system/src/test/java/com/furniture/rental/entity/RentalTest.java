package com.furniture.rental.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RentalTest {

    private RentalItem itemEndingOn(LocalDate endDate) {
        RentalItem item = new RentalItem();
        item.setEndDate(endDate);
        return item;
    }

    private Rental rental(String status, RentalItem... items) {
        Rental rental = new Rental();
        rental.setStatus(status);
        rental.setItems(List.of(items));
        return rental;
    }

    @Test
    void dueDateIsTheLatestEndDate() {
        LocalDate soon = LocalDate.now().plusDays(5);
        LocalDate later = LocalDate.now().plusDays(40);
        Rental rental = rental("ACTIVE", itemEndingOn(soon), itemEndingOn(later));

        assertEquals(later, rental.getDueDate());
    }

    @Test
    void dueDateIsNullWithoutDates() {
        assertNull(rental("ACTIVE", itemEndingOn(null)).getDueDate());
        assertNull(new Rental().getDueDate());
    }

    @Test
    void activeRentalPastItsDueDateIsOverdue() {
        Rental rental = rental("ACTIVE", itemEndingOn(LocalDate.now().minusDays(1)));
        assertTrue(rental.isOverdue());
    }

    @Test
    void rentalDueTodayIsNotYetOverdue() {
        Rental rental = rental("ACTIVE", itemEndingOn(LocalDate.now()));
        assertFalse(rental.isOverdue());
    }

    @Test
    void rentalIsNotOverdueWhileAnyItemPeriodIsStillRunning() {
        Rental rental = rental("ACTIVE",
                itemEndingOn(LocalDate.now().minusDays(10)),
                itemEndingOn(LocalDate.now().plusDays(20)));
        assertFalse(rental.isOverdue());
    }

    @Test
    void onlyActiveRentalsCanBeOverdue() {
        for (String status : List.of("PENDING", "RETURNED", "CANCELLED")) {
            assertFalse(rental(status, itemEndingOn(LocalDate.now().minusDays(30))).isOverdue());
        }
    }

    @Test
    void rentalWithoutDatesIsNotOverdue() {
        assertFalse(rental("ACTIVE", itemEndingOn(null)).isOverdue());
    }

    @Test
    void unpaidPenaltyTotalSumsOnlyUnpaidFees() {
        Rental rental = new Rental();
        rental.setPenalties(new ArrayList<>(List.of(
                penalty("59.90", false), penalty("20.00", false), penalty("100.00", true))));

        assertEquals(new BigDecimal("79.90"), rental.getUnpaidPenaltyTotal());
        assertTrue(rental.isPenaltyDue());
    }

    @Test
    void noPenaltyDueWhenAllPaidOrNone() {
        assertFalse(new Rental().isPenaltyDue());

        Rental paid = new Rental();
        paid.setPenalties(new ArrayList<>(List.of(penalty("59.90", true))));
        assertFalse(paid.isPenaltyDue());
    }

    private Penalty penalty(String amount, boolean paid) {
        Penalty penalty = new Penalty();
        penalty.setAmount(new BigDecimal(amount));
        penalty.setPaid(paid);
        return penalty;
    }
}