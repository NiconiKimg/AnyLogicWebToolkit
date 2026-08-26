# Build manual con javac (sin Gradle)
# Util cuando gradlew no esta disponible
# Requiere: Java 17 en PATH, jcefmaven.jar y gson.jar en scripts/libs/

param(
    [string]$JavaHome = $env:JAVA_HOME,
    [string]$Version  = "0.1.0"
)

$root    = Split-Path $PSScriptRoot -Parent
$src     = "$root\src\main\java"
$out     = "$root\build\classes"
$distJar = "$root\dist\AnyLogicWebToolkit.jar"
$libsDir = "$root\scripts\libs"

if (-not $JavaHome) { $JavaHome = (Get-Command java -ErrorAction SilentlyContinue)?.Source | Split-Path | Split-Path }
$javac = "$JavaHome\bin\javac.exe"
$jar   = "$JavaHome\bin\jar.exe"

if (-not (Test-Path $javac)) { Write-Error "javac no encontrado. Establecer JAVA_HOME o agregar Java 17 al PATH."; exit 1 }

Write-Host "Compilando con $javac..."
New-Item -ItemType Directory -Force -Path $out | Out-Null
New-Item -ItemType Directory -Force -Path "$root\dist" | Out-Null

# Classpath: jars en scripts/libs/
$cp = (Get-ChildItem "$libsDir\*.jar" -ErrorAction SilentlyContinue) -join ";"

$sources = Get-ChildItem $src -Recurse -Filter "*.java" | ForEach-Object { $_.FullName }
if ($sources.Count -eq 0) { Write-Error "No se encontraron fuentes Java en $src"; exit 1 }

& $javac --release 17 -cp "$cp" -d $out $sources
if ($LASTEXITCODE -ne 0) { Write-Error "Compilacion fallida"; exit 1 }

Write-Host "Empaquetando JAR..."
& $jar --create --file $distJar -C $out .
if ($LASTEXITCODE -eq 0) { Write-Host "OK: $distJar" } else { Write-Error "Error creando JAR"; exit 1 }
