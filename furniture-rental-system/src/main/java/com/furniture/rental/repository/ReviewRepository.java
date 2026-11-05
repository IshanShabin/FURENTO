package com.furniture.rental.repository;

import com.furniture.rental.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByFurnitureIdOrderByCreatedAtDesc(Long furnitureId);

    boolean existsByFurnitureIdAndUserId(Long furnitureId, Long userId);
}
