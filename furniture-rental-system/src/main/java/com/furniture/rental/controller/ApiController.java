package com.furniture.rental.controller;

import com.furniture.rental.entity.Furniture;
import com.furniture.rental.entity.Rental;
import com.furniture.rental.service.FurnitureService;
import com.furniture.rental.service.RentalService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final FurnitureService furnitureService;
    private final RentalService rentalService;

    public ApiController(FurnitureService furnitureService, RentalService rentalService) {
        this.furnitureService = furnitureService;
        this.rentalService = rentalService;
    }

    @GetMapping("/me")
    public Map<String, Object> me(Authentication authentication) {
        List<String> roles = authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .toList();
        return Map.of("username", authentication.getName(), "roles", roles);
    }

    @GetMapping("/furniture")
    public List<FurnitureResponse> furniture() {
        return furnitureService.getCustomerCatalog().stream()
                .map(FurnitureResponse::from)
                .toList();
    }

    @GetMapping("/furniture/{id}")
    public ResponseEntity<FurnitureResponse> furnitureById(@PathVariable Long id) {
        Furniture furniture = furnitureService.getFurnitureById(id);
        if (!furnitureService.isCustomerCatalogItem(furniture)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(FurnitureResponse.from(furniture));
    }

    @GetMapping("/admin/rentals")
    public List<RentalSummary> allRentals() {
        return rentalService.getAllRentals().stream()
                .map(RentalSummary::from)
                .toList();
    }

    public record FurnitureResponse(Long id, String name, String category, String description,
            BigDecimal rentalPricePerMonth, int stockQuantity, boolean available, String imageUrl) {

        static FurnitureResponse from(Furniture f) {
            String image = (f.getImages() != null && !f.getImages().isEmpty())
                    ? f.getImages().get(0).getImageUrl() : null;
            return new FurnitureResponse(f.getId(), f.getName(),
                    f.getCategory() != null ? f.getCategory().getName() : null,
                    f.getDescription(), f.getRentalPricePerMonth(), f.getStockQuantity(),
                    f.isAvailable(), image);
        }
    }

    public record RentalSummary(Long id, String username, LocalDateTime orderDate,
            BigDecimal totalAmount, String status, int itemCount) {

        static RentalSummary from(Rental r) {
            return new RentalSummary(r.getId(),
                    r.getUser() != null ? r.getUser().getUsername() : null,
                    r.getOrderDate(), r.getTotalAmount(), r.getStatus(),
                    r.getItems() != null ? r.getItems().size() : 0);
        }
    }
}
