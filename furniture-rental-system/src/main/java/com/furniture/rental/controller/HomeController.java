package com.furniture.rental.controller;

import com.furniture.rental.dto.FurnitureFilter;
import com.furniture.rental.entity.Furniture;
import com.furniture.rental.service.CategoryService;
import com.furniture.rental.service.FurnitureService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Controller
public class HomeController {

    private static final int PAGE_SIZE = 12;

    @Autowired
    private FurnitureService furnitureService;

    @Autowired
    private CategoryService categoryService;

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("trendingFurniture", furnitureService.getTrendingFurniture());
        model.addAttribute("categories", categoryService.getAllCategories());
        return "index";
    }

    @GetMapping("/furniture")
    public String browse(@ModelAttribute("filter") FurnitureFilter filter,
            BindingResult bindingResult, Model model) {
        // A BindingResult parameter makes invalid values (like minPrice=abc) be ignored
        // instead of failing.
        model.addAttribute("results", furnitureService.search(filter, PAGE_SIZE));
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("pageBaseUrl", pageBaseUrl(filter));
        return "furniture-list";
    }

    @GetMapping("/furniture/{id}")
    public String viewFurniture(@PathVariable Long id, Model model) {
        Furniture furniture = furnitureService.getFurnitureById(id);
        if (furniture == null) {
            return "redirect:/";
        }
        model.addAttribute("furniture", furniture);
        model.addAttribute("reviews",
                furniture.getReviews() == null ? java.util.Collections.emptyList() : furniture.getReviews());
        return "furniture-details";
    }

    @GetMapping("/about")
    public String about(Model model) {
        return "about";
    }

    @GetMapping("/category/{id}")
    public String viewCategory(@PathVariable Long id, Model model) {
        model.addAttribute("category", categoryService.getCategoryById(id));
        model.addAttribute("furnitureList", furnitureService.getFurnitureByCategory(id));
        return "category-products";
    }

    /**
     * The "/furniture?..." part of a page link, with only the active filters.
     * It always ends with "?" or "&" so the template can append "page=N".
     */
    static String pageBaseUrl(FurnitureFilter filter) {
        StringBuilder url = new StringBuilder("/furniture?");
        appendParam(url, "q", filter.getQ());
        appendParam(url, "category", filter.getCategory());
        appendParam(url, "minPrice", filter.getMinPrice());
        appendParam(url, "maxPrice", filter.getMaxPrice());
        if (Boolean.TRUE.equals(filter.getAvailableOnly())) {
            appendParam(url, "availableOnly", "true");
        }
        appendParam(url, "sort", filter.getSort());
        return url.toString();
    }

    private static void appendParam(StringBuilder url, String name, Object value) {
        if (value == null || value.toString().isBlank()) {
            return;
        }
        url.append(name).append('=')
                .append(URLEncoder.encode(value.toString(), StandardCharsets.UTF_8))
                .append('&');
    }
}
