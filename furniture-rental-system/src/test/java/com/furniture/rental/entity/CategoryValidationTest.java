package com.furniture.rental.entity;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CategoryValidationTest {

    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private Set<String> violatedFields(Category category) {
        return validator.validate(category).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }

    @Test
    void aNormalCategoryIsValid() {
        Category category = new Category("Living Room", "Chairs and tables", "https://example.com/living.jpg");

        assertTrue(validator.validate(category).isEmpty());
    }

    @Test
    void nameIsRequired() {
        assertEquals(Set.of("name"), violatedFields(new Category("   ", "d", "")));
        assertEquals(Set.of("name"), violatedFields(new Category(null, "d", "")));
    }

    @Test
    void nameAndDescriptionHaveLengthLimits() {
        assertEquals(Set.of("name"), violatedFields(new Category("x".repeat(101), "d", "")));
        assertEquals(Set.of("description"), violatedFields(new Category("Name", "x".repeat(256), "")));
    }

    @Test
    void imageUrlMustBeEmptyOrHttp() {
        for (String ok : List.of("", "http://example.com/a.png", "https://example.com/a.png")) {
            assertTrue(validator.validate(new Category("Name", "d", ok)).isEmpty(), ok);
        }
        assertTrue(validator.validate(new Category("Name", "d", null)).isEmpty());

        for (String bad : List.of("javascript:alert(1)", "ftp://example.com/a.png", "/images/a.png", "https://a b.com")) {
            assertEquals(Set.of("imageUrl"), violatedFields(new Category("Name", "d", bad)), bad);
        }
    }
}
