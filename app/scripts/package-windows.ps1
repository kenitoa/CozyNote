$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$gradle = Get-Command gradle -ErrorAction SilentlyContinue
$jpackage = Get-Command jpackage -ErrorAction SilentlyContinue

if (-not $gradle) {
    throw "Gradle was not found on PATH. Install Gradle or add a Gradle wrapper before packaging."
}

if (-not $jpackage) {
    throw "jpackage was not found on PATH. Use JDK 21 or add its bin directory to PATH."
}

Push-Location $root
try {
    gradle clean installDist

    $appName = "CozyNote"
    $inputDir = Join-Path $root "build\install\cozynote\lib"
    $destDir = Join-Path $root "build\jpackage"
    $mainJar = "cozynote-0.1.0.jar"
    $mainClass = "com.cozynote.CozyNoteApp"
    $icon = Join-Path $root "src\main\resources\com\cozynote\assets\icons\app.ico"

    New-Item -ItemType Directory -Force $destDir | Out-Null

    $args = @(
        "--type", "app-image",
        "--name", $appName,
        "--dest", $destDir,
        "--input", $inputDir,
        "--main-jar", $mainJar,
        "--main-class", $mainClass,
        "--app-version", "0.1.0",
        "--vendor", "CozyNote"
    )

    if (Test-Path $icon) {
        $args += @("--icon", $icon)
    }

    jpackage @args
    Write-Host "Packaged application image: $destDir\$appName"
}
finally {
    Pop-Location
}
