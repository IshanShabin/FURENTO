package com.furniture.rental.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "furniture_image")
public class FurnitureImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String imageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "furniture_id")
    private Furniture furniture;

    public FurnitureImage() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public Furniture getFurniture() { return furniture; }
    public void setFurniture(Furniture furniture) { this.furniture = furniture; }
}
