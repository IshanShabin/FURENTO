package com.furniture.rental.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeliveryAddressTest {

    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private DeliveryAddress valid() {
        DeliveryAddress address = new DeliveryAddress();
        address.setAddressLine1("1 Main Road");
        address.setCity("Malappuram");
        address.setState("Kerala");
        address.setZipCode("676503");
        return address;
    }

    private Set<String> violatedFields(DeliveryAddress address) {
        return validator.validate(address).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }

    @Test
    void aCompleteAddressIsValid() {
        assertTrue(validator.validate(valid()).isEmpty());
    }

    @Test
    void addressLineTwoIsOptional() {
        DeliveryAddress address = valid();
        address.setAddressLine2(null);

        assertTrue(validator.validate(address).isEmpty());
        assertEquals("", address.getAddressLine2());
    }

    @Test
    void requiredPartsCannotBeBlank() {
        DeliveryAddress address = new DeliveryAddress();
        address.setAddressLine1("   ");
        address.setCity(null);
        address.setState("");
        address.setZipCode("  ");

        assertEquals(Set.of("addressLine1", "city", "state", "zipCode"), violatedFields(address));
    }

    @Test
    void pinCodeMustBeSixDigitsNotStartingWithZero() {
        for (String ok : new String[] { "676503", "110001" }) {
            DeliveryAddress address = valid();
            address.setZipCode(ok);
            assertTrue(validator.validate(address).isEmpty(), ok);
        }
        for (String bad : new String[] { "12345", "012345", "67650A", "6765031", "67 503" }) {
            DeliveryAddress address = valid();
            address.setZipCode(bad);
            assertEquals(Set.of("zipCode"), violatedFields(address), bad);
        }
    }

    @Test
    void textIsTrimmedWhenItIsSet() {
        DeliveryAddress address = valid();
        address.setAddressLine1("  1 Main Road ");
        address.setZipCode(" 676503 ");

        assertEquals("1 Main Road", address.getAddressLine1());
        assertEquals("676503", address.getZipCode());
        assertTrue(validator.validate(address).isEmpty());
    }

    @Test
    void textLongerThanTheLimitsIsRejected() {
        DeliveryAddress address = valid();
        address.setAddressLine1("x".repeat(201));
        address.setAddressLine2("x".repeat(201));
        address.setCity("x".repeat(101));
        address.setState("x".repeat(101));

        assertEquals(Set.of("addressLine1", "addressLine2", "city", "state"), violatedFields(address));
    }
}
