package com.furniture.rental.controller;

import com.furniture.rental.entity.Rental;
import com.furniture.rental.entity.User;
import com.furniture.rental.service.RentalService;
import com.furniture.rental.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/customer")
public class CustomerController {

    @Autowired
    private RentalService rentalService;

    @Autowired
    private UserService userService;

    @GetMapping("/dashboard")
    public String dashboard(Authentication auth, Model model) {
        String username = auth.getName();
        User user = userService.findByUsername(username);

        List<Rental> rentals = rentalService.getRentalsByUser(user.getId());
        model.addAttribute("rentals", rentals);
        model.addAttribute("user", user);

        return "customer/dashboard";
    }
}
