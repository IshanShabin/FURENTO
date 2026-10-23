package com.furniture.rental.repository;

import com.furniture.rental.entity.RentalItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RentalItemRepository extends JpaRepository<RentalItem, Long> {
    boolean existsByFurniture_Id(Long furnitureId);
}