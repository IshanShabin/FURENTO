package com.furniture.rental.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * The delivery address entered at checkout.
 * Setters trim the text, so a value of only spaces counts as blank.
 */
public class DeliveryAddress {

    @NotBlank(message = "Address line 1 is required.")
    @Size(max = 200, message = "Address line 1 must be 200 characters or fewer.")
    private String addressLine1;

    @Size(max = 200, message = "Address line 2 must be 200 characters or fewer.")
    private String addressLine2 = "";

    @NotBlank(message = "City is required.")
    @Size(max = 100, message = "City must be 100 characters or fewer.")
    private String city;

    @NotBlank(message = "State is required.")
    @Size(max = 100, message = "State must be 100 characters or fewer.")
    private String state;

    @NotBlank(message = "PIN code is required.")
    @Pattern(regexp = "^$|^[1-9][0-9]{5}$", message = "Enter a valid 6-digit PIN code.")
    private String zipCode;

    public String getAddressLine1() {
        return addressLine1;
    }

    public void setAddressLine1(String addressLine1) {
        this.addressLine1 = trimmed(addressLine1);
    }

    public String getAddressLine2() {
        return addressLine2;
    }

    public void setAddressLine2(String addressLine2) {
        this.addressLine2 = addressLine2 == null ? "" : addressLine2.trim();
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = trimmed(city);
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = trimmed(state);
    }

    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = trimmed(zipCode);
    }

    private static String trimmed(String value) {
        return value == null ? null : value.trim();
    }
}
