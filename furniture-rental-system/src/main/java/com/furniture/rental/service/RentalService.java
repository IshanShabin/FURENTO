package com.furniture.rental.service;

import com.furniture.rental.entity.*;
import com.furniture.rental.repository.FurnitureRepository;
import com.furniture.rental.repository.RentalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

//import java.util.Objects;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class RentalService {

    @Autowired
    private RentalRepository rentalRepository;

    @Autowired
    private FurnitureRepository furnitureRepository;

    @Transactional
    public Rental createRental(Rental rental) {
        // Calculate totals and update stock
        BigDecimal total = BigDecimal.ZERO;

        for (RentalItem item : rental.getItems()) {
            Furniture f = furnitureRepository.findById(item.getFurniture().getId())
                    .orElseThrow(() -> new RuntimeException("Furniture not found"));

            if (f.getStockQuantity() < item.getQuantity()) {
                throw new RuntimeException("Not enough stock for: " + f.getName());
            }

            f.setStockQuantity(f.getStockQuantity() - item.getQuantity());
            f.setAvailable(f.getStockQuantity() > 0);
            furnitureRepository.save(f);

            BigDecimal lineAmount = f.getRentalPricePerMonth()
                    .multiply(new BigDecimal(item.getRentalPeriodMonths()))
                    .multiply(new BigDecimal(item.getQuantity()));
            item.setRentAmount(lineAmount);
            item.setRental(rental);

            total = total.add(lineAmount);
        }

        BigDecimal tax = total.multiply(new BigDecimal("0.05")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal grandTotal = total.add(tax);
        rental.setTotalAmount(grandTotal);
        rental.setOrderDate(LocalDateTime.now());
        rental.setStatus("PENDING"); // or ACTIVE depending on payment flow

        if (rental.getPayment() != null) {
            rental.getPayment().setAmount(grandTotal);
            rental.getPayment().setRental(rental);
        }

        if (rental.getDelivery() != null) {
            rental.getDelivery().setRental(rental);
        }

        return rentalRepository.save(rental);
    }

    public List<Rental> getRentalsByUser(Long userId) {
        return rentalRepository.findByUserId(userId);
    }

    public List<Rental> getAllRentals() {
        return rentalRepository.findAll();
    }

    public Rental getRentalById(Long id) {
        return rentalRepository.findById(id).orElse(null);
    }

    @Transactional
    public void updateStatus(Long id, String status) {
    Rental rental = rentalRepository.findById(id).orElse(null);
        if (rental == null) {
            return;
        }
        if ("ACTIVE".equals(status) && !"PENDING".equals(rental.getStatus())) {
            return;
        }
                    rental.setStatus(status);
        if ("ACTIVE".equals(status)) {
            LocalDate start = LocalDate.now();
            if (rental.getItems() != null) {
                for (RentalItem item : rental.getItems()) {
                    item.setStartDate(start);
                    item.setEndDate(start.plusMonths(item.getRentalPeriodMonths()));
                }
            }
            if (rental.getDelivery() != null) {
                rental.getDelivery().setStatus("DELIVERED");
                rental.getDelivery().setDeliveryDate(LocalDateTime.now());
            }
        }
        rentalRepository.save(rental);
        rentalRepository.flush();
    }

    @Transactional
    public boolean cancelRental(Long id) {
    Rental rental = rentalRepository.findById(id).orElse(null);
    if (rental == null) {
        return false;
    }
        if (!"PENDING".equals(rental.getStatus())) {
        return false;
    }

    if (rental.getItems() != null) {
        for (RentalItem item : rental.getItems()) {
            Furniture furniture = item.getFurniture();
            furniture.setStockQuantity(furniture.getStockQuantity() + item.getQuantity());
            furnitureRepository.save(furniture);
        }
    }

        rental.setStatus("CANCELLED");
        if (rental.getDelivery() != null) {
            rental.getDelivery().setStatus("CANCELLED");
        }
        if (rental.getPayment() != null && "PENDING".equals(rental.getPayment().getStatus())) {
            rental.getPayment().setStatus("CANCELLED");
        }
        rentalRepository.save(rental);
        return true;
    }

    
    @Transactional
    public boolean returnRental(Long id) {
        Rental rental = rentalRepository.findById(id).orElse(null);
        if (rental == null || !"ACTIVE".equals(rental.getStatus())) {
            return false;
        }

        if (rental.getItems() != null) {
            for (RentalItem item : rental.getItems()) {
                Furniture furniture = item.getFurniture();
                furniture.setStockQuantity(furniture.getStockQuantity() + item.getQuantity());
                furnitureRepository.save(furniture);
            }
        }

        rental.setStatus("RETURNED");
        if (rental.getDelivery() != null) {
            rental.getDelivery().setStatus("PICKED_UP");
            rental.getDelivery().setPickupDate(LocalDateTime.now());
        }
        rentalRepository.save(rental);
        return true;
    }

        public Rental saveRental(Rental rental) {
        return rentalRepository.save(rental);
    }
}
