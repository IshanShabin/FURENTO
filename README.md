# FURENTO

FURENTO is a furniture rental management system for browsing furniture, creating rental orders, and managing inventory.

## Features

- Browse furniture by category and view rental pricing and availability.
- Register and sign in to manage a shopping cart and rental orders.
- Choose rental quantities and durations, then submit a delivery address at checkout.
- Track rentals from a customer dashboard.
- Manage furniture, categories, and rental orders through the admin area.
- Accept Cash on Delivery or use Razorpay Checkout for online UPI and card payments.

## Payments

Online checkout creates a Razorpay order and verifies the payment on the server before saving the rental. Razorpay credentials are read from environment variables and should never be committed to the repository. Test Mode simulates transactions; live payments require an activated Razorpay account and Live Mode keys.
