package com.furniture.rental.controller;

import com.furniture.rental.config.SecurityConfig;
import com.furniture.rental.dto.Cart;
import com.furniture.rental.dto.CartItem;
import com.furniture.rental.repository.RevokedTokenRepository;
import com.furniture.rental.repository.UserRepository;
import com.furniture.rental.security.JwtAuthenticationFilter;
import com.furniture.rental.security.JwtService;
import com.furniture.rental.service.CustomUserDetailsService;
import com.furniture.rental.service.FurnitureService;
import com.furniture.rental.service.RazorpayService;
import com.furniture.rental.service.RentalService;
import com.furniture.rental.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static com.furniture.rental.controller.CartTestSupport.CHAIR_ID;
import static com.furniture.rental.controller.CartTestSupport.cartWithChair;
import static com.furniture.rental.controller.CartTestSupport.chair;
import static com.furniture.rental.controller.CartTestSupport.customer;
import static com.furniture.rental.controller.CartTestSupport.sessionWith;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/** Cart pages and actions, plus who is allowed to use them. */
@WebMvcTest(controllers = CartController.class)
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class })
@TestPropertySource(properties = {
        "JWT_SECRET=test-secret-test-secret-test-secret-1234",
        "app.jwt.cookie-secure=false"
})
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private RevokedTokenRepository revokedTokenRepository;

    @MockBean
    private FurnitureService furnitureService;

    @MockBean
    private RentalService rentalService;

    @MockBean
    private UserService userService;

    @MockBean
    private RazorpayService razorpayService;

    private Cart cartOf(MockHttpSession session) {
        return (Cart) session.getAttribute("cart");
    }

    // ---- adding ----

    @Test
    void addingAnItemPutsItInTheCart() throws Exception {
        when(furnitureService.getFurnitureById(CHAIR_ID)).thenReturn(chair("349.00", 3));
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/cart/add").with(customer()).with(csrf()).session(session)
                .param("furnitureId", "10").param("quantity", "2").param("months", "3"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart"));

        CartItem item = cartOf(session).getItems().get(0);
        assertEquals(2, item.getQuantity());
        assertEquals(3, item.getRentalPeriodMonths());
        assertEquals(0, new BigDecimal("349.00").compareTo(item.getPricePerMonth()));
    }

    @Test
    void addingMoreThanTheStockIsRefused() throws Exception {
        when(furnitureService.getFurnitureById(CHAIR_ID)).thenReturn(chair("349.00", 3));
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/cart/add").with(customer()).with(csrf()).session(session)
                .param("furnitureId", "10").param("quantity", "4").param("months", "1"))
                .andExpect(redirectedUrl("/cart"))
                .andExpect(flash().attribute("cartError", "Requested quantity exceeds available stock."));

        Cart cart = cartOf(session);
        assertTrue(cart == null || cart.getItems().isEmpty());
    }

    @Test
    void addingWithAnInvalidQuantityIsRefused() throws Exception {
        when(furnitureService.getFurnitureById(CHAIR_ID)).thenReturn(chair("349.00", 3));

        mockMvc.perform(post("/cart/add").with(customer()).with(csrf())
                .param("furnitureId", "10").param("quantity", "0").param("months", "1"))
                .andExpect(redirectedUrl("/cart"))
                .andExpect(flash().attribute("cartError",
                        "Choose an available item and a valid quantity and duration."));
    }

    // ---- changing quantity and removing ----

    @Test
    void increasingStopsAtTheStockLimit() throws Exception {
        when(furnitureService.getFurnitureById(CHAIR_ID)).thenReturn(chair("349.00", 3));
        MockHttpSession session = sessionWith(cartWithChair("349.00", 3, 1));

        mockMvc.perform(post("/cart/update/0").with(customer()).with(csrf()).session(session)
                .param("action", "increase"))
                .andExpect(redirectedUrl("/cart"))
                .andExpect(flash().attribute("cartError", "Requested quantity exceeds available stock."));

        assertEquals(3, cartOf(session).getItems().get(0).getQuantity());
    }

    @Test
    void increasingAndDecreasingChangeTheQuantityButNeverBelowOne() throws Exception {
        when(furnitureService.getFurnitureById(CHAIR_ID)).thenReturn(chair("349.00", 5));
        MockHttpSession session = sessionWith(cartWithChair("349.00", 1, 1));

        mockMvc.perform(post("/cart/update/0").with(customer()).with(csrf()).session(session)
                .param("action", "increase"));
        assertEquals(2, cartOf(session).getItems().get(0).getQuantity());

        mockMvc.perform(post("/cart/update/0").with(customer()).with(csrf()).session(session)
                .param("action", "decrease"));
        mockMvc.perform(post("/cart/update/0").with(customer()).with(csrf()).session(session)
                .param("action", "decrease"));
        assertEquals(1, cartOf(session).getItems().get(0).getQuantity());
    }

    @Test
    void removingTakesTheItemOutOfTheCart() throws Exception {
        MockHttpSession session = sessionWith(cartWithChair("349.00", 1, 1));

        mockMvc.perform(post("/cart/remove/0").with(customer()).with(csrf()).session(session))
                .andExpect(redirectedUrl("/cart"));

        assertTrue(cartOf(session).getItems().isEmpty());
    }

    @Test
    void removingWithAPlainLinkIsNotAllowed() throws Exception {
        MockHttpSession session = sessionWith(cartWithChair("349.00", 1, 1));

        mockMvc.perform(get("/cart/remove/0").with(customer()).session(session))
                .andExpect(status().isMethodNotAllowed());

        assertEquals(1, cartOf(session).getItems().size());
    }

    // ---- pages ----

    @Test
    void viewingTheCartRefreshesStalePricesFromTheDatabase() throws Exception {
        when(furnitureService.getFurnitureById(CHAIR_ID)).thenReturn(chair("349.00", 3));
        MockHttpSession session = sessionWith(cartWithChair("100.00", 1, 3));

        mockMvc.perform(get("/cart").with(customer()).session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("cart"))
                .andExpect(content().string(containsString("1,099.35")));

        assertEquals(0, new BigDecimal("349.00")
                .compareTo(cartOf(session).getItems().get(0).getPricePerMonth()));
    }

    @Test
    void itemsThatNoLongerExistDropOutOfTheCart() throws Exception {
        when(furnitureService.getFurnitureById(CHAIR_ID)).thenReturn(null);
        MockHttpSession session = sessionWith(cartWithChair("349.00", 1, 1));

        mockMvc.perform(get("/cart").with(customer()).session(session))
                .andExpect(status().isOk());

        assertTrue(cartOf(session).getItems().isEmpty());
    }

    @Test
    void checkoutPageShowsTheCurrentTotal() throws Exception {
        when(furnitureService.getFurnitureById(CHAIR_ID)).thenReturn(chair("349.00", 3));
        when(razorpayService.isConfigured()).thenReturn(false);
        MockHttpSession session = sessionWith(cartWithChair("100.00", 1, 3));

        mockMvc.perform(get("/cart/checkout").with(customer()).session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("checkout"))
                .andExpect(content().string(containsString("1,099.35")));
    }

    @Test
    void checkoutWithAnEmptyCartGoesBackToTheCart() throws Exception {
        mockMvc.perform(get("/cart/checkout").with(customer()).session(new MockHttpSession()))
                .andExpect(redirectedUrl("/cart"));
    }

    // ---- who may use the cart ----

    @Test
    void visitorsAreSentToTheLoginPage() throws Exception {
        mockMvc.perform(get("/cart"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void adminsCannotUseTheCart() throws Exception {
        mockMvc.perform(get("/cart").with(user("admin").roles("ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test
    void postsWithoutACsrfTokenAreRejected() throws Exception {
        when(furnitureService.getFurnitureById(CHAIR_ID)).thenReturn(chair("349.00", 3));
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/cart/add").with(customer()).session(session)
                .param("furnitureId", "10").param("quantity", "1").param("months", "1"))
                .andExpect(status().isForbidden());

        assertNull(cartOf(session));
    }
}
