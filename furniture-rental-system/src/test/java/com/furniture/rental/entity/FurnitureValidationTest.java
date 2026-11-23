package com.furniture.rental.entity;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FurnitureValidationTest {

    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private Furniture valid() {
        Furniture furniture = new Furniture();
        furniture.setName("Wicker Accent Chair");
        furniture.setDescription("Handwoven chair");
        furniture.setRentalPricePerMonth(new BigDecimal("349.00"));
        furniture.setStockQuantity(3);
        return furniture;
    }

    private Set<String> violatedFields(Furniture furniture) {
        return validator.validate(furniture).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }

    @Test
    void aNormalItemIsValid() {
        assertTrue(validator.validate(valid()).isEmpty());
    }

    @Test
    void nameIsRequired() {
        Furniture blank = valid();
        blank.setName("   ");
        Furniture missing = valid();
        missing.setName(null);

        assertEquals(Set.of("name"), violatedFields(blank));
        assertEquals(Set.of("name"), violatedFields(missing));
    }

    @Test
    void rentalPriceMustBeGreaterThanZero() {
        for (String price : List.of("0", "0.00", "-5")) {
            Furniture furniture = valid();
            furniture.setRentalPricePerMonth(new BigDecimal(price));
            assertEquals(Set.of("rentalPricePerMonth"), violatedFields(furniture), price);
        }

        Furniture missing = valid();
        missing.setRentalPricePerMonth(null);
        assertEquals(Set.of("rentalPricePerMonth"), violatedFields(missing));
    }

    @Test
    void pricesCannotHaveMoreThanTwoDecimals() {
        Furniture furniture = valid();
        furniture.setRentalPricePerMonth(new BigDecimal("10.999"));

        assertEquals(Set.of("rentalPricePerMonth"), violatedFields(furniture));
    }

    @Test
    void buyPriceIsOptionalButNeverNegative() {
        Furniture none = valid();
        none.setPurchasePrice(null);
        Furniture zero = valid();
        zero.setPurchasePrice(BigDecimal.ZERO);
        Furniture negative = valid();
        negative.setPurchasePrice(new BigDecimal("-1"));

        assertTrue(validator.validate(none).isEmpty());
        assertTrue(validator.validate(zero).isEmpty());
        assertEquals(Set.of("purchasePrice"), violatedFields(negative));
    }

    @Test
    void stockCannotBeNegative() {
        Furniture empty = valid();
        empty.setStockQuantity(0);
        Furniture negative = valid();
        negative.setStockQuantity(-1);

        assertTrue(validator.validate(empty).isEmpty());
        assertEquals(Set.of("stockQuantity"), violatedFields(negative));
    }

    @Test
    void descriptionIsLimitedToTheColumnSize() {
        Furniture atLimit = valid();
        atLimit.setDescription("x".repeat(1000));
        Furniture tooLong = valid();
        tooLong.setDescription("x".repeat(1001));

        assertTrue(validator.validate(atLimit).isEmpty());
        assertEquals(Set.of("description"), violatedFields(tooLong));
    }
}
