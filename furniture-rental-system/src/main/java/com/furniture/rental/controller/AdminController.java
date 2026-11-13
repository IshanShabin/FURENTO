package com.furniture.rental.controller;

import com.furniture.rental.entity.Category;
import com.furniture.rental.entity.Furniture;
import com.furniture.rental.entity.FurnitureImage;
import com.furniture.rental.entity.Rental;
import com.furniture.rental.service.CategoryService;
import com.furniture.rental.service.FurnitureImageStorageService;
import com.furniture.rental.service.FurnitureService;
import com.furniture.rental.service.RentalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private FurnitureService furnitureService;

    @Autowired
    private RentalService rentalService;

    @Autowired
    private FurnitureImageStorageService furnitureImageStorageService;

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
    public String saveFurniture(@ModelAttribute Furniture form,
            @RequestParam("categoryId") Long categoryId,
            @RequestParam(value = "imageFiles", required = false) List<MultipartFile> uploads,
            RedirectAttributes redirectAttributes) {
        Furniture furniture = form;
        if (form.getId() != null) {
            furniture = furnitureService.getFurnitureById(form.getId());
            if (furniture == null) {
                return "redirect:/admin/furniture";
            }
            furniture.setName(form.getName());
            furniture.setDescription(form.getDescription());
            furniture.setRentalPricePerMonth(form.getRentalPricePerMonth());
            furniture.setPurchasePrice(form.getPurchasePrice());
            furniture.setStockQuantity(form.getStockQuantity());
        }

        Category category = categoryService.getCategoryById(categoryId);
        if (category == null) {
            redirectAttributes.addFlashAttribute("imageError", "Choose a valid furniture category.");
            return "redirect:/admin/furniture";
        }
        furniture.setCategory(category);
        furniture = furnitureService.saveFurniture(furniture);

        List<String> storedFiles = new ArrayList<>();
        try {
            if (uploads != null) {
                for (MultipartFile upload : uploads) {
                    if (upload == null || upload.isEmpty()) {
                        continue;
                    }
                    String imageUrl = furnitureImageStorageService.store(upload);
                    storedFiles.add(imageUrl);
                    FurnitureImage image = new FurnitureImage();
                    image.setImageUrl(imageUrl);
                    image.setFurniture(furniture);
                    if (furniture.getImages() == null) {
                        furniture.setImages(new ArrayList<>());
                    }
                    furniture.getImages().add(image);
                }
                if (!storedFiles.isEmpty()) {
                    furnitureService.saveFurniture(furniture);
                }
            }
        } catch (IOException | IllegalArgumentException exception) {
            for (String imageUrl : storedFiles) {
                try {
                    furnitureImageStorageService.deleteManagedImage(imageUrl);
                } catch (IOException ignored) {
                    // The unreferenced file can be removed from the upload folder later.
                }
            }
            redirectAttributes.addFlashAttribute("imageError", exception.getMessage());
            return "redirect:/admin/furniture/edit/" + furniture.getId();
        }
        return "redirect:/admin/furniture";
    }

    @PostMapping("/furniture/{furnitureId}/images/{imageId}/delete")
    public String deleteFurnitureImage(@PathVariable Long furnitureId, @PathVariable Long imageId,
            RedirectAttributes redirectAttributes) {
        Furniture furniture = furnitureService.getFurnitureById(furnitureId);
        if (furniture == null || furniture.getImages() == null) {
            return "redirect:/admin/furniture";
        }
        FurnitureImage image = furniture.getImages().stream()
                .filter(candidate -> imageId.equals(candidate.getId()))
                .findFirst()
                .orElse(null);
        if (image == null) {
            return "redirect:/admin/furniture/edit/" + furnitureId;
        }

        String imageUrl = image.getImageUrl();
        furniture.getImages().remove(image);
        furnitureService.saveFurniture(furniture);
        try {
            furnitureImageStorageService.deleteManagedImage(imageUrl);
        } catch (IOException exception) {
            redirectAttributes.addFlashAttribute("imageError", "Image removed from the product, but its file could not be deleted.");
        }
        return "redirect:/admin/furniture/edit/" + furnitureId;
    }

    // Rentals
    @GetMapping("/rentals")
    public String viewRentals(Model model) {
        model.addAttribute("rentals", rentalService.getAllRentals());
        return "admin/rentals";
    }

    // Delete Actions
    @PostMapping("/categories/delete/{id}")
    public String deleteCategory(@PathVariable Long id, RedirectAttributes redirectAttributes) {
    if (categoryService.hasFurniture(id)) {
        redirectAttributes.addFlashAttribute("categoryError",
                "This category still has furniture. Move or delete those items first.");
    } else {
        categoryService.deleteCategory(id);
        redirectAttributes.addFlashAttribute("categoryMessage", "Category deleted.");
    }
    return "redirect:/admin/categories";
    }

    @PostMapping("/furniture/delete/{id}")
    public String deleteFurniture(@PathVariable Long id, RedirectAttributes redirectAttributes) {
    Furniture furniture = furnitureService.getFurnitureById(id);
    if (furniture == null) {
        return "redirect:/admin/furniture";
    }
    if (furnitureService.isInRentals(id)) {
        redirectAttributes.addFlashAttribute("furnitureError",
                "This item is part of existing rentals and can't be deleted. Set its stock to 0 to mark it out of stock instead.");
        return "redirect:/admin/furniture";
    }

    List<String> imageUrls = furniture.getImages() == null ? List.of()
            : furniture.getImages().stream().map(FurnitureImage::getImageUrl).toList();
    furnitureService.deleteFurniture(id);
    for (String imageUrl : imageUrls) {
        try {
            furnitureImageStorageService.deleteManagedImage(imageUrl);
        } catch (IOException ignored) {
            // the leftover file can be removed from the upload folder later
        }
    }
    redirectAttributes.addFlashAttribute("furnitureMessage", "Furniture deleted.");
    return "redirect:/admin/furniture";
    }


    @PostMapping("/rentals/accept/{id}")
public String acceptRental(@PathVariable Long id) {
    rentalService.updateStatus(id, "ACTIVE");
    return "redirect:/admin/rentals";
}

@PostMapping("/rentals/cancel/{id}")
public String cancelRental(@PathVariable Long id, RedirectAttributes redirectAttributes) {
    Rental rental = rentalService.getRentalById(id);
    boolean paidOnline = rental != null && rental.getPayment() != null
            && "PAID".equals(rental.getPayment().getStatus());
    if (rentalService.cancelRental(id)) {
        redirectAttributes.addFlashAttribute("rentalMessage", paidOnline
                ? "Rental cancelled and stock restored. This order was paid online, so refund it manually in Razorpay."
                : "Rental cancelled and stock restored.");
    } else {
            redirectAttributes.addFlashAttribute("rentalError", "Only pending rentals can be cancelled.");    }
    return "redirect:/admin/rentals";
}
}
