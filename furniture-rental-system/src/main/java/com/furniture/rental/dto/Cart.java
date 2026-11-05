package com.furniture.rental.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class Cart {
    private List<CartItem> items = new ArrayList<>();

    private BigDecimal totalPrice = BigDecimal.ZERO;

    public List<CartItem> getItems() {
        return items;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public void setItems(List<CartItem> items) {
        this.items = items;
    }

    public void addItem(CartItem item) {
        addItem(item, Integer.MAX_VALUE);
    }

    public void addItem(CartItem item, int availableStock) {
        if (item == null) {
            throw new IllegalArgumentException("Cart item cannot be null");
        }
        if (item.getQuantity() <= 0) {
            throw new IllegalArgumentException("Cart quantity must be greater than zero");
        }

        int existingQuantity = items.stream()
                .filter(existing -> existing.getFurnitureId() != null
                        && existing.getFurnitureId().equals(item.getFurnitureId())
                        && existing.getRentalPeriodMonths() == item.getRentalPeriodMonths())
                .mapToInt(CartItem::getQuantity)
                .sum();

        if (existingQuantity + item.getQuantity() > availableStock) {
            throw new IllegalArgumentException("Requested quantity exceeds available stock");
        }

        for (CartItem existing : items) {
            if (existing.getFurnitureId() != null
                    && existing.getFurnitureId().equals(item.getFurnitureId())
                    && existing.getRentalPeriodMonths() == item.getRentalPeriodMonths()) {
                existing.setQuantity(existing.getQuantity() + item.getQuantity());
                existing.setFurnitureName(item.getFurnitureName());
                existing.setPricePerMonth(item.getPricePerMonth());
                return;
            }
        }

        items.add(item);
    }

    public void removeItem(int index) {
        if (index >= 0 && index < items.size())
            items.remove(index);
    }

    public BigDecimal getTotalMonthly() {
        return items.stream()
                .map(i -> i.getPricePerMonth().multiply(new BigDecimal(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
