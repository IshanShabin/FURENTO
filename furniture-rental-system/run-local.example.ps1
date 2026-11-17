# Local run helper for Windows PowerShell.
#
# 1. Copy this file to run-local.ps1 (that name is git-ignored, so your secrets stay local).
# 2. Fill in the values below.
# 3. From the furniture-rental-system folder, run:  .\run-local.ps1
#
# Requirements: JDK 17 and Maven on your PATH (JAVA_HOME set), and MySQL running
# with an empty database named springdb.

$ErrorActionPreference = "Stop"

# --- Fill these in ------------------------------------------------------------
$env:DB_PASSWORD         = "your-mysql-password"
$env:ADMIN_USERNAME      = "admin"
$env:ADMIN_PASSWORD      = "choose-a-strong-password"   # at least 8 characters
$env:RAZORPAY_KEY_ID     = ""                           # optional, e.g. rzp_test_...
$env:RAZORPAY_KEY_SECRET = ""                           # optional
# -----------------------------------------------------------------------------

# Keep the same JWT secret between runs so logins survive restarts.
$secretFile = Join-Path $PSScriptRoot ".jwt-secret.local"
if (-not (Test-Path $secretFile)) {
    $bytes = New-Object byte[] 48
    [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
    [Convert]::ToBase64String($bytes) | Set-Content -Path $secretFile -NoNewline
}
$env:JWT_SECRET = (Get-Content -Path $secretFile -Raw).Trim()

Set-Location $PSScriptRoot
mvn spring-boot:run
