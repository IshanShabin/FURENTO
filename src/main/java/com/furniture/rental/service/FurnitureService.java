package com.furniture.rental.service;

import com.furniture.rental.entity.Furniture;
import com.furniture.rental.repository.FurnitureRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.Comparator;
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

    public List<Furniture> getCustomerCatalog() {
        return getAllFurniture().stream()
                .filter(furniture -> !"Sofa".equalsIgnoreCase(furniture.getName()))
                .toList();
    }

    public List<Furniture> getTrendingFurniture() {
        List<Furniture> newestAvailable = getCustomerCatalog().stream()
                .filter(Furniture::isAvailable)
                .sorted(Comparator.comparing(Furniture::getId).reversed())
                .toList();
        List<Furniture> trending = new ArrayList<>(6);

        addNewestByCategory(newestAvailable, trending, "Living Room", 1);
        addNewestByCategory(newestAvailable, trending, "Bedroom", 2);
        addNewestByCategory(newestAvailable, trending, "Dining Room", 1);
        addNewestByCategory(newestAvailable, trending, "Home Office", 1);
        addNewestByCategory(newestAvailable, trending, "Office Room", 1);
        return trending;
    }

    private void addNewestByCategory(List<Furniture> available, List<Furniture> trending,
            String categoryName, int limit) {
        for (Furniture item : available) {
            if (item.getCategory() != null && categoryName.equalsIgnoreCase(item.getCategory().getName())) {
                trending.add(item);
                if (--limit == 0) {
                    return;
                }
            }
        }
    }

    public boolean isCustomerCatalogItem(Furniture furniture) {
        return furniture != null && !"Sofa".equalsIgnoreCase(furniture.getName());
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
