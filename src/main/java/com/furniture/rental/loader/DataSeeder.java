package com.furniture.rental.loader;

import com.furniture.rental.entity.Category;
import com.furniture.rental.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    @Autowired
    private CategoryRepository categoryRepository;

    @Override
    public void run(String... args) throws Exception {
        if (categoryRepository.count() == 0) {
            List<Category> categories = Arrays.asList(
                    new Category("Living Room", "Comfortable sofas, coffee tables, and TV units for your living space.",
                            "https://images.unsplash.com/photo-1555041469-a586c61ea9bc?auto=format&fit=crop&w=800&q=80"),
                    new Category("Bedroom", "Cozy beds, efficient wardrobes, and bedside tables.",
                            "https://images.unsplash.com/photo-1505693416388-b0346ef4174b?auto=format&fit=crop&w=800&q=80"),
                    new Category("Dining Room", "Elegant dining tables and chairs for family meals.",
                            "https://images.unsplash.com/photo-1617806118233-18e1de247200?auto=format&fit=crop&w=800&q=80"),
                    new Category("Home Office", "Ergonomic desks and chairs for maximum productivity.",
                            "https://images.unsplash.com/photo-1593640408182-31c70c8268f5?auto=format&fit=crop&w=800&q=80"),
                    new Category("Appliances", "Essential home appliances like refrigerators and washing machines.",
                            "https://images.unsplash.com/photo-1571175443880-49e1d25b2bc5?auto=format&fit=crop&w=800&q=80"));

            categoryRepository.saveAll(categories);
            System.out.println("Default categories seeded successfully!");
        }
    }
}
