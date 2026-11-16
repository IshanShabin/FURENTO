package com.furniture.rental.config;

import com.furniture.rental.util.Pricing;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class PricingModelAdvice {

    @ModelAttribute("taxPercent")
    public String taxPercent() {
        return Pricing.taxPercentLabel();
    }
}