<#
  Clookie release: bump version, build the signed release APK, publish it on GitHub.
  Usage (from the project folder):  powershell -ExecutionPolicy Bypass -File scripts\release.ps1 0.2.0 [-Notes "text"]
  Needs: keystore.properties (git-ignored), git remote "origin", gh logged in.
#>
param(
    [Parameter(Mandatory = $true)][string]$Version,
    [string]$Notes = ""
)
$ErrorActionPreference = 'Stop'
if ($Version -notmatch '^\d+\.\d+\.\d+$') { throw "Version must look like 1.2.3" }

$root = Split-Path -Parent $PSScriptRoot
Set-Location $root
$repo = 'harieedawwn/clookie'
$tag = "v$Version"
$gh = if (Get-Command gh -ErrorAction SilentlyContinue) { 'gh' } else { "$env:ProgramFiles\GitHub CLI\gh.exe" }
if (-not (Test-Path 'keystore.properties')) { throw "keystore.properties missing: release signing is not configured" }
git rev-parse -q --verify "refs/tags/$tag" | Out-Null
if ($LASTEXITCODE -eq 0) { throw "Tag $tag already exists" }

# 1. Version bump (versionCode only grows; same name twice keeps the code for a first publish).
$propsPath = Join-Path $root 'version.properties'
$props = @{}
Get-Content $propsPath | Where-Object { $_ -match '=' } | ForEach-Object { $k, $v = $_ -split '=', 2; $props[$k.Trim()] = $v.Trim() }
$code = [int]$props['versionCode']
if ($props['versionName'] -ne $Version) { $code++ }
[IO.File]::WriteAllText($propsPath, "versionCode=$code`nversionName=$Version`n", (New-Object Text.UTF8Encoding $false))
Write-Host "Version $Version (code $code)"

# 2. Signed release build (no clean build: incremental is enough).
& .\gradlew.bat :app:assembleRelease --console=plain
if ($LASTEXITCODE -ne 0) { throw "Release build failed" }
$apk = 'app\build\outputs\apk\release\app-release.apk'
if (-not (Test-Path $apk)) { throw "No signed APK at $apk" }

# 3. Verify the signature with the SDK's apksigner.
$sdk = ((Get-Content 'local.properties' | Where-Object { $_ -match '^sdk.dir=' }) -replace '^sdk.dir=', '') -replace '\\:', ':' -replace '\\\\', '\'
$apksigner = Get-ChildItem "$sdk\build-tools" -Directory | Sort-Object Name -Descending | ForEach-Object { "$($_.FullName)\apksigner.bat" } | Where-Object { Test-Path $_ } | Select-Object -First 1
& $apksigner verify --print-certs $apk | Select-String 'Signer #1 certificate SHA-256'
if ($LASTEXITCODE -ne 0) { throw "APK signature check failed" }

# 4. Copy to dist/.
New-Item -ItemType Directory -Force 'dist' | Out-Null
$out = "dist\Clookie-v$Version.apk"
Copy-Item $apk $out -Force
Write-Host ("APK: $out ({0:N1} MB)" -f ((Get-Item $out).Length / 1MB))

# 5. Commit and push main.
git add -A
git diff --cached --quiet
if ($LASTEXITCODE -ne 0) {
    git commit -q -m "Release $tag" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
    if ($LASTEXITCODE -ne 0) { throw "git commit failed" }
}
git push -q origin main
if ($LASTEXITCODE -ne 0) { throw "git push failed" }

# 6. GitHub release with the APK attached.
if (-not $Notes) { $Notes = "Clookie $Version" }
& $gh release create $tag $out --repo $repo --target main --title "Clookie $Version" --notes $Notes
if ($LASTEXITCODE -ne 0) { throw "gh release create failed" }

$url = "https://github.com/$repo/releases/download/$tag/Clookie-v$Version.apk"
Write-Host ""
Write-Host "Download: $url"
