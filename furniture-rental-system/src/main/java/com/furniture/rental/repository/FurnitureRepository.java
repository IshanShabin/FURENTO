package com.furniture.rental.repository;

import com.furniture.rental.entity.Furniture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;

public interface FurnitureRepository extends JpaRepository<Furniture, Long>, JpaSpecificationExecutor<Furniture> {
    List<Furniture> findByCategoryId(Long categoryId);

    boolean existsByNameIgnoreCase(String name);

}
