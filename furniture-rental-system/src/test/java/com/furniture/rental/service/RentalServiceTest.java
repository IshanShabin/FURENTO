package com.furniture.rental.service;

import com.furniture.rental.entity.Delivery;
import com.furniture.rental.entity.Furniture;
import com.furniture.rental.entity.Rental;
import com.furniture.rental.entity.RentalItem;
import com.furniture.rental.repository.FurnitureRepository;
import com.furniture.rental.repository.RentalRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RentalServiceTest {

    @Mock
    private RentalRepository rentalRepository;

    @Mock
    private FurnitureRepository furnitureRepository;

    @InjectMocks
    private RentalService rentalService;

    private Rental rentalWith(Furniture furniture, int quantity, String status) {
        RentalItem item = new RentalItem();
        item.setFurniture(furniture);
        item.setQuantity(quantity);
        Delivery delivery = new Delivery();
        delivery.setStatus("PENDING");
        Rental rental = new Rental();
        rental.setStatus(status);
        rental.setItems(new ArrayList<>(List.of(item)));
        rental.setDelivery(delivery);
        return rental;
    }

    @Test
    void cancelRestoresStockAndMarksCancelled() {
        Furniture chair = new Furniture();
        chair.setStockQuantity(0);
        Rental rental = rentalWith(chair, 3, "PENDING");
        when(rentalRepository.findById(1L)).thenReturn(Optional.of(rental));

        assertTrue(rentalService.cancelRental(1L));

        assertEquals(3, chair.getStockQuantity());
        assertTrue(chair.isAvailable());
        assertEquals("CANCELLED", rental.getStatus());
        assertEquals("CANCELLED", rental.getDelivery().getStatus());
        verify(furnitureRepository).save(chair);
        verify(rentalRepository).save(rental);
    }

    @Test
    void cancellingTwiceDoesNotRestoreStockAgain() {
        Furniture chair = new Furniture();
        chair.setStockQuantity(5);
        Rental rental = rentalWith(chair, 2, "CANCELLED");
        when(rentalRepository.findById(1L)).thenReturn(Optional.of(rental));

        assertFalse(rentalService.cancelRental(1L));

        assertEquals(5, chair.getStockQuantity());
        verify(furnitureRepository, never()).save(any());
    }

    @Test
    void returnedRentalCannotBeCancelled() {
        Furniture chair = new Furniture();
        chair.setStockQuantity(1);
        Rental rental = rentalWith(chair, 1, "RETURNED");
        when(rentalRepository.findById(1L)).thenReturn(Optional.of(rental));

        assertFalse(rentalService.cancelRental(1L));
        assertEquals("RETURNED", rental.getStatus());
    }

    
    @Test
    void activeRentalCannotBeCancelled() {
        Furniture chair = new Furniture();
        chair.setStockQuantity(1);
        Rental rental = rentalWith(chair, 2, "ACTIVE");
        when(rentalRepository.findById(1L)).thenReturn(Optional.of(rental));

        assertFalse(rentalService.cancelRental(1L));

        assertEquals("ACTIVE", rental.getStatus());
        assertEquals(1, chair.getStockQuantity());
        verify(furnitureRepository, never()).save(any());
    }

    @Test
    void cancellingUnknownRentalReturnsFalse() {
        assertFalse(rentalService.cancelRental(99L));
    }

    @Test
    void cancelledRentalCannotBeReactivated() {
        Rental rental = rentalWith(new Furniture(), 1, "CANCELLED");
        when(rentalRepository.findById(1L)).thenReturn(Optional.of(rental));

        rentalService.updateStatus(1L, "ACTIVE");

        assertEquals("CANCELLED", rental.getStatus());
        verify(rentalRepository, never()).save(any());
    }

    @Test
    void acceptingPendingRentalActivatesItAndMarksDelivered() {
        Rental rental = rentalWith(new Furniture(), 1, "PENDING");
        when(rentalRepository.findById(1L)).thenReturn(Optional.of(rental));

        rentalService.updateStatus(1L, "ACTIVE");

        assertEquals("ACTIVE", rental.getStatus());
        assertEquals("DELIVERED", rental.getDelivery().getStatus());
    }
}