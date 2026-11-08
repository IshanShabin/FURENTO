package com.furniture.rental.dto;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class CartTest {

    @Test
    void addItemShouldMergeSameProductAndRentalOptions() {
        Cart cart = new Cart();
        CartItem first = new CartItem(10L, "Chair", new BigDecimal("200"), 1, 1);
        CartItem second = new CartItem(10L, "Chair", new BigDecimal("200"), 2, 1);

        cart.addItem(first);
        cart.addItem(second);

        assertEquals(1, cart.getItems().size());
        assertEquals(3, cart.getItems().get(0).getQuantity());
    }

    @Test
    void addItemShouldRejectQuantityAboveStock() {
        Cart cart = new Cart();
        CartItem item = new CartItem(12L, "Desk", new BigDecimal("500"), 4, 1);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> cart.addItem(item, 3));

        assertTrue(ex.getMessage().contains("available stock"));
    }

    @Test
    void addItemShouldCountStockAcrossDifferentRentalDurations() {
        Cart cart = new Cart();
        cart.addItem(new CartItem(15L, "Table", new BigDecimal("300"), 2, 1), 3);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> cart.addItem(new CartItem(15L, "Table", new BigDecimal("300"), 2, 3), 3));

        assertTrue(ex.getMessage().contains("available stock"));
        assertEquals(1, cart.getItems().size());
        assertEquals(2, cart.getItems().get(0).getQuantity());
    }
}
