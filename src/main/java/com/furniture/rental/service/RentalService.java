package com.furniture.rental.service;

import com.furniture.rental.entity.*;
import com.furniture.rental.repository.FurnitureRepository;
import com.furniture.rental.repository.RentalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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

        rental.setTotalAmount(total);
        rental.setOrderDate(LocalDateTime.now());
        rental.setStatus("PENDING"); // or ACTIVE depending on payment flow

        if (rental.getPayment() != null) {
            rental.getPayment().setAmount(total);
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

    public void deleteRental(Long id) {
        // Ideally we should restore stock here before deleting
        rentalRepository.deleteById(id);
    }
}
