package com.furniture.rental.service;

import com.furniture.rental.entity.Category;
import com.furniture.rental.repository.CategoryRepository;
import com.furniture.rental.repository.FurnitureRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    public Category getCategoryById(Long id) {
        return categoryRepository.findById(id).orElse(null);
    }

    /**
     * True if another category already uses this name (ignoring case).
     * Pass the id of the category being edited so it does not clash with itself.
     */
    public boolean isNameTaken(String name, Long excludeId) {
        if (name == null) {
            return false;
        }
        return categoryRepository.findByNameIgnoreCase(name.trim())
                .map(existing -> !existing.getId().equals(excludeId))
                .orElse(false);
    }

    public void saveCategory(Category category) {
        categoryRepository.save(category);
    }

    public void deleteCategory(Long id) {
        categoryRepository.deleteById(id);
    }

    @Autowired
    private FurnitureRepository furnitureRepository;

    public boolean hasFurniture(Long categoryId) {
        return !furnitureRepository.findByCategoryId(categoryId).isEmpty();
    }

}
