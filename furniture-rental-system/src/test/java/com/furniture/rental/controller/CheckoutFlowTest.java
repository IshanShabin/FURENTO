package com.furniture.rental.controller;

import com.furniture.rental.config.SecurityConfig;
import com.furniture.rental.entity.Rental;
import com.furniture.rental.entity.User;
import com.furniture.rental.repository.RevokedTokenRepository;
import com.furniture.rental.repository.UserRepository;
import com.furniture.rental.security.JwtAuthenticationFilter;
import com.furniture.rental.security.JwtService;
import com.furniture.rental.service.CustomUserDetailsService;
import com.furniture.rental.service.FurnitureService;
import com.furniture.rental.service.RazorpayService;
import com.furniture.rental.service.RazorpayService.GatewayOrder;
import com.furniture.rental.service.RazorpayService.GatewayPayment;
import com.furniture.rental.service.RentalService;
import com.furniture.rental.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.client.RestClientException;

import static com.furniture.rental.controller.CartTestSupport.CHAIR_ID;
import static com.furniture.rental.controller.CartTestSupport.cartWithChair;
import static com.furniture.rental.controller.CartTestSupport.chair;
import static com.furniture.rental.controller.CartTestSupport.customer;
import static com.furniture.rental.controller.CartTestSupport.sessionWith;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cash on delivery and Razorpay checkout.
 * The test cart holds 1 chair for 3 months at 349.00: subtotal 1047.00, tax
 * 52.35, total 1099.35,
 * which is 109935 paise.
 */
@WebMvcTest(controllers = CartController.class)
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class })
@TestPropertySource(properties = {
        "JWT_SECRET=test-secret-test-secret-test-secret-1234",
        "app.jwt.cookie-secure=false"
})
class CheckoutFlowTest {

    private static final long TOTAL_PAISE = 109935L;

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

    private User jaseem;

    @BeforeEach
    void setUp() {
        jaseem = new User();
        jaseem.setUsername("jaseem");
        when(userService.findByUsername("jaseem")).thenReturn(jaseem);
        when(furnitureService.getFurnitureById(CHAIR_ID)).thenReturn(chair("349.00", 3));
    }

    // ---------------- cash on delivery ----------------

    private ResultActions placeCodOrder(MockHttpSession session, String paymentMethod) throws Exception {
        return mockMvc.perform(post("/cart/process-checkout").with(customer()).with(csrf()).session(session)
                .param("addressLine1", "1 Main Road")
                .param("addressLine2", "")
                .param("city", "Malappuram")
                .param("state", "Kerala")
                .param("zipCode", "676503")
                .param("paymentMethod", paymentMethod));
    }

    @Test
    void codCheckoutCreatesAPendingRentalAndClearsTheCart() throws Exception {
        MockHttpSession session = sessionWith(cartWithChair("349.00", 1, 3));

        placeCodOrder(session, "COD")
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/customer/dashboard"));

        ArgumentCaptor<Rental> captor = ArgumentCaptor.forClass(Rental.class);
        verify(rentalService).createRental(captor.capture());
        Rental rental = captor.getValue();
        assertSame(jaseem, rental.getUser());
        assertEquals("COD", rental.getPayment().getPaymentMethod());
        assertEquals("PENDING", rental.getPayment().getStatus());
        assertEquals("PENDING", rental.getDelivery().getStatus());
        assertEquals("1 Main Road", rental.getDelivery().getAddressLine1());
        assertEquals("Malappuram", rental.getDelivery().getCity());
        assertEquals(1, rental.getItems().size());
        assertEquals(1, rental.getItems().get(0).getQuantity());
        assertEquals(3, rental.getItems().get(0).getRentalPeriodMonths());
        assertNull(session.getAttribute("cart"));
    }

    @Test
    void codCheckoutWithAnEmptyCartCreatesNothing() throws Exception {
        placeCodOrder(new MockHttpSession(), "COD")
                .andExpect(redirectedUrl("/cart"));

        verify(rentalService, never()).createRental(any());
    }

    @Test
    void codCheckoutIsRefusedWhenTheStockRanOut() throws Exception {
        when(furnitureService.getFurnitureById(CHAIR_ID)).thenReturn(chair("349.00", 0));
        MockHttpSession session = sessionWith(cartWithChair("349.00", 1, 3));

        placeCodOrder(session, "COD")
                .andExpect(redirectedUrl("/cart"))
                .andExpect(flash().attribute("cartError",
                        "Requested quantity exceeds available stock for Wicker Chair"));

        verify(rentalService, never()).createRental(any());
        assertNotNull(session.getAttribute("cart"));
    }

    @Test
    void anOnlineMethodSentToTheCodEndpointGoesBackToCheckout() throws Exception {
        MockHttpSession session = sessionWith(cartWithChair("349.00", 1, 3));

        placeCodOrder(session, "RAZORPAY")
                .andExpect(redirectedUrl("/cart/checkout"));

        verify(rentalService, never()).createRental(any());
    }

    // ---------------- starting an online payment ----------------

    private ResultActions createPaymentOrder(MockHttpSession session) throws Exception {
        return mockMvc.perform(post("/cart/create-payment-order").with(customer()).with(csrf()).session(session)
                .param("addressLine1", "1 Main Road")
                .param("city", "Malappuram")
                .param("state", "Kerala")
                .param("zipCode", "676503"));
    }

    private void razorpayIsConfigured() {
        when(razorpayService.isConfigured()).thenReturn(true);
        when(razorpayService.getKeyId()).thenReturn("rzp_test_key");
    }

    @Test
    void startingPaymentWithAnEmptyCartIsRefused() throws Exception {
        razorpayIsConfigured();

        createPaymentOrder(new MockHttpSession())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Your cart is empty."));
    }

    @Test
    void startingPaymentWithoutRazorpayKeysIsRefused() throws Exception {
        when(razorpayService.isConfigured()).thenReturn(false);

        createPaymentOrder(sessionWith(cartWithChair("349.00", 1, 3)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("Online payments are not configured."));
    }

    @Test
    void theAmountComesFromDatabasePricesNotTheCart() throws Exception {
        razorpayIsConfigured();
        when(razorpayService.createOrder(TOTAL_PAISE)).thenReturn(new GatewayOrder("order_1", TOTAL_PAISE, "INR"));
        // The cart still shows an old price of 100.00, but the database price is
        // 349.00.
        MockHttpSession session = sessionWith(cartWithChair("100.00", 1, 3));

        createPaymentOrder(session)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keyId").value("rzp_test_key"))
                .andExpect(jsonPath("$.orderId").value("order_1"))
                .andExpect(jsonPath("$.amount").value(109935))
                .andExpect(jsonPath("$.currency").value("INR"));

        verify(razorpayService).createOrder(TOTAL_PAISE);
    }

    @Test
    void aGatewayOrderWithTheWrongAmountIsRejected() throws Exception {
        razorpayIsConfigured();
        when(razorpayService.createOrder(anyLong())).thenReturn(new GatewayOrder("order_1", 1L, "INR"));

        createPaymentOrder(sessionWith(cartWithChair("349.00", 1, 3)))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("The payment gateway returned an invalid order."));
    }

    @Test
    void aGatewayFailureWhileStartingPaymentGivesAClearError() throws Exception {
        razorpayIsConfigured();
        when(razorpayService.createOrder(anyLong())).thenThrow(new RestClientException("gateway down"));

        createPaymentOrder(sessionWith(cartWithChair("349.00", 1, 3)))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value(startsWith("Unable to start payment.")));
    }

    // ---------------- confirming an online payment ----------------

    /**
     * Starts a payment the normal way, so the session holds a genuine pending
     * checkout.
     */
    private MockHttpSession startedPayment() throws Exception {
        razorpayIsConfigured();
        when(razorpayService.createOrder(TOTAL_PAISE)).thenReturn(new GatewayOrder("order_1", TOTAL_PAISE, "INR"));
        MockHttpSession session = sessionWith(cartWithChair("349.00", 1, 3));
        createPaymentOrder(session).andExpect(status().isOk());
        return session;
    }

    private ResultActions confirm(MockHttpSession session, String orderId) throws Exception {
        return confirmAs("jaseem", session, orderId);
    }

    private ResultActions confirmAs(String username, MockHttpSession session, String orderId) throws Exception {
        return mockMvc.perform(post("/cart/confirm-payment").with(customer(username)).with(csrf()).session(session)
                .param("razorpay_order_id", orderId)
                .param("razorpay_payment_id", "pay_1")
                .param("razorpay_signature", "sig"));
    }

    private void signatureIsValid() {
        when(razorpayService.verifySignature("order_1", "pay_1", "sig")).thenReturn(true);
    }

    private GatewayPayment gatewayPayment(String orderId, String status, long amountPaise) {
        return new GatewayPayment(orderId, status, "upi", amountPaise);
    }

    @Test
    void confirmingWithNoStartedPaymentIsRefused() throws Exception {
        confirm(new MockHttpSession(), "order_1")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("This payment attempt is no longer valid."));

        verify(rentalService, never()).createRental(any());
    }

    @Test
    void confirmingAnotherOrderIdIsRefused() throws Exception {
        MockHttpSession session = startedPayment();

        confirm(session, "order_other")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("This payment attempt is no longer valid."));

        verify(razorpayService, never()).verifySignature(anyString(), anyString(), anyString());
        verify(rentalService, never()).createRental(any());
    }

    @Test
    void anotherCustomerCannotConfirmSomeoneElsesPayment() throws Exception {
        MockHttpSession original = startedPayment();
        MockHttpSession other = new MockHttpSession();
        other.setAttribute("pendingCheckout", original.getAttribute("pendingCheckout"));
        signatureIsValid();

        confirmAs("mallory", other, "order_1")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("This payment attempt is no longer valid."));

        verify(rentalService, never()).createRental(any());
    }

    @Test
    void aBadSignatureIsRefused() throws Exception {
        MockHttpSession session = startedPayment();
        when(razorpayService.verifySignature("order_1", "pay_1", "sig")).thenReturn(false);

        confirm(session, "order_1")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Payment verification failed."));

        verify(razorpayService, never()).fetchPayment(anyString());
        verify(rentalService, never()).createRental(any());
    }

    @Test
    void aPaymentForADifferentAmountIsRefused() throws Exception {
        MockHttpSession session = startedPayment();
        signatureIsValid();
        when(razorpayService.fetchPayment("pay_1")).thenReturn(gatewayPayment("order_1", "captured", 100L));

        confirm(session, "order_1")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("The payment details do not match this order."));

        verify(rentalService, never()).createRental(any());
    }

    @Test
    void aPaymentBelongingToAnotherGatewayOrderIsRefused() throws Exception {
        MockHttpSession session = startedPayment();
        signatureIsValid();
        when(razorpayService.fetchPayment("pay_1"))
                .thenReturn(gatewayPayment("order_other", "captured", TOTAL_PAISE));

        confirm(session, "order_1")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("The payment details do not match this order."));

        verify(rentalService, never()).createRental(any());
    }

    @Test
    void ifThePriceChangedNothingIsCapturedAndNoRentalIsCreated() throws Exception {
        MockHttpSession session = startedPayment();
        signatureIsValid();
        when(razorpayService.fetchPayment("pay_1")).thenReturn(gatewayPayment("order_1", "authorized", TOTAL_PAISE));
        // An admin raises the price after the customer started paying.
        when(furnitureService.getFurnitureById(CHAIR_ID)).thenReturn(chair("399.00", 3));

        confirm(session, "order_1")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("Prices or stock changed")))
                .andExpect(jsonPath("$.message").value(containsString("pay_1")));

        verify(razorpayService, never()).capturePayment(anyString(), anyLong());
        verify(rentalService, never()).createRental(any());
    }

    @Test
    void ifTheStockRanOutNothingIsCapturedAndNoRentalIsCreated() throws Exception {
        MockHttpSession session = startedPayment();
        signatureIsValid();
        when(razorpayService.fetchPayment("pay_1")).thenReturn(gatewayPayment("order_1", "authorized", TOTAL_PAISE));
        when(furnitureService.getFurnitureById(CHAIR_ID)).thenReturn(chair("349.00", 0));

        confirm(session, "order_1")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(startsWith("Stock changed during checkout")))
                .andExpect(jsonPath("$.message").value(containsString("pay_1")));

        verify(razorpayService, never()).capturePayment(anyString(), anyLong());
        verify(rentalService, never()).createRental(any());
    }

    @Test
    void anAuthorizedPaymentIsCapturedAndThenTheRentalIsCreated() throws Exception {
        MockHttpSession session = startedPayment();
        signatureIsValid();
        when(razorpayService.fetchPayment("pay_1")).thenReturn(gatewayPayment("order_1", "authorized", TOTAL_PAISE));
        when(razorpayService.capturePayment("pay_1", TOTAL_PAISE))
                .thenReturn(gatewayPayment("order_1", "captured", TOTAL_PAISE));

        confirm(session, "order_1")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.redirectUrl").value("/customer/dashboard"));

        verify(razorpayService).capturePayment("pay_1", TOTAL_PAISE);
        ArgumentCaptor<Rental> captor = ArgumentCaptor.forClass(Rental.class);
        verify(rentalService).createRental(captor.capture());
        Rental rental = captor.getValue();
        assertSame(jaseem, rental.getUser());
        assertEquals("UPI", rental.getPayment().getPaymentMethod());
        assertEquals("PAID", rental.getPayment().getStatus());
        assertEquals("order_1", rental.getPayment().getGatewayOrderId());
        assertEquals("pay_1", rental.getPayment().getGatewayPaymentId());
        assertEquals("Malappuram", rental.getDelivery().getCity());
        assertNull(session.getAttribute("cart"));
        assertNull(session.getAttribute("pendingCheckout"));
    }

    @Test
    void anAlreadyCapturedPaymentIsNotCapturedAgain() throws Exception {
        MockHttpSession session = startedPayment();
        signatureIsValid();
        when(razorpayService.fetchPayment("pay_1")).thenReturn(gatewayPayment("order_1", "captured", TOTAL_PAISE));

        confirm(session, "order_1").andExpect(status().isOk());

        verify(razorpayService, never()).capturePayment(anyString(), anyLong());
        verify(rentalService).createRental(any());
    }

    @Test
    void aPaymentThatIsNotCapturedCreatesNoRental() throws Exception {
        MockHttpSession session = startedPayment();
        signatureIsValid();
        when(razorpayService.fetchPayment("pay_1")).thenReturn(gatewayPayment("order_1", "failed", TOTAL_PAISE));

        confirm(session, "order_1")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("The payment has not been captured."));

        verify(rentalService, never()).createRental(any());
    }

    @Test
    void aConfirmedPaymentCannotBeReplayed() throws Exception {
        MockHttpSession session = startedPayment();
        signatureIsValid();
        when(razorpayService.fetchPayment("pay_1")).thenReturn(gatewayPayment("order_1", "captured", TOTAL_PAISE));

        confirm(session, "order_1").andExpect(status().isOk());
        confirm(session, "order_1")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("This payment attempt is no longer valid."));

        verify(rentalService, times(1)).createRental(any());
    }

    @Test
    void aGatewayFailureWhileConfirmingGivesAClearError() throws Exception {
        MockHttpSession session = startedPayment();
        signatureIsValid();
        when(razorpayService.fetchPayment("pay_1")).thenThrow(new RestClientException("gateway down"));

        confirm(session, "order_1")
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value(startsWith("Unable to verify payment")));

        verify(rentalService, never()).createRental(any());
    }

    // ---------------- delivery address checks ----------------

    private ResultActions placeCodOrderWithAddress(MockHttpSession session, String line1, String city,
            String state, String zip) throws Exception {
        return mockMvc.perform(post("/cart/process-checkout").with(customer()).with(csrf()).session(session)
                .param("addressLine1", line1)
                .param("addressLine2", "")
                .param("city", city)
                .param("state", state)
                .param("zipCode", zip)
                .param("paymentMethod", "COD"));
    }

    @Test
    void codCheckoutWithMissingAddressPartsGoesBackToCheckoutWithMessages() throws Exception {
        MockHttpSession session = sessionWith(cartWithChair("349.00", 1, 3));

        placeCodOrderWithAddress(session, "   ", "   ", "", "676503")
                .andExpect(redirectedUrl("/cart/checkout"))
                .andExpect(flash().attribute("addressErrors", hasItem("Address line 1 is required.")))
                .andExpect(flash().attribute("addressErrors", hasItem("City is required.")))
                .andExpect(flash().attribute("addressErrors", hasItem("State is required.")));

        verify(rentalService, never()).createRental(any());
        assertNotNull(session.getAttribute("cart"));
    }

    @Test
    void codCheckoutRejectsPinCodesThatAreNotSixDigits() throws Exception {
        for (String pin : new String[] { "12345", "012345", "67650A", "6765031", "" }) {
            MockHttpSession session = sessionWith(cartWithChair("349.00", 1, 3));

            placeCodOrderWithAddress(session, "1 Main Road", "Malappuram", "Kerala", pin)
                    .andExpect(redirectedUrl("/cart/checkout"));
        }

        verify(rentalService, never()).createRental(any());
    }

    @Test
    void theAddressIsTrimmedBeforeItIsSaved() throws Exception {
        MockHttpSession session = sessionWith(cartWithChair("349.00", 1, 3));

        placeCodOrderWithAddress(session, "  1 Main Road  ", " Malappuram ", "Kerala", " 676503 ")
                .andExpect(redirectedUrl("/customer/dashboard"));

        ArgumentCaptor<Rental> captor = ArgumentCaptor.forClass(Rental.class);
        verify(rentalService).createRental(captor.capture());
        assertEquals("1 Main Road", captor.getValue().getDelivery().getAddressLine1());
        assertEquals("Malappuram", captor.getValue().getDelivery().getCity());
        assertEquals("676503", captor.getValue().getDelivery().getZipCode());
    }

    @Test
    void startingPaymentWithAnIncompleteAddressIsRefused() throws Exception {
        razorpayIsConfigured();

        mockMvc.perform(post("/cart/create-payment-order").with(customer()).with(csrf())
                .session(sessionWith(cartWithChair("349.00", 1, 3)))
                .param("addressLine1", "1 Main Road")
                .param("state", "Kerala")
                .param("zipCode", "676503"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("City is required."));

        verify(razorpayService, never()).createOrder(anyLong());
    }

    @Test
    void startingPaymentWhenTheStockRanOutSaysSo() throws Exception {
        razorpayIsConfigured();
        when(furnitureService.getFurnitureById(CHAIR_ID)).thenReturn(chair("349.00", 0));

        createPaymentOrder(sessionWith(cartWithChair("349.00", 1, 3)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("Requested quantity exceeds available stock for Wicker Chair"));

        verify(razorpayService, never()).createOrder(anyLong());
    }
}
