package com.furniture.rental.repository;

import com.furniture.rental.entity.Furniture;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FurnitureRepository extends JpaRepository<Furniture, Long> {
    List<Furniture> findByCategoryId(Long categoryId);
}
