$ErrorActionPreference = "Stop"
Set-Location (Split-Path $PSScriptRoot -Parent)
.\mvnw -pl client -am package
jpackage `
  --type exe `
  --name "Outline Next" `
  --input client\target `
  --main-jar outline-client-0.1.0.jar `
  --main-class com.outline.client.Launcher `
  --dest dist\windows
