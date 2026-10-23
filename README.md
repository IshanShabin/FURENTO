# FURENTO

Furento is a furniture rental management system for browsing available furniture and placing rental orders.

## Run locally

Requirements: Java 17, Maven, and MySQL.

1. Create the database in MySQL:

   ```sql
   CREATE DATABASE springdb;
   ```

2. In PowerShell, set your local MySQL password and (for online checkout) Razorpay **Test Mode** keys. Enter secrets at the prompts; do not commit them.

   ```powershell
   $env:DB_PASSWORD = Read-Host "MySQL password"
   $env:RAZORPAY_KEY_ID = Read-Host "Razorpay test Key ID"
   $secret = Read-Host "Razorpay test Key Secret" -AsSecureString
   $env:RAZORPAY_KEY_SECRET = [System.Net.NetworkCredential]::new("", $secret).Password
   ```

   Razorpay keys are optional if you only want to use Cash on Delivery. Online test payments are simulated and do not charge real money.

3. From the project directory, start the app:

   ```powershell
   mvn spring-boot:run
   ```

4. Open [http://localhost:8082](http://localhost:8082).
