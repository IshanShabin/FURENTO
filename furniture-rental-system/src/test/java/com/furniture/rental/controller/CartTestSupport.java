package com.furniture.rental.controller;

import com.furniture.rental.dto.Cart;
import com.furniture.rental.dto.CartItem;
import com.furniture.rental.entity.Furniture;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

/** Shared builders for the cart and checkout web tests. */
final class CartTestSupport {

    static final long CHAIR_ID = 10L;

    private CartTestSupport() {
    }

    /** A chair as it is in the database right now. */
    static Furniture chair(String price, int stock) {
        Furniture furniture = new Furniture();
        furniture.setId(CHAIR_ID);
        furniture.setName("Wicker Chair");
        furniture.setRentalPricePerMonth(new BigDecimal(price));
        furniture.setStockQuantity(stock);
        return furniture;
    }

    /** A cart holding the chair, with the price the customer saw when adding it. */
    static Cart cartWithChair(String priceInCart, int quantity, int months) {
        Cart cart = new Cart();
        cart.addItem(new CartItem(CHAIR_ID, "Wicker Chair", new BigDecimal(priceInCart), quantity, months));
        return cart;
    }

    static MockHttpSession sessionWith(Cart cart) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("cart", cart);
        return session;
    }

    static RequestPostProcessor customer() {
        return customer("jaseem");
    }

    static RequestPostProcessor customer(String username) {
        return user(username).roles("CUSTOMER");
    }
}
