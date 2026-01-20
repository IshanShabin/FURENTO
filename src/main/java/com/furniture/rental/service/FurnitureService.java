package com.furniture.rental.service;

import com.furniture.rental.entity.Furniture;
import com.furniture.rental.repository.FurnitureRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class FurnitureService {

    @Autowired
    private FurnitureRepository furnitureRepository;

    public List<Furniture> getAllFurniture() {
        return furnitureRepository.findAll();
    }

    public List<Furniture> getFurnitureByCategory(Long categoryId) {
        return furnitureRepository.findByCategoryId(categoryId);
    }

    public Furniture getFurnitureById(Long id) {
        return furnitureRepository.findById(id).orElse(null);
    }

    public Furniture saveFurniture(Furniture furniture) {
        // Update availability logic
        furniture.setAvailable(furniture.getStockQuantity() > 0);
        return furnitureRepository.save(furniture);
    }

    public void deleteFurniture(Long id) {
        furnitureRepository.deleteById(id);
    }
}
