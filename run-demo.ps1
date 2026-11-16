Write-Host "========================================================" -ForegroundColor Cyan
Write-Host "  VolunteerGrid - Campus Event Volunteer Management" -ForegroundColor Green
Write-Host "  Starting with H2 In-Memory DB + Pre-seeded Demo Data" -ForegroundColor Yellow
Write-Host "========================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Application URL : http://localhost:8080" -ForegroundColor White
Write-Host "H2 Console      : http://localhost:8080/h2-console" -ForegroundColor White
Write-Host ""
Write-Host "Demo Accounts:" -ForegroundColor Magenta
Write-Host "  - Admin:     admin@volunteergrid.com / Admin@1234" -ForegroundColor Gray
Write-Host "  - Organizer: org1@volunteergrid.com  / Org@1234" -ForegroundColor Gray
Write-Host "  - Volunteer: vol1@volunteergrid.com  / Vol@1234" -ForegroundColor Gray
Write-Host ""

$appJar = Join-Path $PSScriptRoot "target\volunteergrid-1.0.0-SNAPSHOT.jar"

if (-not (Test-Path $appJar)) {
    Write-Error "Packaged application JAR not found at $appJar. Run 'mvn clean package' first."
    exit 1
}

java "-Dnet.bytebuddy.experimental=true" -jar $appJar --spring.profiles.active=h2,demo
