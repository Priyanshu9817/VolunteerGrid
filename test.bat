@echo off
title VolunteerGrid - Test Suite
echo ========================================================
echo   Running VolunteerGrid Standalone Test Suite
echo ========================================================
echo.
powershell -NoProfile -ExecutionPolicy Bypass -Command "$jars = (Get-ChildItem -Path \"$env:USERPROFILE\.m2\repository\" -Recurse -Filter \"*.jar\" | Where-Object { $_.FullName -notmatch '1\.7\.36' } | Select-Object -ExpandProperty FullName) -join ';'; java \"-Dnet.bytebuddy.experimental=true\" -cp ('target\classes;target\test-classes;' + $jars) com.volunteergrid.StandaloneTestRunner"
echo.
pause
