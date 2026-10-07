<#
.SYNOPSIS
Creates the Super Artemis release keystore and uploads the four signing secrets to GitHub.

.DESCRIPTION
Run once from PowerShell 7. The password is read as a secure string and never written to disk or echoed.
The keystore stays outside the repository. Back it up together with its password, because losing either one blocks every future update of the installed app.
#>
param(
    [string]$KeystorePath = (Join-Path $HOME ".android-keystores\super-artemis-release.jks"),
    [string]$KeyAlias = "superartemis",
    [string]$Repo = "Arora-Sir/Super-Artemis",
    [string]$Keytool = "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe"
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path $Keytool)) {
    throw "keytool not found at $Keytool. Pass -Keytool with the path to a JDK keytool.exe."
}
gh auth status | Out-Null

$securePassword = Read-Host "Keystore password (at least 6 characters)" -AsSecureString
$password = [System.Net.NetworkCredential]::new("", $securePassword).Password

if (Test-Path $KeystorePath) {
    Write-Host "Reusing existing keystore at $KeystorePath"
}
else {
    $confirm = [System.Net.NetworkCredential]::new("", (Read-Host "Confirm password" -AsSecureString)).Password
    if ($password -ne $confirm) {
        throw "Passwords do not match."
    }
    if ($password.Length -lt 6) {
        throw "keytool requires at least 6 characters."
    }
    New-Item -ItemType Directory -Force -Path (Split-Path $KeystorePath) | Out-Null

    # keytool reads the password from an environment variable, so it never appears on a command line
    $env:SUPER_ARTEMIS_KS_PASS = $password
    try {
        & $Keytool -genkeypair -keystore $KeystorePath -alias $KeyAlias -keyalg RSA -keysize 4096 -validity 10000 `
            -storepass:env SUPER_ARTEMIS_KS_PASS -keypass:env SUPER_ARTEMIS_KS_PASS `
            -dname "CN=Super Artemis, O=Arora-Sir, C=IN"
        if ($LASTEXITCODE -ne 0) { throw "keytool failed with exit code $LASTEXITCODE" }
    }
    finally {
        Remove-Item Env:SUPER_ARTEMIS_KS_PASS -ErrorAction SilentlyContinue
    }
    Write-Host "Created keystore at $KeystorePath"
}

# PKCS12 keystores use one password for the store and the key, so both secrets get the same value
$keystoreBase64 = [Convert]::ToBase64String([IO.File]::ReadAllBytes($KeystorePath))
gh secret set ANDROID_KEYSTORE_BASE64 --repo $Repo --body $keystoreBase64
gh secret set KEYSTORE_PASSWORD --repo $Repo --body $password
gh secret set KEY_PASSWORD --repo $Repo --body $password
gh secret set KEY_ALIAS --repo $Repo --body $KeyAlias

$password = $null
$keystoreBase64 = $null
Write-Host "Signing secrets set on $Repo. Back up $KeystorePath and its password now."
