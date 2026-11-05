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
import org.springframework.web.bind.annotation.PathVariable;
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

    @GetMapping("/rental/{id}/invoice")
    public String viewInvoice(@PathVariable Long id, Authentication auth, Model model) {
        Rental rental = rentalService.getRentalById(id);
        if (rental == null) {
            return "redirect:/customer/dashboard";
        }

        // simple security check
        String currentUsername = auth.getName();
        if (!rental.getUser().getUsername().equals(currentUsername)) {
            return "redirect:/customer/dashboard";
        }

        // Self-healing: Ensure delivery status matches rental status for the invoice
        // view
        if ("ACTIVE".equals(rental.getStatus()) && rental.getDelivery() != null
                && "PENDING".equals(rental.getDelivery().getStatus())) {
            rental.getDelivery().setStatus("DELIVERED");
            rentalService.createRental(rental); // Re-saving to persist the fix (createRental acts as save here)
        }

        model.addAttribute("rental", rental);
        return "customer/invoice";
    }
}
