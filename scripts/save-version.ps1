param(
    [Parameter(Mandatory = $true)]
    [ValidatePattern('^v\d+\.\d+\.\d+(-[a-zA-Z0-9.-]+)?$')]
    [string]$Version,

    [Parameter(Mandatory = $true)]
    [string]$Message,

    [switch]$SkipTests
)

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot

# Prevent overwriting an existing version tag
$existingTag = git tag -l $Version
if ($existingTag) {
    Write-Error "Version tag '$Version' already exists! Pick the next version number (run 'git tag -l -n' to inspect)."
    exit 1
}

if (-not $SkipTests) {
    Write-Host "Running automated test suite before saving version $Version..." -ForegroundColor Cyan
    & .\mvnw.cmd test -B
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Tests failed. Fix issues before saving version $Version."
        exit 1
    }
}

# Sync <version> in pom.xml (strip leading 'v')
$semver = $Version.TrimStart('v')
$pomPath = Join-Path $repoRoot "pom.xml"
$pomContent = Get-Content $pomPath -Raw
$updatedPom = [regex]::Replace($pomContent, '(<artifactId>ilonggo-bistro</artifactId>\s*<version>)[^<]+(</version>)', "`${1}$semver`${2}", 1)
if ($updatedPom -ne $pomContent) {
    Set-Content -Path $pomPath -Value $updatedPom -NoNewline
    Write-Host "Updated pom.xml version to $semver" -ForegroundColor Green
}

git add -A
$pending = git status --porcelain
if ($pending) {
    git commit -m "$Version - $Message"
} else {
    Write-Host "Working tree already clean — tagging current commit as $Version." -ForegroundColor Yellow
}

git tag -a $Version -m "$Version - $Message"
Write-Host "Saved local version tag: $Version" -ForegroundColor Green

$remote = git remote
if ($remote -contains "origin") {
    $branch = git rev-parse --abbrev-ref HEAD
    Write-Host "Pushing branch '$branch' and tag '$Version' to GitHub (origin)..." -ForegroundColor Cyan
    git push origin $branch
    git push origin $Version
    Write-Host "Pushed $Version safely to GitHub!" -ForegroundColor Green
} else {
    Write-Host "No GitHub remote 'origin' configured yet. Run 'git remote add origin <URL>' then 'git push -u origin main --tags'." -ForegroundColor Yellow
}
