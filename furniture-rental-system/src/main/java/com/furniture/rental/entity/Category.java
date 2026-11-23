package com.furniture.rental.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

@Entity
@Table(name = "category")
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Category name is required.")
    @Size(max = 100, message = "Category name must be 100 characters or fewer.")
    @Column(nullable = false, unique = true)
    private String name;

    @Size(max = 255, message = "Description must be 255 characters or fewer.")
    private String description;

    @Pattern(regexp = "^$|^https?://\\S+", message = "Image URL must start with http:// or https://")
    @Size(max = 255, message = "Image URL must be 255 characters or fewer.")
    private String imageUrl;

    @OneToMany(mappedBy = "category")
    private List<Furniture> furnitures;

    public Category() {
    }

    public Category(String name, String description, String imageUrl) {
        this.name = name;
        this.description = description;
        this.imageUrl = imageUrl;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public List<Furniture> getFurnitures() {
        return furnitures;
    }

    public void setFurnitures(List<Furniture> furnitures) {
        this.furnitures = furnitures;
    }
}
