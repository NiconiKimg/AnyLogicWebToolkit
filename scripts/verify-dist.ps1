# Verifica que la distribucion tiene todos los archivos necesarios
param([string]$DistDir = (Split-Path $PSScriptRoot -Parent) + "\dist")

$required = @("AnyLogicWebToolkit.jar")
$ok = $true
foreach ($f in $required) {
    $path = Join-Path $DistDir $f
    if (Test-Path $path) {
        $size = (Get-Item $path).Length
        Write-Host "OK  $f ($([math]::Round($size/1KB, 1)) KB)"
    } else {
        Write-Host "MISSING  $f" -ForegroundColor Red
        $ok = $false
    }
}
if ($ok) { Write-Host "`nDistribucion lista." -ForegroundColor Green }
else     { Write-Host "`nFaltan archivos en la distribucion." -ForegroundColor Red }
