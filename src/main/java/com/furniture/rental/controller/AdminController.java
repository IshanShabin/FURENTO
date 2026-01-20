package com.furniture.rental.controller;

import com.furniture.rental.entity.Category;
import com.furniture.rental.entity.Furniture;
import com.furniture.rental.service.CategoryService;
import com.furniture.rental.service.FurnitureService;
import com.furniture.rental.service.RentalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

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
        model.addAttribute("totalRentals", rentalService.getAllRentals().size());
        model.addAttribute("totalFurniture", furnitureService.getAllFurniture().size());
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

    @PostMapping("/categories/save")
    public String saveCategory(@ModelAttribute Category category) {
        categoryService.saveCategory(category);
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

    @PostMapping("/furniture/save")
    public String saveFurniture(@ModelAttribute Furniture furniture) {
        furnitureService.saveFurniture(furniture);
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
}
