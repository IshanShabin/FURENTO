package com.furniture.rental.controller;

import com.furniture.rental.entity.Category;
import com.furniture.rental.entity.Furniture;
import com.furniture.rental.entity.FurnitureImage;
import com.furniture.rental.entity.Rental;
import com.furniture.rental.service.CategoryService;
import com.furniture.rental.service.FurnitureService;
import com.furniture.rental.service.RentalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private FurnitureService furnitureService;

    @Autowired
    private RentalService rentalService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        List<Rental> rentals = rentalService.getAllRentals();
        List<Furniture> furniture = furnitureService.getAllFurniture();
        List<Furniture> lowStockFurniture = furniture.stream()
            .filter(item -> item.getStockQuantity() > 0 && item.getStockQuantity() <= 2)
            .sorted(Comparator.comparingInt(Furniture::getStockQuantity))
            .limit(5)
            .toList();

        model.addAttribute("totalRentals", rentals.size());
        model.addAttribute("pendingRentals", rentals.stream()
            .filter(rental -> "PENDING".equalsIgnoreCase(rental.getStatus())).count());
        model.addAttribute("activeRentals", rentals.stream()
            .filter(rental -> "ACTIVE".equalsIgnoreCase(rental.getStatus())).count());
        model.addAttribute("availableFurniture", furniture.stream().filter(Furniture::isAvailable).count());
        model.addAttribute("lowStockCount", furniture.stream()
            .filter(item -> item.getStockQuantity() > 0 && item.getStockQuantity() <= 2).count());
        model.addAttribute("recentRentals", rentals.stream()
            .sorted(Comparator.comparing(Rental::getOrderDate,
                Comparator.nullsLast(Comparator.reverseOrder())))
            .limit(5)
            .toList());
        model.addAttribute("lowStockFurniture", lowStockFurniture);
        return "admin/dashboard";
    }

    // Category Management
    @GetMapping("/categories")
    public String listCategories(Model model) {
        model.addAttribute("categories", categoryService.getAllCategories());
        return "admin/categories";
    }

    @GetMapping("/categories/add")
    public String addCategoryForm(Model model) {
        model.addAttribute("category", new Category());
        return "admin/category-form";
    }

    @GetMapping("/categories/edit/{id}")
    public String editCategoryForm(@PathVariable Long id, Model model) {
        Category category = categoryService.getCategoryById(id);
        if (category == null) {
            return "redirect:/admin/categories";
        }
        model.addAttribute("category", category);
        return "admin/category-form";
    }

    @PostMapping("/categories/save")
    public String saveCategory(@ModelAttribute Category category) {
        if (category.getId() != null) {
            Category existing = categoryService.getCategoryById(category.getId());
            if (existing != null) {
                existing.setName(category.getName());
                existing.setDescription(category.getDescription());
                existing.setImageUrl(category.getImageUrl());
                categoryService.saveCategory(existing);
            }
        } else {
            categoryService.saveCategory(category);
        }
        return "redirect:/admin/categories";
    }

    // Furniture Management
    @GetMapping("/furniture")
    public String listFurniture(Model model) {
        model.addAttribute("furnitureList", furnitureService.getAllFurniture());
        return "admin/furniture";
    }

    @GetMapping("/furniture/add")
    public String addFurnitureForm(Model model) {
        model.addAttribute("furniture", new Furniture());
        model.addAttribute("categories", categoryService.getAllCategories());
        return "admin/furniture-form";
    }

    @GetMapping("/furniture/edit/{id}")
    public String editFurnitureForm(@PathVariable Long id, Model model) {
        Furniture furniture = furnitureService.getFurnitureById(id);
        if (furniture == null) {
            return "redirect:/admin/furniture";
        }
        model.addAttribute("furniture", furniture);
        model.addAttribute("categories", categoryService.getAllCategories());
        return "admin/furniture-form";
    }

    @PostMapping("/furniture/save")
    public String saveFurniture(@ModelAttribute Furniture furniture,
            @RequestParam("categoryId") Long categoryId,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile) {
        if (categoryId != null) {
            furniture.setCategory(categoryService.getCategoryById(categoryId));
        }

        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                Path uploadDir = Path.of("src/main/resources/static/images/furniture");
                Files.createDirectories(uploadDir);
                String fileName = UUID.randomUUID() + "-" + imageFile.getOriginalFilename();
                Path targetPath = uploadDir.resolve(fileName);
                imageFile.transferTo(targetPath);

                FurnitureImage image = new FurnitureImage();
                image.setImageUrl("/images/furniture/" + fileName);
                image.setFurniture(furniture);

                if (furniture.getImages() == null) {
                    furniture.setImages(new ArrayList<>());
                }
                furniture.getImages().clear();
                furniture.getImages().add(image);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to upload furniture image", e);
            }
        }

        if (furniture.getId() != null) {
            Furniture existing = furnitureService.getFurnitureById(furniture.getId());
            if (existing != null) {
                existing.setName(furniture.getName());
                existing.setDescription(furniture.getDescription());
                existing.setRentalPricePerMonth(furniture.getRentalPricePerMonth());
                existing.setPurchasePrice(furniture.getPurchasePrice());
                existing.setStockQuantity(furniture.getStockQuantity());
                existing.setAvailable(furniture.getStockQuantity() > 0);
                existing.setCategory(furniture.getCategory());
                if (furniture.getImages() != null && !furniture.getImages().isEmpty()) {
                    existing.setImages(furniture.getImages());
                    for (FurnitureImage image : existing.getImages()) {
                        image.setFurniture(existing);
                    }
                }
                furnitureService.saveFurniture(existing);
            }
        } else {
            furnitureService.saveFurniture(furniture);
        }
        return "redirect:/admin/furniture";
    }

    // Rentals
    @GetMapping("/rentals")
    public String viewRentals(Model model) {
        model.addAttribute("rentals", rentalService.getAllRentals());
        return "admin/rentals";
    }

    // Delete Actions
    @GetMapping("/categories/delete/{id}")
    public String deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return "redirect:/admin/categories";
    }

    @GetMapping("/furniture/delete/{id}")
    public String deleteFurniture(@PathVariable Long id) {
        furnitureService.deleteFurniture(id);
        return "redirect:/admin/furniture";
    }

    @GetMapping("/rentals/delete/{id}") // In reality, this might be convert to 'cancel'
    public String deleteRental(@PathVariable Long id) {
        // Implement logic to possibly return stock if needed, or soft delete
        rentalService.deleteRental(id);
        return "redirect:/admin/rentals";
    }

    @GetMapping("/rentals/accept/{id}")
    public String acceptRental(@PathVariable Long id) {
        rentalService.updateStatus(id, "ACTIVE");
        return "redirect:/admin/rentals";
    }
}
