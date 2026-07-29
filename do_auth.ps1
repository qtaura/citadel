$PSClientId = "1950a258-227b-4e31-a9cf-717495945fc2"

# Step 1: Device code
$body = "client_id=$PSClientId&scope=XboxLive.signin+XboxLive.offline_access"
$resp = Invoke-RestMethod -Method Post -Uri "https://login.microsoftonline.com/consumers/oauth2/v2.0/devicecode" -Body $body -ContentType "application/x-www-form-urlencoded"
Write-Host "`nOpen https://login.microsoft.com/device"
Write-Host "Enter code: $($resp.user_code)"
Write-Host "Sign in with your Microsoft account`n"

# Step 2: Poll
$pollBody = "client_id=$PSClientId&grant_type=urn:ietf:params:oauth:grant-type:device_code&device_code=$($resp.device_code)"
$tokenUrl = "https://login.microsoftonline.com/consumers/oauth2/v2.0/token"
$msToken = $null
$deadline = [DateTime]::Now.AddSeconds($resp.expires_in)
while ([DateTime]::Now -lt $deadline) {
    Start-Sleep -Seconds 3
    try {
        $pollResp = Invoke-RestMethod -Method Post -Uri $tokenUrl -Body $pollBody -ContentType "application/x-www-form-urlencoded" -ErrorAction SilentlyContinue
        if ($pollResp.access_token) {
            $msToken = $pollResp.access_token
            break
        }
    } catch {
        $err = $_.Exception.Response
        if ($err.StatusCode -ne 400) { throw }
    }
}
if (-not $msToken) { Write-Host "Timed out"; exit 1 }
Write-Host "Got Microsoft token!"

# Step 3: XBL
$xblBody = @{Properties=@{AuthMethod="RPS";SiteName="user.auth.xboxlive.com";RpsTicket="d=$msToken"};RelyingParty="http://auth.xboxlive.com";TokenType="JWT"} | ConvertTo-Json
$xblResp = Invoke-RestMethod -Method Post -Uri "https://user.auth.xboxlive.com/user/authenticate" -Body $xblBody -ContentType "application/json"
$xblToken = $xblResp.Token
$uhs = $xblResp.DisplayClaims.xui[0].uhs
Write-Host "XBL OK"

# Step 4: XSTS
$xstsBody = @{Properties=@{SandboxId="RETAIL";UserTokens=@($xblToken)};RelyingParty="rp://api.minecraftservices.com/";TokenType="JWT"} | ConvertTo-Json
$xstsResp = Invoke-RestMethod -Method Post -Uri "https://xsts.auth.xboxlive.com/xsts/authorize" -Body $xstsBody -ContentType "application/json"
$xstsToken = $xstsResp.Token
Write-Host "XSTS OK"

# Step 5: Minecraft login
$identityToken = "XBL3.0 x=$uhs;$xstsToken"
$mcBody = @{identityToken=$identityToken} | ConvertTo-Json
$mcResp = Invoke-RestMethod -Method Post -Uri "https://api.minecraftservices.com/authentication/login_with_xbox" -Body $mcBody -ContentType "application/json"
$mcToken = $mcResp.access_token
Write-Host "Minecraft login OK"

# Step 6: Profile
$profileResp = Invoke-RestMethod -Method Get -Uri "https://api.minecraftservices.com/minecraft/profile" -Headers @{Authorization="Bearer $mcToken"}
Write-Host "`n========== AUTHENTICATED =========="
Write-Host "Username: $($profileResp.name)"
Write-Host "UUID: $($profileResp.id)"
Write-Host "MC Token: $($mcToken.Substring(0,30))..."

# Save to file for Citadel to use
$sessionData = @{profileId=$profileResp.id;username=$profileResp.name;accessToken=$mcToken;expiresAt=([DateTime]::Now.AddHours(24)).ToString("o")} | ConvertTo-Json
$sessionData | Out-File -FilePath "C:\Users\kaesh\Downloads\citadel\session.json" -Encoding UTF8
Write-Host "`nSession saved! Run the bot now."
