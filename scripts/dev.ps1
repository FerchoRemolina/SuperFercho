#Requires -Version 5.1
Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root

function Write-Step([string]$Message) {
    Write-Host $Message
}

function Stop-WithError([string]$Message) {
    Write-Host $Message -ForegroundColor Red
    exit 1
}

function Test-RequiredCommand([string]$Name) {
    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        Stop-WithError "No se encontro '$Name' en el PATH. Instala la dependencia y vuelve a ejecutar."
    }
}

function Test-RequiredPath([string]$RelativePath) {
    $full = Join-Path $Root $RelativePath
    if (-not (Test-Path $full)) {
        Stop-WithError "No se encontro '$RelativePath'. Ejecuta el script desde el repositorio SuperFercho."
    }
}

function Get-DotEnvMap([string]$Path) {
    $map = @{}
    if (-not (Test-Path $Path)) {
        return $map
    }
    foreach ($line in [System.IO.File]::ReadAllLines($Path)) {
        $trimmed = $line.Trim()
        if ($trimmed -eq "" -or $trimmed.StartsWith("#")) {
            continue
        }
        $equals = $trimmed.IndexOf("=")
        if ($equals -lt 1) {
            continue
        }
        $key = $trimmed.Substring(0, $equals).Trim()
        $value = $trimmed.Substring($equals + 1)
        if (
            ($value.StartsWith('"') -and $value.EndsWith('"')) -or
            ($value.StartsWith("'") -and $value.EndsWith("'"))
        ) {
            if ($value.Length -ge 2) {
                $value = $value.Substring(1, $value.Length - 2)
            }
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
    foreach ($key in $Map.Keys) {
        $lines.Add("$key=$($Map[$key])")
    }
    $utf8 = New-Object System.Text.UTF8Encoding $false
    [System.IO.File]::WriteAllLines($Path, $lines.ToArray(), $utf8)
}

function Import-DotEnvToProcess([string]$Path) {
    $map = Get-DotEnvMap $Path
    foreach ($key in $map.Keys) {
        Set-Item -Path "Env:$key" -Value $map[$key]
    }
}

function New-LocalJwtSecret {
    $bytes = New-Object byte[] 48
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        $rng.GetBytes($bytes)
    } finally {
        $rng.Dispose()
    }
    return [Convert]::ToBase64String($bytes)
}

function Invoke-NativeCommand {
    param(
        [Parameter(Mandatory = $true)]
        [scriptblock]$Command
    )
    $previous = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $outputLines = & $Command 2>&1 | ForEach-Object { "$_" }
        return @{
            ExitCode = $LASTEXITCODE
            Output   = ($outputLines -join [Environment]::NewLine)
        }
    } finally {
        $ErrorActionPreference = $previous
    }
}

function Get-ProcessRole {
    param(
        [string]$Name,
        [string]$CommandLine,
        [int]$Port
    )
    $processName = ""
    if ($Name) {
        $processName = $Name.ToLowerInvariant()
    }
    $command = ""
    if ($CommandLine) {
        $command = $CommandLine.ToLowerInvariant()
    }
    if ($processName -match "java") {
        return "backend Java/Spring Boot"
    }
    if ($processName -match "node") {
        return "frontend Next.js"
    }
    if ($processName -match "docker") {
        return "Docker Desktop (publicacion de puertos)"
    }
    if ($processName -match "postgres") {
        return "PostgreSQL de Windows"
    }
    if ($command -match "spring-boot|mvnw") {
        return "backend Java/Spring Boot"
    }
    if ($command -match "next") {
        return "frontend Next.js"
    }
    switch ($Port) {
        5432 { return "proceso en el puerto de PostgreSQL" }
        8080 { return "proceso en el puerto del backend" }
        3000 { return "proceso en el puerto del frontend" }
        default { return "proceso desconocido" }
    }
}

function Get-ListeningOccupants {
    param([int]$Port)
    $processIds = @()
    $previous = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $connections = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
        if ($connections) {
            $processIds = @($connections | Select-Object -ExpandProperty OwningProcess -Unique)
        }
    } finally {
        $ErrorActionPreference = $previous
    }
    if (-not $processIds -or $processIds.Count -eq 0) {
        $net = Invoke-NativeCommand { netstat -ano -p tcp }
        foreach ($line in ($net.Output -split "`r?`n")) {
            if ($line -match "LISTENING" -and $line -match ":${Port}\s" -and $line -match "(\d+)\s*$") {
                $processIds += [int]$Matches[1]
            }
        }
        $processIds = @($processIds | Select-Object -Unique)
    }

    $occupants = @()
    foreach ($processId in $processIds) {
        if (-not $processId -or $processId -eq 0) {
            continue
        }
        $proc = Get-CimInstance Win32_Process -Filter "ProcessId=$processId" -ErrorAction SilentlyContinue
        $procName = "desconocido"
        $commandLine = ""
        if ($proc) {
            $procName = $proc.Name
            if ($proc.CommandLine) {
                $commandLine = [string]$proc.CommandLine
            }
        }
        $occupants += [pscustomobject]@{
            ProcessId = $processId
            Name      = $procName
            Role      = Get-ProcessRole $procName $commandLine $Port
        }
    }
    return $occupants
}

function Format-Occupants {
    param($Occupants)
    $parts = @()
    foreach ($occupant in @($Occupants)) {
        $parts += "$($occupant.Name) PID $($occupant.ProcessId) ($($occupant.Role))"
    }
    return ($parts -join "; ")
}

function Test-OurPostgresContainerRunning {
    $result = Invoke-NativeCommand { docker inspect --format "{{.State.Running}}" superfercho-postgres }
    return ($result.ExitCode -eq 0 -and $result.Output.Trim().ToLowerInvariant() -eq "true")
}

function Assert-PostgresPortReady {
    $occupants = @(Get-ListeningOccupants 5432)
    if ($occupants.Count -eq 0) {
        return
    }
    if (Test-OurPostgresContainerRunning) {
        Write-Step "PostgreSQL de Docker ya esta publicado en el puerto 5432."
        return
    }
    $detail = Format-Occupants $occupants
    Stop-WithError "El puerto 5432 (PostgreSQL) esta ocupado por: $detail. SuperFercho usa el PostgreSQL de Docker y no detiene procesos existentes. Libera el puerto e intenta de nuevo."
}

function Wait-PostgresHealthy {
    $container = "superfercho-postgres"
    $deadline = (Get-Date).AddSeconds(90)
    Write-Step "Esperando a que PostgreSQL acepte conexiones..."
    while ((Get-Date) -lt $deadline) {
        $result = Invoke-NativeCommand { docker inspect --format "{{.State.Health.Status}}" "superfercho-postgres" }
        if ($result.ExitCode -eq 0 -and $result.Output.Trim() -eq "healthy") {
            Write-Step "PostgreSQL esta listo."
            return
        }
        Start-Sleep -Seconds 2
    }
    Stop-WithError "PostgreSQL no quedo disponible a tiempo. Revisa Docker Desktop y el contenedor '$container'."
}

# --- A. Validaciones ---
Test-RequiredCommand "docker"
Test-RequiredCommand "java"
Test-RequiredPath "backend\mvnw.cmd"
Test-RequiredPath "frontend\package.json"
Test-RequiredPath "docker-compose.yml"

$corepack = Get-Command "corepack.cmd" -ErrorAction SilentlyContinue
if (-not $corepack) {
    Stop-WithError "No se encontro 'corepack.cmd' en el PATH. Instala Node.js (incluye Corepack) y vuelve a ejecutar."
}

$dockerInfo = Invoke-NativeCommand { docker info --format "{{.ServerVersion}}" }
if ($dockerInfo.ExitCode -ne 0) {
    Stop-WithError "Docker esta instalado pero el daemon no responde. Abre Docker Desktop e intenta de nuevo."
}

# --- B. Variables locales ---
$envExample = Join-Path $Root ".env.example"
$envFile = Join-Path $Root ".env"
if (-not (Test-Path $envFile)) {
    if (-not (Test-Path $envExample)) {
        Stop-WithError "Falta .env.example; no se puede crear .env."
    }
    Copy-Item $envExample $envFile
    Write-Step "Se creo .env a partir de .env.example."
}

$envMap = Get-DotEnvMap $envFile
$jwt = ""
if ($envMap.ContainsKey("SUPERFERCHO_JWT_SECRET")) {
    $jwt = [string]$envMap["SUPERFERCHO_JWT_SECRET"]
}
if ([string]::IsNullOrWhiteSpace($jwt)) {
    $jwt = New-LocalJwtSecret
    $envMap["SUPERFERCHO_JWT_SECRET"] = $jwt
    Save-DotEnvMap $envFile $envMap
    Write-Step "Se guardo un secreto JWT local en .env (archivo ignorado por Git)."
} else {
    Write-Step "Se reutiliza el secreto JWT local de .env."
}

Import-DotEnvToProcess $envFile

# --- C. PostgreSQL ---
Assert-PostgresPortReady
Write-Step "Levantando PostgreSQL con Docker Compose..."
$compose = Invoke-NativeCommand { docker compose up -d }
if ($compose.ExitCode -ne 0) {
    if ($compose.Output -match "port is already allocated" -or $compose.Output -match "already in use") {
        $detail = Format-Occupants (Get-ListeningOccupants 5432)
        if ([string]::IsNullOrWhiteSpace($detail)) {
            $detail = "un proceso que no se pudo identificar"
        }
        Stop-WithError "El puerto 5432 (PostgreSQL) esta ocupado por: $detail. SuperFercho usa el PostgreSQL de Docker y no detiene procesos existentes. Libera el puerto e intenta de nuevo."
    }
    Write-Host $compose.Output
    Stop-WithError "No se pudo iniciar Docker Compose."
}

Wait-PostgresHealthy

# --- D / E. Backend y frontend en ventanas propias ---
$occupiedAppPorts = @()
foreach ($check in @(
        @{ Port = 8080; Name = "backend Spring Boot" },
        @{ Port = 3000; Name = "frontend Next.js" }
    )) {
    $occupants = @(Get-ListeningOccupants $check.Port)
    if ($occupants.Count -gt 0) {
        $occupiedAppPorts += "El puerto $($check.Port) ($($check.Name)) esta ocupado por: $(Format-Occupants $occupants)."
    }
}
if ($occupiedAppPorts.Count -gt 0) {
    Stop-WithError (($occupiedAppPorts + "El script no detiene procesos existentes. Libera los puertos e intenta de nuevo.") -join " ")
}

$backendDir = Join-Path $Root "backend"
$frontendDir = Join-Path $Root "frontend"

Start-Process -FilePath "cmd.exe" -WorkingDirectory $backendDir -ArgumentList @(
    "/k",
    "title SuperFercho backend && mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local"
) | Out-Null

Start-Process -FilePath "cmd.exe" -WorkingDirectory $frontendDir -ArgumentList @(
    "/k",
    "title SuperFercho frontend && corepack.cmd pnpm dev"
) | Out-Null

# --- F. URLs ---
Write-Host ""
Write-Host "Frontend:"
Write-Host "http://localhost:3000"
Write-Host ""
Write-Host "Backend:"
Write-Host "http://localhost:8080"
Write-Host ""
Write-Host "PostgreSQL:"
Write-Host "localhost:5432"
Write-Host ""
Write-Host "Backend y frontend corren en ventanas aparte. Cierra esas ventanas para detenerlos."
