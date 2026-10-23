package com.furniture.rental.loader;

import com.furniture.rental.entity.Category;
import com.furniture.rental.entity.Furniture;
import com.furniture.rental.entity.FurnitureImage;
import com.furniture.rental.repository.CategoryRepository;
import com.furniture.rental.repository.FurnitureRepository;
import com.furniture.rental.repository.RentalItemRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    @Autowired
    private CategoryRepository categoryRepository;

        @Autowired
        private FurnitureRepository furnitureRepository;

        @Autowired
        private RentalItemRepository rentalItemRepository;

        @Autowired
        private ObjectMapper objectMapper;

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

                ensureCatalogCategories();
                seedFurniture();
                retireSofa();
        }

        private void ensureCatalogCategories() {
                List<Category> defaults = Arrays.asList(
                                new Category("Living Room", "Chairs, tables, and storage for shared spaces.", ""),
                                new Category("Bedroom", "Beds and bedside furniture.", ""),
                                new Category("Dining Room", "Tables and chairs for dining spaces.", ""),
                                new Category("Home Office", "Desks and furniture for focused work.", ""));

                for (Category category : defaults) {
                        if (categoryRepository.findByNameIgnoreCase(category.getName()).isEmpty()) {
                                categoryRepository.save(category);
                        }
                }
        }

        private void seedFurniture() throws Exception {
                JsonNode catalog = objectMapper.readTree(new ClassPathResource("furniture-catalog.json").getInputStream());
                int added = 0;

                for (JsonNode item : catalog) {
                        String name = item.path("name").asText();
                        if (furnitureRepository.existsByNameIgnoreCase(name)) {
                                continue;
                        }

                        Category category = categoryRepository.findByNameIgnoreCase(item.path("category").asText())
                                        .orElseThrow(() -> new IllegalStateException("Missing category for " + name));
                        Furniture furniture = new Furniture();
                        furniture.setName(name);
                        furniture.setDescription(item.path("description").asText());
                        furniture.setCategory(category);
                        furniture.setRentalPricePerMonth(new BigDecimal(item.path("rentalPricePerMonth").asText()));
                        furniture.setStockQuantity(item.path("stockQuantity").asInt());

                        FurnitureImage image = new FurnitureImage();
                        image.setImageUrl(item.path("imageUrl").asText());
                        image.setFurniture(furniture);
                        furniture.setImages(new ArrayList<>(List.of(image)));
                        furnitureRepository.save(furniture);
                        added++;
                }

                if (added > 0) {
                        System.out.println("Seeded " + added + " furniture catalog items.");
                }
        }

        private void retireSofa() {
                for (Furniture furniture : furnitureRepository.findByNameIgnoreCase("Sofa")) {
                        if (rentalItemRepository.existsByFurniture_Id(furniture.getId())) {
                                furniture.setStockQuantity(0);
                                furnitureRepository.save(furniture);
                        } else {
                                furnitureRepository.delete(furniture);
                        }
                }
    }
}
