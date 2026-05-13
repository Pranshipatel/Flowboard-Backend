param(
    [string]$SonarHostUrl = "http://localhost:9000",
    [double]$MinimumLineCoverage = 70,
    [double]$MaximumLineCoverage = 80
)

$ErrorActionPreference = "Stop"

Push-Location $PSScriptRoot
try {
    Write-Host "Running tests and generating JaCoCo coverage..."
    mvn clean test

    $coverageReport = Join-Path $PSScriptRoot "target/site/jacoco/jacoco.xml"
    if (-not (Test-Path -LiteralPath $coverageReport)) {
        throw "JaCoCo XML report was not generated at $coverageReport"
    }

    $coverageXml = [xml](Get-Content -Raw -LiteralPath $coverageReport)
    $lineCounter = $coverageXml.report.counter | Where-Object { $_.type -eq "LINE" }
    if (-not $lineCounter) {
        throw "JaCoCo XML report does not contain a LINE coverage counter: $coverageReport"
    }

    $missedLines = [double]$lineCounter.missed
    $coveredLines = [double]$lineCounter.covered
    $totalLines = $missedLines + $coveredLines
    if ($totalLines -le 0) {
        throw "JaCoCo XML report has no measurable lines: $coverageReport"
    }

    $lineCoverage = [math]::Round(($coveredLines / $totalLines) * 100, 2)
    Write-Host "Auth service JaCoCo line coverage: $lineCoverage% ($coveredLines/$totalLines lines covered)"

    if ($lineCoverage -lt $MinimumLineCoverage) {
        throw 'Auth service line coverage is ' + $lineCoverage + '%, below required ' + $MinimumLineCoverage + '%. SonarQube scan was not published.'
    }

    if ($lineCoverage -gt $MaximumLineCoverage) {
        throw 'Auth service line coverage is ' + $lineCoverage + '%, above allowed ' + $MaximumLineCoverage + '%. SonarQube scan was not published.'
    }

    if (-not $env:SONAR_TOKEN) {
        throw "SONAR_TOKEN is not set. Create a SonarQube token, set `$env:SONAR_TOKEN, then rerun this script so coverage can be published."
    }

    $scannerArgs = @(
        "-Dsonar.host.url=$SonarHostUrl",
        "-Dsonar.token=$env:SONAR_TOKEN",
        "-Dsonar.projectBaseDir=$PSScriptRoot",
        "-Dsonar.coverage.jacoco.xmlReportPaths=$coverageReport"
    )

    Write-Host "Publishing coverage to SonarQube..."
    sonar-scanner @scannerArgs
}
finally {
    Pop-Location
}
