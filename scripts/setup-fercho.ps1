#Requires -Version 5.1
<#
.SYNOPSIS
    Configura Fercho AI (100% local) para SuperFercho usando Ollama.

.DESCRIPTION
    Idempotente y no destructivo: verifica prerequisitos, detecta/arranca Ollama,
    descarga el modelo segun el hardware disponible, crea el modelo "fercho" con
    contexto ampliado, escribe la configuracion local en .env (sin pisar valores
    no vacios) y ejecuta un probe de tool calling contra el modelo.

    Nunca lee credenciales de OpenCode, nunca descarga ni copia API keys y nunca
    elimina procesos ni archivos del usuario.

.PARAMETER Model
    Tag de Ollama a usar (ejemplo: gpt-oss:20b, granite4.1:8b). Si se omite, se
    selecciona segun la RAM disponible.

.EXAMPLE
    .\scripts\setup-fercho.ps1
    .\scripts\setup-fercho.ps1 -Model granite4.1:8b
#>
[CmdletBinding()]
param(
    [string]$Model = ""
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent $PSScriptRoot
$OllamaBase = "http://localhost:11434"
$NumCtx = 16384
$FerchoModel = "fercho"

$script:Summary = New-Object System.Collections.Generic.List[string]

function Write-Step([string]$Message) { Write-Host ""; Write-Host "== $Message" }
function Write-Ok([string]$Message) {
    Write-Host "  [OK] $Message" -ForegroundColor Green
    $script:Summary.Add("[OK] $Message")
}
function Write-Warn2([string]$Message) {
    Write-Host "  [AVISO] $Message" -ForegroundColor Yellow
    $script:Summary.Add("[AVISO] $Message")
}
function Write-Fail([string]$Message) {
    Write-Host "  [FALLO] $Message" -ForegroundColor Red
    $script:Summary.Add("[FALLO] $Message")
}
function Stop-WithError([string]$Message) {
    Write-Host "" -ForegroundColor Red
    Write-Host "ERROR: $Message" -ForegroundColor Red
    Write-Host "Setup abortado. No se modifico nada que requiera deshacer." -ForegroundColor Red
    exit 1
}

function Invoke-NativeCommand {
    param([Parameter(Mandatory = $true)][scriptblock]$Command)
    $previous = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $outputLines = & $Command 2>&1 | ForEach-Object { "$_" }
        return @{ ExitCode = $LASTEXITCODE; Output = ($outputLines -join [Environment]::NewLine) }
    } finally {
        $ErrorActionPreference = $previous
    }
}

function Test-HttpStatusOk([string]$Url, [int]$TimeoutSeconds = 3) {
    try {
        $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec $TimeoutSeconds
        return $response.StatusCode -eq 200
    } catch {
        return $false
    }
}

function Get-DotEnvMap([string]$Path) {
    $map = @{}
    if (-not (Test-Path $Path)) { return $map }
    foreach ($line in [System.IO.File]::ReadAllLines($Path)) {
        $trimmed = $line.Trim()
        if ($trimmed -eq "" -or $trimmed.StartsWith("#")) { continue }
        $equals = $trimmed.IndexOf("=")
        if ($equals -lt 1) { continue }
        $key = $trimmed.Substring(0, $equals).Trim()
        $value = $trimmed.Substring($equals + 1)
        if (($value.StartsWith('"') -and $value.EndsWith('"')) -or ($value.StartsWith("'") -and $value.EndsWith("'"))) {
            if ($value.Length -ge 2) { $value = $value.Substring(1, $value.Length - 2) }
        }
        $map[$key] = $value
    }
    return $map
}

function Save-DotEnvMap([string]$Path, $Map) {
    $lines = New-Object System.Collections.Generic.List[string]
    if (Test-Path $Path) {
        foreach ($line in [System.IO.File]::ReadAllLines($Path)) {
            $trimmed = $line.Trim()
            if ($trimmed -eq "" -or $trimmed.StartsWith("#") -or $trimmed.IndexOf("=") -lt 1) {
                $lines.Add($line)
                continue
            }
            $key = $trimmed.Substring(0, $trimmed.IndexOf("=")).Trim()
            if ($Map.ContainsKey($key)) {
                $lines.Add("$key=$($Map[$key])")
                $Map.Remove($key)
            } else {
                $lines.Add($line)
            }
        }
    }
    foreach ($key in $Map.Keys) { $lines.Add("$key=$($Map[$key])") }
    $utf8 = New-Object System.Text.UTF8Encoding $false
    [System.IO.File]::WriteAllLines($Path, $lines.ToArray(), $utf8)
}

# ============================================================
Write-Host "SuperFercho - setup de Fercho AI (Ollama local)"
Write-Host "Repositorio: $Root"

# --- 1-3. Prerequisitos de la aplicacion (no fatales para el setup de IA) ---
Write-Step "Prerequisitos de la aplicacion SuperFercho"

$appDeps = @(
    @{ Name = "Java 21";   Ok = $false; Hint = "Instala JDK 21 (https://adoptium.net)" },
    @{ Name = "Docker";    Ok = $false; Hint = "Instala Docker Desktop (https://www.docker.com/products/docker-desktop)" },
    @{ Name = "Node/corepack"; Ok = $false; Hint = "Instala Node.js 20.9+ (incluye Corepack) (https://nodejs.org)" }
)

$javaCheck = Invoke-NativeCommand { java -version 2>&1 }
if ($javaCheck.ExitCode -eq 0 -and $javaCheck.Output -match 'version "21') {
    $appDeps[0].Ok = $true
}
$dockerCheck = Invoke-NativeCommand { docker info --format "{{.ServerVersion}}" }
if ($dockerCheck.ExitCode -eq 0) { $appDeps[1].Ok = $true }
if ((Get-Command "corepack.cmd" -ErrorAction SilentlyContinue) -or (Get-Command "corepack" -ErrorAction SilentlyContinue)) {
    $appDeps[2].Ok = $true
}

foreach ($dep in $appDeps) {
    if ($dep.Ok) {
        Write-Ok "$($dep.Name) disponible"
    } else {
        Write-Warn2 "$($dep.Name) NO disponible. $($dep.Hint). La app no levantara sin esto, pero el setup de Fercho AI puede continuar."
    }
}

# --- 4-5. Ollama instalado y ejecutandose ---
Write-Step "Ollama"

$ollamaCommand = Get-Command "ollama" -ErrorAction SilentlyContinue
if (-not $ollamaCommand) {
    Write-Host "  Ollama no esta instalado." -ForegroundColor Red
    Write-Host ""
    Write-Host "  Instalalo con una de estas opciones:" -ForegroundColor Yellow
    Write-Host "    Windows : winget install Ollama.Ollama   (o https://ollama.com/download)"
    Write-Host "    macOS   : brew install ollama            (o https://ollama.com/download)"
    Write-Host "    Linux   : curl -fsSL https://ollama.com/install.sh | sh"
    Write-Host ""
    Write-Host "  Luego vuelve a ejecutar este script." -ForegroundColor Yellow
    Stop-WithError "Ollama no esta instalado."
}
Write-Ok "Ollama instalado ($($ollamaCommand.Source))"

$versionOk = Test-HttpStatusOk "$OllamaBase/api/version"
if (-not $versionOk) {
    Write-Step "Ollama esta instalado pero no responde; intentando arrancarlo"
    $ollamaPort = Get-NetTCPConnection -LocalPort 11434 -State Listen -ErrorAction SilentlyContinue
    if ($ollamaPort) {
        $procIds = @($ollamaPort | Select-Object -ExpandProperty OwningProcess -Unique)
        $procNames = ($procIds | ForEach-Object {
            $p = Get-CimInstance Win32_Process -Filter "ProcessId=$_" -ErrorAction SilentlyContinue
            if ($p) { "$($p.Name) (PID $_)" } else { "PID $_" }
        }) -join "; "
        Stop-WithError "El puerto 11434 esta ocupado por: $procNames. No es un servidor Ollama que responda en /api/version. Libera el puerto o revisa ese proceso; este script no detiene procesos."
    }
    $serve = Start-Process -FilePath $ollamaCommand.Source -ArgumentList "serve" -WindowStyle Hidden -PassThru
    Write-Host "  Proceso 'ollama serve' iniciado (PID $($serve.Id)). Esperando respuesta..."
    $deadline = (Get-Date).AddSeconds(30)
    while ((Get-Date) -lt $deadline) {
        if (Test-HttpStatusOk "$OllamaBase/api/version") { $versionOk = $true; break }
        Start-Sleep -Seconds 2
    }
    if (-not $versionOk) {
        Stop-WithError "Ollama no quedo disponible en $OllamaBase tras 30 segundos. Abre la aplicacion de Ollama manualmente y vuelve a ejecutar el script."
    }
}
$ollamaVersion = (Invoke-RestMethod -Uri "$OllamaBase/api/version" -TimeoutSec 5).version
Write-Ok "Servidor Ollama activo en $OllamaBase (version $ollamaVersion)"

# --- 6. Hardware ---
Write-Step "Hardware detectado"

$totalRamGb = 0
try {
    $cs = Get-CimInstance Win32_ComputerSystem
    $totalRamGb = [math]::Round($cs.TotalPhysicalMemory / 1GB, 1)
} catch {
    $totalRamGb = 0
}
if ($totalRamGb -gt 0) {
    Write-Host "  RAM total: $totalRamGb GB"
} else {
    Write-Host "  RAM total: no se pudo determinar"
}

try {
    $gpus = Get-CimInstance Win32_VideoController -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -and $_.Name -notmatch "Basic|Virtual" }
    foreach ($gpu in $gpus) {
        $vramBytes = 0
        $regPath = "HKLM:\SYSTEM\CurrentControlSet\Control\Class\{4d36e968-e325-11ce-bfc1-08002be10318}"
        try {
            $keys = Get-ChildItem $regPath -ErrorAction SilentlyContinue
            foreach ($key in $keys) {
                $props = Get-ItemProperty $key.PSPath -ErrorAction SilentlyContinue
                if ($props -and $props."HardwareInformation.qwMemorySize" -and $props.DriverDesc -eq $gpu.Name) {
                    $vramBytes = [int64]$props."HardwareInformation.qwMemorySize"
                    break
                }
            }
        } catch { }
        $vramText = if ($vramBytes -gt 0) { ", VRAM ~$([math]::Round($vramBytes / 1GB, 1)) GB" } else { ", VRAM no determinada" }
        Write-Host "  GPU: $($gpu.Name)$vramText"
    }
    if (-not $gpus) { Write-Host "  GPU: no se detecto (inferencia por CPU)" }
} catch {
    Write-Host "  GPU: no se pudo determinar"
}

# --- 7. Seleccion de modelo ---
Write-Step "Seleccion de modelo"

if ($Model -ne "") {
    $baseModel = $Model
    Write-Host "  Modelo seleccionado manualmente: $baseModel"
    if ($totalRamGb -gt 0 -and $totalRamGb -lt 16 -and $baseModel -match "gpt-oss:20b") {
        Write-Warn2 "Elegiste $baseModel con menos de 16 GB de RAM. Puede quedarse sin memoria o ser muy lento. Considera granite4.1:8b."
    }
} elseif ($totalRamGb -ge 16) {
    $baseModel = "gpt-oss:20b"
    Write-Host "  RAM ${totalRamGb} GB >= 16 GB -> candidato: $baseModel (Apache 2.0, tool calling nativo)"
} elseif ($totalRamGb -gt 0) {
    $baseModel = "granite4.1:8b"
    Write-Host "  RAM ${totalRamGb} GB < 16 GB -> candidato ligero: $baseModel (Apache 2.0, multilingue)"
    Write-Warn2 "Con menos de 16 GB la calidad y velocidad del razonamiento seran menores."
} else {
    $baseModel = "granite4.1:8b"
    Write-Warn2 "No se pudo detectar la RAM; se usa el candidato ligero $baseModel. Usa -Model para elegir otro."
}
Write-Host "  Nota: ningun modelo esta aprobado de antemano; el probe y el benchmark deciden."

$modelSizesGb = @{ "gpt-oss:20b" = 14; "granite4.1:8b" = 6 }
if ($modelSizesGb.ContainsKey($baseModel)) {
    $needGb = $modelSizesGb[$baseModel] + 2
} else {
    $needGb = 25
    Write-Warn2 "No conocemos el tamano de '$baseModel'; exigimos 25 GB libres por seguridad."
}

# --- 8. Espacio en disco ---
Write-Step "Espacio en disco"
$modelsDir = $env:OLLAMA_MODELS
if ([string]::IsNullOrWhiteSpace($modelsDir)) { $modelsDir = Join-Path $env:USERPROFILE ".ollama" }
$driveRoot = (Resolve-Path -LiteralPath ([System.IO.Path]::GetPathRoot([System.IO.Path]::GetFullPath($modelsDir)))).Path
$drive = Get-PSDrive -Name ($driveRoot.Substring(0, 1)) -ErrorAction SilentlyContinue
if ($drive) {
    $freeGb = [math]::Round($drive.Free / 1GB, 1)
    Write-Host "  Unidad de modelos de Ollama ($driveRoot): $freeGb GB libres; se requieren ~$needGb GB"
    if ($freeGb -lt $needGb) {
        Stop-WithError "Espacio insuficiente en $driveRoot ($freeGb GB libres; se requieren ~$needGb GB para $baseModel). Libera espacio y vuelve a ejecutar."
    }
    Write-Ok "Espacio suficiente en $driveRoot"
} else {
    Write-Warn2 "No se pudo verificar el espacio en $driveRoot. Continuando; `ollama pull` validara por su cuenta."
}

# --- 9. Descarga del modelo si falta ---
Write-Step "Modelo base de Ollama"
$tagsResponse = Invoke-RestMethod -Uri "$OllamaBase/api/tags" -TimeoutSec 10
$existingModels = @($tagsResponse.models | ForEach-Object { $_.name })
if ($existingModels -contains $baseModel) {
    Write-Ok "$baseModel ya esta descargado"
} else {
    Write-Host "  Descargando $baseModel (puede tardar segun tu conexion; el pull se puede reanudar re-ejecutando el script)..."
    $pull = Invoke-NativeCommand { & ollama pull $baseModel }
    if ($pull.ExitCode -ne 0) {
        Write-Host $pull.Output
        Stop-WithError "`ollama pull $baseModel` fallo. Revisa tu conexion y vuelve a ejecutar el script (continua donde quedo)."
    }
    Write-Ok "$baseModel descargado"
}

# --- 10-11. Modelfile y modelo derivado "fercho" (num_ctx) ---
Write-Step "Modelo derivado '$FerchoModel' (contexto $NumCtx tokens)"
$modelfileContent = "FROM $baseModel`nPARAMETER num_ctx $NumCtx`n"
$needsCreate = $true
$showCurrent = Invoke-NativeCommand { & ollama show $FerchoModel --modelfile }
if ($showCurrent.ExitCode -eq 0) {
    if ($showCurrent.Output -match [regex]::Escape("FROM $baseModel") -and $showCurrent.Output -match "num_ctx\s+$NumCtx") {
        $needsCreate = $false
        Write-Ok "'$FerchoModel' ya existe con la configuracion esperada"
    }
}
if ($needsCreate) {
    $modelfilePath = Join-Path ([System.IO.Path]::GetTempPath()) "Modelfile.fercho"
    [System.IO.File]::WriteAllText($modelfilePath, $modelfileContent, (New-Object System.Text.UTF8Encoding $false))
    Write-Host "  Contenido del Modelfile:"
    Write-Host ($modelfileContent -replace "`n", "`n  ")
    $create = Invoke-NativeCommand { & ollama create $FerchoModel -f $modelfilePath }
    Remove-Item -LiteralPath $modelfilePath -ErrorAction SilentlyContinue
    if ($create.ExitCode -ne 0) {
        Write-Host $create.Output
        Stop-WithError "`ollama create $FerchoModel` fallo."
    }
    Write-Ok "'$FerchoModel' creado desde $baseModel con num_ctx $NumCtx"
}

# --- 12. Merge seguro del .env ---
Write-Step "Configuracion local (.env)"
$envFile = Join-Path $Root ".env"
$envExample = Join-Path $Root ".env.example"
if (-not (Test-Path $envFile)) {
    if (-not (Test-Path $envExample)) {
        Stop-WithError "No existe .env ni .env.example en el repositorio."
    }
    Copy-Item $envExample $envFile
    Write-Host "  Se creo .env a partir de .env.example"
}

$envMap = Get-DotEnvMap $envFile
$desired = [ordered]@{
    "SUPERFERCHO_OPENAI_CHAT_URL"         = "$OllamaBase/v1/chat/completions"
    "SUPERFERCHO_OPENAI_CHAT_MODEL"       = $FerchoModel
    "OPENAI_API_KEY"                      = "ollama"
    "SUPERFERCHO_OPENAI_CHAT_READ_TIMEOUT" = "300s"
}

$pendingChanges = @()
foreach ($key in $desired.Keys) {
    $current = ""
    if ($envMap.ContainsKey($key)) { $current = [string]$envMap[$key] }
    if ([string]::IsNullOrWhiteSpace($current)) {
        $pendingChanges += @{ Key = $key; Value = $desired[$key] }
    }
}

if ($pendingChanges.Count -gt 0) {
    $backup = Join-Path $Root (".env.bak-" + (Get-Date -Format "yyyyMMdd-HHmmss"))
    Copy-Item $envFile $backup
    Write-Host "  Respaldo de .env creado: $(Split-Path -Leaf $backup)"
    foreach ($change in $pendingChanges) {
        $envMap[$change.Key] = $change.Value
        Write-Ok "$($change.Key) configurado (estaba vacio o ausente)"
    }
    Save-DotEnvMap $envFile $envMap
} else {
    Write-Ok ".env ya contiene la configuracion de Fercho AI (valores existentes no se tocan)"
}
Write-Host "  NOTA: OPENAI_API_KEY=ollama es un valor dummy requerido por el adapter actual;"
Write-Host "        los endpoints locales lo ignoran. NO es una credencial de OpenAI ni genera cobros."

# --- 13. Probe de tool calling ---
Write-Step "Probe de tool calling (modelo '$FerchoModel')"
Write-Host "  Puede tardar varios minutos en CPU la primera vez (carga del modelo)..."
$probeBody = @{
    model = $FerchoModel
    messages = @(
        @{ role = "user"; content = "What is the weather in Bogota right now? Use the provided tool." }
    )
    tools = @(
        @{
            type = "function"
            function = @{
                name = "get_weather_probe"
                description = "Get the current weather for a city"
                parameters = @{
                    type = "object"
                    properties = @{
                        city = @{ type = "string"; description = "City name" }
                    }
                    required = @("city")
                }
            }
        }
    )
} | ConvertTo-Json -Depth 10

$probeJson = $null
try {
    $probeResponse = Invoke-WebRequest -Uri "$OllamaBase/v1/chat/completions" `
        -Method Post -ContentType "application/json" -Body $probeBody `
        -Headers @{ Authorization = "Bearer ollama" } -UseBasicParsing -TimeoutSec 300
    $probeJson = $probeResponse.Content
} catch {
    Write-Host ("  Respuesta del servidor: " + $_.Exception.Message)
    Stop-WithError "El probe de tool calling no pudo completarse contra $OllamaBase/v1/chat/completions. Verifica que Ollama siga activo y vuelve a ejecutar el script."
}

$probeOk = $false
$toolCallInfo = ""
try {
    $parsed = $probeJson | ConvertFrom-Json
    $toolCalls = $parsed.choices[0].message.tool_calls
    if ($toolCalls -and @($toolCalls).Count -ge 1) {
        $probeOk = $true
        $first = @($toolCalls)[0]
        $toolCallInfo = "$($first.function.name)($($first.function.arguments))"
    }
} catch {
    Stop-WithError "La respuesta del probe no es JSON valido. Detalle: $($_.Exception.Message)"
}

if ($probeOk) {
    Write-Ok "El modelo respondio con un tool call: $toolCallInfo"
} else {
    Write-Host "  Respuesta recibida (primeras lineas):"
    ($probeJson -split "`n" | Select-Object -First 30) | ForEach-Object { Write-Host "    $_" }
    Write-Fail "El modelo '$FerchoModel' NO devolvio tool_calls en el probe."
    Write-Host ""
    Write-Host "  Este modelo NO pasa la prueba de tool calling, requisito de Fercho." -ForegroundColor Red
    Write-Host "  Opciones:" -ForegroundColor Yellow
    Write-Host "    1. Ejecuta de nuevo con otro modelo: .\scripts\setup-fercho.ps1 -Model granite4.1:8b"
    Write-Host "    2. O con: .\scripts\setup-fercho.ps1 -Model gpt-oss:20b (requiere ~16 GB de RAM)"
    Stop-WithError "El modelo no pasó la prueba de tool calling."
}

# --- 14. Resumen ---
Write-Step "Resumen del setup"
foreach ($line in $script:Summary) {
    if ($line.StartsWith("[OK]")) { Write-Host "  $line" -ForegroundColor Green }
    elseif ($line.StartsWith("[AVISO]")) { Write-Host "  $line" -ForegroundColor Yellow }
    else { Write-Host "  $line" -ForegroundColor Red }
}
Write-Host ""
Write-Host "Fercho AI quedo configurado en modo LOCAL (modelo '$FerchoModel' = $baseModel, num_ctx $NumCtx)."
Write-Host ""
Write-Host "Siguientes pasos:"
Write-Host "  1. Levanta la aplicacion:  .\scripts\dev.ps1"
Write-Host "  2. Registrate como cliente en http://localhost:3000 y habla con Fercho en /assistant"
Write-Host "  3. (Opcional) Benchmark: scripts/fercho-bench.ps1 cuando este disponible"
Write-Host ""
Write-Host "Recuerda: OpenCode es solo una herramienta de desarrollo; nunca es parte del runtime de SuperFercho."
