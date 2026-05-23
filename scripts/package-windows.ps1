$ErrorActionPreference = "Stop"
Set-Location (Split-Path $PSScriptRoot -Parent)
.\mvnw -pl client -am package
Remove-Item -Recurse -Force client\target\package-input, dist\windows -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force client\target\package-input | Out-Null
Copy-Item client\target\outline-client-0.1.0.jar client\target\package-input\
.\mvnw -pl client dependency:copy-dependencies -DincludeScope=runtime -DoutputDirectory=target/package-input
jpackage `
  --type exe `
  --name "Outline Chat" `
  --app-version 1.0.0 `
  --module-path client\target\package-input `
  --module com.outline.client/com.outline.client.OutlineClientApplication `
  --dest dist\windows
