package com.furniture.rental.controller;

import com.furniture.rental.dto.Cart;
import com.furniture.rental.dto.CartItem;
import com.furniture.rental.entity.Delivery;
import com.furniture.rental.entity.Furniture;
import com.furniture.rental.entity.Payment;
import com.furniture.rental.entity.Rental;
import com.furniture.rental.entity.RentalItem;
import com.furniture.rental.entity.User;
import com.furniture.rental.service.FurnitureService;
import com.furniture.rental.service.RentalService;
import com.furniture.rental.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;

@Controller
@RequestMapping("/cart")
public class CartController {

    @Autowired
    private FurnitureService furnitureService;

    @Autowired
    private RentalService rentalService;

    @Autowired
    private UserService userService;

    @GetMapping
    public String viewCart(HttpSession session, Model model) {
        Cart cart = (Cart) session.getAttribute("cart");
        if (cart == null) {
            cart = new Cart();
            session.setAttribute("cart", cart);
        }

        // Calculate total
        BigDecimal total = BigDecimal.ZERO;
        for (CartItem item : cart.getItems()) {
            total = total.add(item.getPricePerMonth().multiply(new BigDecimal(item.getQuantity()))
                    .multiply(new BigDecimal(item.getRentalPeriodMonths())));
        }
        cart.setTotalPrice(total);

        model.addAttribute("cart", cart);
        return "cart";
    }

    @PostMapping("/add")
    public String addToCart(@RequestParam Long furnitureId,
            @RequestParam int quantity,
            @RequestParam int months,
            HttpSession session) {
        Furniture f = furnitureService.getFurnitureById(furnitureId);
        if (f != null && quantity > 0) {
            Cart cart = (Cart) session.getAttribute("cart");
            if (cart == null) {
                cart = new Cart();
                session.setAttribute("cart", cart);
            }
            cart.addItem(new CartItem(f.getId(), f.getName(), f.getRentalPricePerMonth(), quantity, months));
        }
        return "redirect:/cart";
    }

    @GetMapping("/update/{index}")
    public String updateCartQuantity(@PathVariable int index, @RequestParam String action, HttpSession session) {
        Cart cart = (Cart) session.getAttribute("cart");
        if (cart != null && index >= 0 && index < cart.getItems().size()) {
            CartItem item = cart.getItems().get(index);
            if ("increase".equals(action)) {
                item.setQuantity(item.getQuantity() + 1);
            } else if ("decrease".equals(action)) {
                if (item.getQuantity() > 1) {
                    item.setQuantity(item.getQuantity() - 1);
                }
            }
        }
        return "redirect:/cart";
    }

    @GetMapping("/remove/{index}")
    public String removeFromCart(@PathVariable int index, HttpSession session) {
        Cart cart = (Cart) session.getAttribute("cart");
        if (cart != null) {
            cart.removeItem(index);
        }
        return "redirect:/cart";
    }

    @GetMapping("/checkout")
    public String checkoutPage(HttpSession session, Model model) {
        Cart cart = (Cart) session.getAttribute("cart");
        if (cart == null || cart.getItems().isEmpty()) {
            return "redirect:/cart";
        }

        // Calculate total if not already
        BigDecimal total = BigDecimal.ZERO;
        for (CartItem item : cart.getItems()) {
            total = total.add(item.getPricePerMonth().multiply(new BigDecimal(item.getQuantity()))
                    .multiply(new BigDecimal(item.getRentalPeriodMonths())));
        }
        cart.setTotalPrice(total); // Assuming you add setTotalPrice to Cart or calculate it in template

        model.addAttribute("cart", cart);
        return "checkout";
    }

    @PostMapping("/process-checkout")
    public String processCheckout(@RequestParam String addressLine1,
            @RequestParam String addressLine2,
            @RequestParam String city,
            @RequestParam String state,
            @RequestParam String zipCode,
            @RequestParam String paymentMethod,
            HttpSession session,
            Authentication auth) {

        Cart cart = (Cart) session.getAttribute("cart");
        if (cart == null || cart.getItems().isEmpty()) {
            return "redirect:/cart";
        }

        User user = userService.findByUsername(auth.getName());

        Rental rental = new Rental();
        rental.setUser(user);
        rental.setItems(new ArrayList<>());

        // Create Delivery Object
        Delivery delivery = new Delivery();
        delivery.setAddressLine1(addressLine1);
        delivery.setAddressLine2(addressLine2);
        delivery.setCity(city);
        delivery.setState(state);
        delivery.setZipCode(zipCode);
        delivery.setStatus("PENDING");
        rental.setDelivery(delivery);

        // Create Payment Object
        Payment payment = new Payment();
        payment.setPaymentMethod(paymentMethod);
        payment.setPaymentDate(LocalDateTime.now());
        payment.setStatus("COMPLETED"); // Simplified for demo
        rental.setPayment(payment);

        for (CartItem ci : cart.getItems()) {
            RentalItem ri = new RentalItem();
            ri.setFurniture(furnitureService.getFurnitureById(ci.getFurnitureId()));
            ri.setQuantity(ci.getQuantity());
            ri.setRentalPeriodMonths(ci.getRentalPeriodMonths());
            ri.setRental(rental);
            rental.getItems().add(ri);
        }

        rentalService.createRental(rental);

        session.removeAttribute("cart");
        return "redirect:/customer/dashboard";
    }
}
