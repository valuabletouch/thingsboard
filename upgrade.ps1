# .env dosyasının yolu
$envFilePath = ".vscode\ThingsboardUpdateApplication\.env"

$rootDirectory = "."

# $jarFilesContainingDirectories = Get-ChildItem -Path $rootDirectory -Recurse -File | Where-Object { $_.Extension -eq ".jar" } | Select-Object -ExpandProperty Directory | Get-Unique

# # Belirli bir dizinden göreli yolun hesaplanması için fonksiyon
# function Get-RelativePath {
#     param (
#         [string]$basePath,
#         [string]$targetPath
#     )

#     $absoluteBase = (Get-Item $basePath).FullName
#     $absoluteTarget = (Get-Item $targetPath).FullName

#     $relativePath = $absoluteTarget.Replace($absoluteBase, "")
#     if ($relativePath.StartsWith("\")) {
#         $relativePath = $relativePath.Substring(1)
#     }
#     return $relativePath
# }

# # Tüm dizinlerin göreli yollarını alıp sonuna * ekleyerek ; ile birleştir
# $combinedPaths = foreach ($directory in $jarFilesContainingDirectories) {
#     $relativePath = Get-RelativePath -basePath $rootDirectory -targetPath $directory.FullName
#     $relativePath + "\*"
# }

# Sonuçları ; ile birleştirerek ekrana yazdır
# $classPath = $combinedPaths -join ";"

$newEnv = ""
# .env dosyasını oku ve her bir satırı PowerShell değişkenlerine ata
Get-Content $envFilePath | ForEach-Object {
    $line = $_
    if ($line -match '^(?<key>[^=]+)=(?<value>.*)$') {
        $key = $Matches['key']
        $value = $Matches['value']
        $key = $key.Trim('"')
        $value = $value.Trim('"')
        $value = $value -replace '\\', '\\'
        $newEnv = $newEnv + "$key=$value; "
        Set-Item -Path "env:$key" -Value $value
    }
}

Write-Host $newEnv

# '-cp' '$classPath'

$javaCommand = "& java '-cp' 'application\\target\\*;application\\target\\dependency\\*;target\\dependency\\*' 'org.thingsboard.server.vsensor.update.ThingsboardUpdateApplication' 'org.springframework.boot.loader.PropertiesLauncher'"

Write-Host $javaCommand

Invoke-Expression $javaCommand