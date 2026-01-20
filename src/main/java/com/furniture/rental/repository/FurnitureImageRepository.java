package com.furniture.rental.repository;

import com.furniture.rental.entity.FurnitureImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FurnitureImageRepository extends JpaRepository<FurnitureImage, Long> {
}
