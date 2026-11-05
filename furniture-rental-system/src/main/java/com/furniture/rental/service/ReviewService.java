package com.furniture.rental.service;

import com.furniture.rental.entity.Furniture;
import com.furniture.rental.entity.Review;
import com.furniture.rental.entity.User;
import com.furniture.rental.repository.FurnitureRepository;
import com.furniture.rental.repository.ReviewRepository;
import com.furniture.rental.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReviewService {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private FurnitureRepository furnitureRepository;

    @Autowired
    private UserRepository userRepository;

    public Review submitReview(Long furnitureId, String username, int rating, String comment) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }

        Furniture furniture = furnitureRepository.findById(furnitureId)
                .orElseThrow(() -> new IllegalArgumentException("Furniture not found"));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (reviewRepository.existsByFurnitureIdAndUserId(furnitureId, user.getId())) {
            throw new IllegalArgumentException("You have already reviewed this product");
        }

        Review review = new Review();
        review.setFurniture(furniture);
        review.setUser(user);
        review.setRating(rating);
        review.setComment(comment == null ? "" : comment.trim());
        return reviewRepository.save(review);
    }

    public List<Review> getReviewsForFurniture(Long furnitureId) {
        return reviewRepository.findByFurnitureIdOrderByCreatedAtDesc(furnitureId);
    }
}
