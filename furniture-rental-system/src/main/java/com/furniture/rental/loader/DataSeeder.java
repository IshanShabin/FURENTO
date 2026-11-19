package com.furniture.rental.loader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.furniture.rental.entity.Category;
import com.furniture.rental.entity.Furniture;
import com.furniture.rental.entity.FurnitureImage;
import com.furniture.rental.repository.CategoryRepository;
import com.furniture.rental.repository.FurnitureRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Seeds the sample categories and furniture catalog, but only into empty
 * tables.
 * It never overwrites, renames or re-creates anything an admin has changed or
 * deleted.
 */
@Component
public class DataSeeder implements CommandLineRunner {

        private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

        private static final String HOME_OFFICE_DESCRIPTION = "Ergonomic desks and chairs for comfortable, focused work at home.";
        private static final String OFFICE_ROOM_DESCRIPTION = "Functional office furniture for meetings, shared workspaces, and everyday organization.";
        private static final String OFFICE_ROOM_IMAGE = "https://images.unsplash.com/photo-1758630737361-ca7532fb5e7f?auto=format&fit=crop&w=900&q=80";

        @Autowired
        private CategoryRepository categoryRepository;

        @Autowired
        private FurnitureRepository furnitureRepository;

        @Autowired
        private ObjectMapper objectMapper;

        @Override
        public void run(String... args) throws Exception {
                seedCategories();
                seedFurniture();
        }

        private void seedCategories() {
                if (categoryRepository.count() > 0) {
                        return;
                }

                List<Category> categories = Arrays.asList(
                                new Category("Living Room",
                                                "Comfortable sofas, coffee tables, and TV units for your living space.",
                                                "https://images.unsplash.com/photo-1555041469-a586c61ea9bc?auto=format&fit=crop&w=800&q=80"),
                                new Category("Bedroom", "Cozy beds, efficient wardrobes, and bedside tables.",
                                                "https://images.unsplash.com/photo-1617325247661-675ab4b64ae2?q=80&w=800&auto=format&fit=crop"),
                                new Category("Dining Room",
                                                "Elegant dining tables and chairs for family meals.",
                                                "https://images.unsplash.com/photo-1617806118233-18e1de247200?auto=format&fit=crop&w=800&q=80"),
                                new Category("Home Office",
                                                HOME_OFFICE_DESCRIPTION,
                                                "https://images.unsplash.com/photo-1593640408182-31c70c8268f5?auto=format&fit=crop&w=800&q=80"),
                                new Category("Office Room", OFFICE_ROOM_DESCRIPTION, OFFICE_ROOM_IMAGE));

                categoryRepository.saveAll(categories);
                log.info("Default categories seeded.");
        }

        private void seedFurniture() throws Exception {
                if (furnitureRepository.count() > 0) {
                        return;
                }

                JsonNode catalog;
                try (InputStream input = new ClassPathResource("furniture-catalog.json").getInputStream()) {
                        catalog = objectMapper.readTree(input);
                }

                int added = 0;
                for (JsonNode item : catalog) {
                        String name = item.path("name").asText();
                        if (furnitureRepository.existsByNameIgnoreCase(name)) {
                                continue;
                        }

                        String categoryName = item.path("category").asText();
                        Category category = categoryRepository.findByNameIgnoreCase(categoryName).orElse(null);
                        if (category == null) {
                                log.warn("Skipping '{}': category '{}' not found.", name, categoryName);
                                continue;
                        }

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
                        log.info("Seeded {} furniture catalog items.", added);
                }
        }
}
