package com.furniture.rental.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@EnableConfigurationProperties(BusinessProperties.class)
public class SiteModelAdvice {

    private final BusinessProperties business;

    public SiteModelAdvice(BusinessProperties business) {
        this.business = business;
    }

    @ModelAttribute("business")
    public BusinessProperties business() {
        return business;
    }
}