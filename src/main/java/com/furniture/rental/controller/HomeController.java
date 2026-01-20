package com.furniture.rental.controller;

import com.furniture.rental.service.CategoryService;
import com.furniture.rental.service.FurnitureService;
import com.furniture.rental.entity.Furniture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class HomeController {

    @Autowired
    private FurnitureService furnitureService;

    @Autowired
    private CategoryService categoryService;

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("furnitureList", furnitureService.getAllFurniture());
        model.addAttribute("categories", categoryService.getAllCategories());
        return "index";
    }

    @GetMapping("/furniture/{id}")
    public String viewFurniture(@PathVariable Long id, Model model) {
        Furniture furniture = furnitureService.getFurnitureById(id);
        model.addAttribute("furniture", furniture);
        return "furniture-details";
    }

    @GetMapping("/about")
    public String about(Model model) {
        return "about";
    }
}
