Write-Host "========================================================" -ForegroundColor Cyan
Write-Host "   Running VolunteerGrid Standalone Test Suite" -ForegroundColor Green
Write-Host "========================================================" -ForegroundColor Cyan
Write-Host ""

$jars = (Get-ChildItem -Path "$env:USERPROFILE\.m2\repository" -Recurse -Filter "*.jar" | Where-Object { $_.FullName -notmatch "1\.7\.36" } | Select-Object -ExpandProperty FullName) -join ";"
$cp = "target\classes;target\test-classes;" + $jars

java "-Dnet.bytebuddy.experimental=true" -cp $cp com.volunteergrid.StandaloneTestRunner
