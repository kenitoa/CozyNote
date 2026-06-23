$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$javaFxVersion = "21.0.6"
$javaFxRoot = Join-Path $env:USERPROFILE ".m2\repository\org\openjfx"

$jars = @(
    Join-Path $javaFxRoot "javafx-base\$javaFxVersion\javafx-base-$javaFxVersion-win.jar"
    Join-Path $javaFxRoot "javafx-graphics\$javaFxVersion\javafx-graphics-$javaFxVersion-win.jar"
    Join-Path $javaFxRoot "javafx-controls\$javaFxVersion\javafx-controls-$javaFxVersion-win.jar"
    Join-Path $javaFxRoot "javafx-fxml\$javaFxVersion\javafx-fxml-$javaFxVersion-win.jar"
    Join-Path $javaFxRoot "javafx-media\$javaFxVersion\javafx-media-$javaFxVersion-win.jar"
    Join-Path $javaFxRoot "javafx-web\$javaFxVersion\javafx-web-$javaFxVersion-win.jar"
)

foreach ($jar in $jars) {
    if (-not (Test-Path $jar)) {
        throw "JavaFX jar not found: $jar"
    }
}

$classes = Join-Path $root "target\classes"
$sources = Get-ChildItem (Join-Path $root "src\main\java") -Recurse -Filter "*.java" | ForEach-Object { $_.FullName }
$resources = Join-Path $root "src\main\resources\*"
$sqliteJar = Get-ChildItem (Join-Path $env:USERPROFILE ".gradle\caches\modules-2\files-2.1\org.xerial\sqlite-jdbc") -Recurse -Filter "sqlite-jdbc-*.jar" -ErrorAction SilentlyContinue | Select-Object -First 1
if (-not $sqliteJar) {
    $sqliteJar = Get-ChildItem (Join-Path $env:USERPROFILE ".m2\repository\org\xerial\sqlite-jdbc") -Recurse -Filter "sqlite-jdbc-*.jar" -ErrorAction SilentlyContinue | Select-Object -First 1
}
if (-not $sqliteJar) {
    Write-Warning "sqlite-jdbc jar was not found in Gradle or Maven cache. SQLite features will fail unless you run with Gradle dependencies."
}
$runtimeJars = @($jars)
if ($sqliteJar) {
    $runtimeJars += $sqliteJar.FullName
}
$classpath = ($runtimeJars + $classes) -join ";"

New-Item -ItemType Directory -Force $classes | Out-Null
javac -encoding UTF-8 --class-path ($runtimeJars -join ";") -d $classes $sources
Copy-Item $resources $classes -Recurse -Force
java --class-path $classpath com.cozynote.Main
