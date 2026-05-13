param(
    [string]$SonarHostUrl = "http://localhost:9000"
)

$ErrorActionPreference = "Stop"

Push-Location $PSScriptRoot
try {
    mvn clean test

    $coverageReport = Join-Path $PSScriptRoot "target/site/jacoco/jacoco.xml"
    if (-not (Test-Path -LiteralPath $coverageReport)) {
        throw "Missing JaCoCo XML report: $coverageReport"
    }

    $scannerArgs = @("-Dsonar.host.url=$SonarHostUrl")
    if ($env:SONAR_TOKEN) {
        $scannerArgs += "-Dsonar.login=$env:SONAR_TOKEN"
    }

    sonar-scanner @scannerArgs
}
finally {
    Pop-Location
}
