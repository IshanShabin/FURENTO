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
import com.furniture.rental.service.RazorpayService;
import com.furniture.rental.service.RentalService;
import com.furniture.rental.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Controller
@RequestMapping("/cart")
public class CartController {

    @Autowired
    private FurnitureService furnitureService;

    @Autowired
    private RentalService rentalService;

    @Autowired
    private UserService userService;

    @Autowired
    private RazorpayService razorpayService;

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
        model.addAttribute("razorpayEnabled", razorpayService.isConfigured());
        model.addAttribute("razorpayKeyId", razorpayService.getKeyId());
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

        if (!"COD".equals(paymentMethod)) {
            return "redirect:/cart/checkout";
        }

        Cart cart = (Cart) session.getAttribute("cart");
        if (cart == null || cart.getItems().isEmpty()) {
            return "redirect:/cart";
        }

        User user = userService.findByUsername(auth.getName());

        Payment payment = new Payment();
        payment.setPaymentMethod("COD");
        payment.setPaymentDate(LocalDateTime.now());
        payment.setStatus("PENDING");
        createRental(user, cart.getItems(), addressLine1, addressLine2, city, state, zipCode, payment);

        session.removeAttribute("cart");
        return "redirect:/customer/dashboard";
    }

    @PostMapping("/create-payment-order")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createPaymentOrder(
            @RequestParam String addressLine1,
            @RequestParam(required = false, defaultValue = "") String addressLine2,
            @RequestParam String city,
            @RequestParam String state,
            @RequestParam String zipCode,
            HttpSession session,
            Authentication auth) {
        Cart cart = (Cart) session.getAttribute("cart");
        if (cart == null || cart.getItems().isEmpty()) {
            return paymentError(HttpStatus.BAD_REQUEST, "Your cart is empty.");
        }
        if (!razorpayService.isConfigured()) {
            return paymentError(HttpStatus.SERVICE_UNAVAILABLE, "Online payments are not configured.");
        }

        try {
            List<CartItem> orderItems = getPricedOrderItems(cart);
            BigDecimal amount = calculateGrandTotal(orderItems);
            long amountPaise = RazorpayService.toPaise(amount);
            RazorpayService.GatewayOrder order = razorpayService.createOrder(amountPaise);
            if (order.amountPaise() != amountPaise || !"INR".equals(order.currency())) {
                return paymentError(HttpStatus.BAD_GATEWAY, "The payment gateway returned an invalid order.");
            }

            session.setAttribute("pendingCheckout", new PendingCheckout(order.id(), auth.getName(), amountPaise,
                    addressLine1, addressLine2, city, state, zipCode, orderItems));
            return ResponseEntity.ok(Map.of(
                    "keyId", razorpayService.getKeyId(),
                    "orderId", order.id(),
                    "amount", amountPaise,
                    "currency", "INR"));
        } catch (RestClientException | IllegalArgumentException exception) {
            return paymentError(HttpStatus.BAD_GATEWAY, "Unable to start payment. Check your Razorpay keys and try again.");
        }
    }

    @PostMapping("/confirm-payment")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> confirmPayment(
            @RequestParam("razorpay_order_id") String orderId,
            @RequestParam("razorpay_payment_id") String paymentId,
            @RequestParam("razorpay_signature") String signature,
            HttpSession session,
            Authentication auth) {
        PendingCheckout pending = (PendingCheckout) session.getAttribute("pendingCheckout");
        if (pending == null || !pending.username().equals(auth.getName()) || !pending.orderId().equals(orderId)) {
            return paymentError(HttpStatus.BAD_REQUEST, "This payment attempt is no longer valid.");
        }
        if (!razorpayService.verifySignature(orderId, paymentId, signature)) {
            return paymentError(HttpStatus.BAD_REQUEST, "Payment verification failed.");
        }

        try {
            RazorpayService.GatewayPayment gatewayPayment = razorpayService.fetchPayment(paymentId);
            if (!orderId.equals(gatewayPayment.orderId()) || gatewayPayment.amountPaise() != pending.amountPaise()) {
                return paymentError(HttpStatus.CONFLICT, "The payment details do not match this order.");
            }
            if ("authorized".equals(gatewayPayment.status())) {
                gatewayPayment = razorpayService.capturePayment(paymentId, pending.amountPaise());
            }
            if (!"captured".equals(gatewayPayment.status())) {
                return paymentError(HttpStatus.CONFLICT, "The payment has not been captured.");
            }

            Payment payment = new Payment();
            payment.setPaymentMethod(gatewayPayment.method().toUpperCase(Locale.ROOT));
            payment.setPaymentDate(LocalDateTime.now());
            payment.setStatus("PAID");
            payment.setGatewayOrderId(orderId);
            payment.setGatewayPaymentId(paymentId);
            User user = userService.findByUsername(auth.getName());
            createRental(user, pending.items(), pending.addressLine1(), pending.addressLine2(), pending.city(),
                    pending.state(), pending.zipCode(), payment);

            session.removeAttribute("cart");
            session.removeAttribute("pendingCheckout");
            return ResponseEntity.ok(Map.of("redirectUrl", "/customer/dashboard"));
        } catch (RestClientException exception) {
            return paymentError(HttpStatus.BAD_GATEWAY, "Unable to verify payment with Razorpay. Please contact support.");
        }
    }

    private List<CartItem> getPricedOrderItems(Cart cart) {
        List<CartItem> items = new ArrayList<>();
        for (CartItem item : cart.getItems()) {
            if (item.getQuantity() <= 0 || item.getRentalPeriodMonths() <= 0) {
                throw new IllegalArgumentException("Invalid cart quantity or rental period");
            }
            Furniture furniture = furnitureService.getFurnitureById(item.getFurnitureId());
            if (furniture == null || furniture.getStockQuantity() < item.getQuantity()) {
                throw new IllegalArgumentException("Furniture is unavailable");
            }
            items.add(new CartItem(furniture.getId(), furniture.getName(), furniture.getRentalPricePerMonth(),
                    item.getQuantity(), item.getRentalPeriodMonths()));
        }
        return items;
    }

    private BigDecimal calculateGrandTotal(List<CartItem> items) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem item : items) {
            subtotal = subtotal.add(item.getPricePerMonth()
                    .multiply(BigDecimal.valueOf(item.getQuantity()))
                    .multiply(BigDecimal.valueOf(item.getRentalPeriodMonths())));
        }
        BigDecimal tax = subtotal.multiply(new BigDecimal("0.05")).setScale(2, RoundingMode.HALF_UP);
        return subtotal.add(tax).setScale(2, RoundingMode.HALF_UP);
    }

    private Rental createRental(User user, List<CartItem> items, String addressLine1, String addressLine2,
            String city, String state, String zipCode, Payment payment) {
        Rental rental = new Rental();
        rental.setUser(user);
        rental.setItems(new ArrayList<>());

        Delivery delivery = new Delivery();
        delivery.setAddressLine1(addressLine1);
        delivery.setAddressLine2(addressLine2);
        delivery.setCity(city);
        delivery.setState(state);
        delivery.setZipCode(zipCode);
        delivery.setStatus("PENDING");
        rental.setDelivery(delivery);
        rental.setPayment(payment);

        for (CartItem cartItem : items) {
            RentalItem rentalItem = new RentalItem();
            rentalItem.setFurniture(furnitureService.getFurnitureById(cartItem.getFurnitureId()));
            rentalItem.setQuantity(cartItem.getQuantity());
            rentalItem.setRentalPeriodMonths(cartItem.getRentalPeriodMonths());
            rentalItem.setRental(rental);
            rental.getItems().add(rentalItem);
        }
        return rentalService.createRental(rental);
    }

    private ResponseEntity<Map<String, Object>> paymentError(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("message", message));
    }

    private record PendingCheckout(String orderId, String username, long amountPaise, String addressLine1,
            String addressLine2, String city, String state, String zipCode, List<CartItem> items) {
    }
}
