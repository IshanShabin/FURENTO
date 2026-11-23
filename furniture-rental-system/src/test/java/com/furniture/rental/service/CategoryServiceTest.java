package com.furniture.rental.service;

import com.furniture.rental.entity.Category;
import com.furniture.rental.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    private Category category(Long id) {
        Category category = new Category("Living Room", "Chairs", "");
        category.setId(id);
        return category;
    }

    @Test
    void nameIsTakenWhenAnotherCategoryUsesIt() {
        when(categoryRepository.findByNameIgnoreCase("living room")).thenReturn(Optional.of(category(1L)));

        assertTrue(categoryService.isNameTaken("  living room ", null));
        assertTrue(categoryService.isNameTaken("living room", 2L));
    }

    @Test
    void aCategoryMayKeepItsOwnName() {
        when(categoryRepository.findByNameIgnoreCase("Living Room")).thenReturn(Optional.of(category(1L)));

        assertFalse(categoryService.isNameTaken("Living Room", 1L));
    }

    @Test
    void nameIsFreeWhenNoCategoryUsesIt() {
        when(categoryRepository.findByNameIgnoreCase("Sofas")).thenReturn(Optional.empty());

        assertFalse(categoryService.isNameTaken("Sofas", null));
    }

    @Test
    void nullNameIsNeverTaken() {
        assertFalse(categoryService.isNameTaken(null, null));
    }
}
