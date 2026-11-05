package com.furniture.rental.controller;

import com.furniture.rental.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/reviews")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    @PostMapping("/submit")
    @PreAuthorize("hasRole('CUSTOMER')")
    public String submitReview(@RequestParam Long furnitureId,
            @RequestParam int rating,
            @RequestParam(required = false) String comment,
            Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        try {
            reviewService.submitReview(furnitureId, authentication.getName(), rating, comment);
        } catch (IllegalArgumentException ignored) {
            // Keep the user on the product page even if validation fails.
        }

        return "redirect:/furniture/" + furnitureId + "#reviews";
    }
}
