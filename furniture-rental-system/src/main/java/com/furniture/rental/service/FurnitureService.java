package com.furniture.rental.service;

import com.furniture.rental.dto.FurnitureFilter;
import com.furniture.rental.entity.Furniture;
import com.furniture.rental.repository.FurnitureRepository;
import com.furniture.rental.repository.RentalItemRepository;

import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class FurnitureService {

    private static final char LIKE_ESCAPE = '!';

    @Autowired
    private FurnitureRepository furnitureRepository;

    public List<Furniture> getAllFurniture() {
        return furnitureRepository.findAll();
    }

    public List<Furniture> getFurnitureByCategory(Long categoryId) {
        return furnitureRepository.findByCategoryId(categoryId);
    }

    /** One page of furniture matching the filter, for the Browse Furniture page. */
    public Page<Furniture> search(FurnitureFilter filter, int size) {
        int page = filter.getPage() == null ? 0 : Math.max(filter.getPage(), 0);
        PageRequest pageRequest = PageRequest.of(page, size, toSort(filter.getSort()));
        return furnitureRepository.findAll(toSpecification(filter), pageRequest);
    }

    /** Only these named options are accepted, so users cannot sort by arbitrary columns. */
    static Sort toSort(String sort) {
        if (sort == null) {
            return Sort.by(Sort.Direction.DESC, "id");
        }
        return switch (sort) {
            case "priceAsc" -> Sort.by(Sort.Direction.ASC, "rentalPricePerMonth", "id");
            case "priceDesc" -> Sort.by(Sort.Direction.DESC, "rentalPricePerMonth", "id");
            case "name" -> Sort.by(Sort.Order.asc("name").ignoreCase(), Sort.Order.asc("id"));
            default -> Sort.by(Sort.Direction.DESC, "id");
        };
    }

    /** Escapes LIKE wildcards so a search for "50%" matches the text, not everything. */
    static String escapeLike(String text) {
        String escape = String.valueOf(LIKE_ESCAPE);
        return text.replace(escape, escape + escape)
                .replace("%", escape + "%")
                .replace("_", escape + "_");
    }

    private Specification<Furniture> toSpecification(FurnitureFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.getQ() != null && !filter.getQ().isBlank()) {
                String pattern = "%" + escapeLike(filter.getQ().trim().toLowerCase(Locale.ROOT)) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.<String>get("name")), pattern, LIKE_ESCAPE),
                        cb.like(cb.lower(root.<String>get("description")), pattern, LIKE_ESCAPE)));
            }
            if (filter.getCategory() != null) {
                predicates.add(cb.equal(root.get("category").get("id"), filter.getCategory()));
            }
            if (filter.getMinPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.<BigDecimal>get("rentalPricePerMonth"), filter.getMinPrice()));
            }
            if (filter.getMaxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(
                        root.<BigDecimal>get("rentalPricePerMonth"), filter.getMaxPrice()));
            }
            if (Boolean.TRUE.equals(filter.getAvailableOnly())) {
                predicates.add(cb.isTrue(root.<Boolean>get("available")));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public List<Furniture> getTrendingFurniture() {
        List<Furniture> newestAvailable = getAllFurniture().stream()
                .filter(Furniture::isAvailable)
                .sorted(Comparator.comparing(Furniture::getId).reversed())
                .toList();
        List<Furniture> trending = new ArrayList<>(6);

        addNewestByCategory(newestAvailable, trending, "Living Room", 1);
        addNewestByCategory(newestAvailable, trending, "Bedroom", 2);
        addNewestByCategory(newestAvailable, trending, "Dining Room", 1);
        addNewestByCategory(newestAvailable, trending, "Home Office", 1);
        addNewestByCategory(newestAvailable, trending, "Office Room", 1);
        return trending;
    }

    private void addNewestByCategory(List<Furniture> available, List<Furniture> trending,
            String categoryName, int limit) {
        for (Furniture item : available) {
            if (item.getCategory() != null && categoryName.equalsIgnoreCase(item.getCategory().getName())) {
                trending.add(item);
                if (--limit == 0) {
                    return;
                }
            }
        }
    }

    public Furniture getFurnitureById(Long id) {
        return furnitureRepository.findById(id).orElse(null);
    }

    public Furniture saveFurniture(Furniture furniture) {
        // Update availability logic
        furniture.setAvailable(furniture.getStockQuantity() > 0);
        return furnitureRepository.save(furniture);
    }

    public void deleteFurniture(Long id) {
        furnitureRepository.deleteById(id);
    }

    @Autowired
    private RentalItemRepository rentalItemRepository;

    public boolean isInRentals(Long furnitureId) {
        return rentalItemRepository.existsByFurniture_Id(furnitureId);
    }
}
