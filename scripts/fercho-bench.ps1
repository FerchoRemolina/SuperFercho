#Requires -Version 5.1
<#
.SYNOPSIS
    Benchmark reducido de Fercho (10 escenarios) contra backend real + Ollama local.

.DESCRIPTION
    Ejecuta 10 escenarios contra POST /api/v1/assistant/chat del backend real,
    verificando ESTADO REAL (carrito, ordenes, confirmaciones) en lugar de texto.
    Registra latencia por turno. Las metricas no observables via API (tools
    elegidas, argumentos, rondas, tokens) se reportan como N/A: el backend no
    expone el detalle de tool calls y estas no se inventan.

    No modifica codigo ni configuracion de produccion. Crea sus propios fixtures
    (productos bench + cliente + direccion) y no depende de estado previo.
#>
[CmdletBinding()]
param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$OllamaBase = "http://localhost:11434",
    [string]$Model = "fercho",
    [int]$ChatTimeoutSeconds = 880
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent $PSScriptRoot
$ReportDir = Join-Path $env:TEMP "fercho-bench"
if (-not (Test-Path $ReportDir)) { New-Item -ItemType Directory -Path $ReportDir | Out-Null }
$ReportPath = Join-Path $ReportDir ("report-" + (Get-Date -Format "yyyyMMdd-HHmmss") + ".txt")
$script:Report = New-Object System.Collections.Generic.List[string]

function Log([string]$Message) {
    $ts = (Get-Date).ToString("yyyy-MM-dd HH:mm:ss")
    $script:Report.Add("[$ts] $Message")
    $ascii = ("[$ts] " + $Message) -replace "[^\x00-\x7F]", "?"
    Write-Host $ascii
}
function LogStep([string]$Message) {
    Log ""
    Log "============================== $Message =============================="
}

function Get-DotEnvMap([string]$Path) {
    $map = @{}
    if (-not (Test-Path $Path)) { return $map }
    foreach ($line in [System.IO.File]::ReadAllLines($Path)) {
        $trimmed = $line.Trim()
        if ($trimmed -eq "" -or $trimmed.StartsWith("#")) { continue }
        $equals = $trimmed.IndexOf("=")
        if ($equals -lt 1) { continue }
        $map[$trimmed.Substring(0, $equals).Trim()] = $trimmed.Substring($equals + 1).Trim()
    }
    return $map
}

function Invoke-ApiJson {
    param(
        [string]$Method, [string]$Path, [object]$Body,
        [string]$Token = "", [int]$TimeoutSec = 30
    )
    $json = if ($null -ne $Body) { $Body | ConvertTo-Json -Depth 12 } else { $null }
    $bytes = if ($null -ne $json) { [System.Text.Encoding]::UTF8.GetBytes($json) } else { $null }
    $headers = @{ Accept = "application/json, application/problem+json" }
    if ($Token -ne "") { $headers["Authorization"] = "Bearer $Token" }
    try {
        $response = Invoke-WebRequest -Uri "$BaseUrl$Path" -Method $Method -Headers $headers `
            -ContentType "application/json; charset=utf-8" -Body $json -UseBasicParsing -TimeoutSec $TimeoutSec
        $text = [System.Text.Encoding]::UTF8.GetString($response.RawContentStream.ToArray())
        return @{ Ok = $true; Status = [int]$response.StatusCode; Body = ($text | ConvertFrom-Json); Raw = $text }
    } catch {
        $status = 0; $raw = $_.Exception.Message
        if ($_.Exception.Response) {
            $status = [int]$_.Exception.Response.StatusCode
            try {
                $stream = $_.Exception.Response.GetResponseStream()
                if ($stream) { $raw = [System.Text.Encoding]::UTF8.GetString((New-Object System.IO.StreamReader($stream)).BaseStream.ToArray()) }
            } catch { }
        }
        return @{ Ok = $false; Status = $status; Body = $null; Raw = $raw }
    }
}

function Invoke-Chat {
    param([string]$Token, [string]$ConversationId, [string]$Message, [string]$ConfirmationToken = "")
    $body = @{}
    if ($ConversationId -ne "") { $body["conversationId"] = $ConversationId }
    if ($ConfirmationToken -ne "") { $body["confirmation"] = @{ token = $ConfirmationToken } }
    else { $body["message"] = $Message }
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    $result = Invoke-ApiJson -Method "POST" -Path "/api/v1/assistant/chat" -Body $body -Token $Token -TimeoutSec $ChatTimeoutSeconds
    $sw.Stop()
    return @{ Result = $result; LatencyMs = $sw.ElapsedMilliseconds }
}

function Get-CartQuantities([string]$Token) {
    $cart = Invoke-ApiJson -Method "GET" -Path "/api/v1/cart" -Token $Token
    $map = @{}
    if ($cart.Ok -and $cart.Body.items) {
        foreach ($item in $cart.Body.items) {
            $productId = $item.productId.ToString()
            if (-not $map.ContainsKey($productId)) { $map[$productId] = 0 }
            $map[$productId] += [int]$item.quantity
        }
    }
    return $map
}

function Get-OrderCount([string]$Token) {
    $orders = Invoke-ApiJson -Method "GET" -Path "/api/v1/orders?page=0&size=1" -Token $Token
    if ($orders.Ok) { return [long]$orders.Body.totalElements }
    return -1
}

function Ensure-Session {
    # El JWT del cliente expira (~20 min) y el benchmark completo dura mas;
    # cada caso refresca el token con las MISMAS credenciales del cliente bench
    # (misma identidad, mismo carrito, mismas ordenes).
    $login = Invoke-ApiJson -Method "POST" -Path "/api/v1/auth/login" -Body @{
        email = $script:benchEmail; password = $script:benchPass
    }
    if ($login.Ok -and $login.Body.accessToken) {
        $script:TOKEN = $login.Body.accessToken
    } else {
        Log "AVISO: re-login del cliente bench fallo (HTTP $($login.Status)); se reutiliza el token anterior"
    }
}

function Format-ResultLine([string]$CaseId, [string]$Outcome, [string]$Classification, [long]$LatencyMs, [int]$Rounds) {
    Log ("{0} | resultado={1} | clasificacion={2} | latencia={3} ms | rondas_llm=N/A" -f $CaseId, $Outcome, $Classification, $LatencyMs)
}

# ============================ PREFLIGHT ============================
LogStep "PREFLIGHT"
Log "Modelo: $Model (derivado de granite4.1:8b, num_ctx 16384)"
Log "Backend: $BaseUrl"

$cat = Invoke-ApiJson -Method "GET" -Path "/api/v1/categories"
if (-not $cat.Ok) { Log "FALLO: backend no responde en $BaseUrl"; $script:Report | Out-File $ReportPath -Encoding utf8; exit 1 }
Log "[OK] Backend activo"

$ollamaVersion = Invoke-RestMethod -Uri "$OllamaBase/api/version" -TimeoutSec 5
Log "[OK] Ollama $($ollamaVersion.version) activo"
$tags = Invoke-RestMethod -Uri "$OllamaBase/api/tags" -TimeoutSec 10
$modelExists = @($tags.models | ForEach-Object { $_.name }) | Where-Object { $_ -eq $Model -or $_ -eq "$Model`:latest" -or $_ -like "$Model`:*" }
if (-not $modelExists) {
    Log "FALLO: el modelo '$Model' no existe en Ollama. Ejecuta scripts/setup-fercho.ps1"
    $script:Report | Out-File $ReportPath -Encoding utf8; exit 1
}
Log "[OK] Modelo '$Model' presente"

$envMap = Get-DotEnvMap (Join-Path $Root ".env")
$adminEmail = [string]$envMap["SUPERFERCHO_DEV_ADMIN_EMAIL"]
$adminPassword = [string]$envMap["SUPERFERCHO_DEV_ADMIN_PASSWORD"]
if ($adminEmail -eq "" -or $adminPassword -eq "") {
    Log "FALLO: SUPERFERCHO_DEV_ADMIN_EMAIL/PASSWORD no configurados en .env (necesarios para fixtures)"
    $script:Report | Out-File $ReportPath -Encoding utf8; exit 1
}
$adminLogin = Invoke-ApiJson -Method "POST" -Path "/api/v1/auth/login" -Body @{ email = $adminEmail; password = $adminPassword }
if (-not $adminLogin.Ok) {
    $loginJson = (@{ email = $adminEmail; password = "***" } | ConvertTo-Json -Depth 12)
    Log "FALLO: login admin (HTTP $($adminLogin.Status)): $($adminLogin.Raw)"
    Log "DEBUG email len=$($adminEmail.Length) pass len=$($adminPassword.Length) json len=$($loginJson.Length)"
    $script:Report | Out-File $ReportPath -Encoding utf8; exit 1
}
$adminToken = $adminLogin.Body.accessToken
Log "[OK] Admin autenticado (credenciales de .env, no se muestran)"

# ============================ FIXTURES ============================
LogStep "FIXTURES"

function Find-Products([string]$Text) {
    $r = Invoke-ApiJson -Method "GET" -Path "/api/v1/products/search?text=$([uri]::EscapeDataString($Text))" -Token $adminToken
    if ($r.Ok -and $null -ne $r.Body) { return @($r.Body) }
    return @()
}

$deslactosada = @(Find-Products "deslactosada" | Where-Object { $_.status -eq "ACTIVE" })
if ($deslactosada.Count -eq 0) {
    $anyActive = @(Find-Products "leche")
    if ($anyActive.Count -eq 0) { $anyActive = @(Invoke-ApiJson -Method "GET" -Path "/api/v1/products?size=1" -Token $adminToken).Body }
    $template = $anyActive | Select-Object -First 1
    $created = Invoke-ApiJson -Method "POST" -Path "/api/v1/products" -Token $adminToken -Body @{
        productTypeId = $template.productTypeId
        productVariantId = $template.productVariantId
        presentation = @{ quantity = 1.000; unit = "UNIT" }
        barcode = $null
        name = "Leche Deslactosada Bench 1L"
        brand = "BenchDairy"
        description = "Leche deslactosada entera de 1 litro (fixture benchmark)"
        price = @{ amount = 5000.00; currency = "COP" }
        stock = 50
        imageUrl = $null
    }
    if (-not $created.Ok) { Log "FALLO creando producto fixture: $($created.Raw)"; $script:Report | Out-File $ReportPath -Encoding utf8; exit 1 }
    $deslactosada = @($created.Body)
    Log "[OK] Creado producto fixture: Leche Deslactosada Bench 1L ($($deslactosada[0].id))"
} else {
    Log "[OK] Producto deslactosado existente: $($deslactosada[0].name) ($($deslactosada[0].id))"
}
$PRODUCT_A = $deslactosada[0]
$PRODUCT_A_ID = $PRODUCT_A.id.ToString()
$PRODUCT_A_NAME = $PRODUCT_A.name

$leches = @(Find-Products "leche" | Where-Object { $_.status -eq "ACTIVE" })
if ($leches.Count -lt 2) {
    $created = Invoke-ApiJson -Method "POST" -Path "/api/v1/products" -Token $adminToken -Body @{
        productTypeId = $PRODUCT_A.productTypeId
        productVariantId = $PRODUCT_A.productVariantId
        presentation = @{ quantity = 1.000; unit = "UNIT" }
        barcode = $null
        name = "Leche Entera Bench 1L"
        brand = "BenchDairy"
        description = "Leche entera de 1 litro (fixture benchmark)"
        price = @{ amount = 5500.00; currency = "COP" }
        stock = 50
        imageUrl = $null
    }
    if ($created.Ok) { Log "[OK] Creada segunda leche para desambiguacion: Leche Entera Bench 1L" }
} else {
    Log "[OK] Ya existen $($leches.Count) productos 'leche' activos (contexto de desambiguacion real)"
}

$unicornio = @(Find-Products "unicornio")
if ($unicornio.Count -gt 0) {
    Log "FALLO: 'unicornio' deberia no existir en el catalogo y aparecio $($unicornio.Count) vez/veces"
    $script:Report | Out-File $ReportPath -Encoding utf8; exit 1
}
Log "[OK] Producto inexistente verificado: no hay 'unicornio' en el catalogo"

$suffix = Get-Random -Minimum 10000 -Maximum 99999
$benchEmail = "fercho-bench.$suffix@test.local"
$reg = Invoke-ApiJson -Method "POST" -Path "/api/v1/customers" -Body @{
    documentType = "CC"; documentNumber = "$suffix$suffix"; firstName = "Bench"; lastName = "Fercho";
    email = $benchEmail; phone = "30000$suffix"; password = "Bench#2026x"
}
if (-not $reg.Ok) { Log "FALLO registrando cliente bench: $($reg.Raw)"; $script:Report | Out-File $ReportPath -Encoding utf8; exit 1 }
$login = Invoke-ApiJson -Method "POST" -Path "/api/v1/auth/login" -Body @{ email = $benchEmail; password = "Bench#2026x" }
$TOKEN = $login.Body.accessToken
$script:benchEmail = $benchEmail
$script:benchPass = "Bench#2026x"
Log "[OK] Cliente bench creado: $benchEmail"

$addr = Invoke-ApiJson -Method "POST" -Path "/api/v1/addresses" -Token $TOKEN -Body @{
    label = "Casa"; recipientName = "Bench Fercho"; addressLine = "Calle Bench 123";
    additionalInfo = "Apt 501"; city = "Bogota"; department = "Cundinamarca"; phone = "3000000000"; isDefault = $true
}
if (-not $addr.Ok) { Log "FALLO creando direccion: $($addr.Raw)"; $script:Report | Out-File $ReportPath -Encoding utf8; exit 1 }
Log "[OK] Direccion de compra creada (default)"

$CASES = New-Object System.Collections.Generic.List[object]

# ============================ CASOS ============================
LogStep "CASO 1 - Busqueda especifica"
Ensure-Session
$conv = ""
$swC1 = [System.Diagnostics.Stopwatch]::StartNew()
try {
$c1 = Invoke-Chat -Token $TOKEN -ConversationId "" -Message "Busca leche deslactosada."
if ($c1.Result.Ok) {
    $conv = $c1.Result.Body.conversationId.ToString()
    $text = $c1.Result.Body.assistantMessage
    $mentions = $text -match "Deslactosada|deslactosada"
    Log "input: Busca leche deslactosada."
    Log "respuesta: $text"
    if ($mentions) { Log ("{0} | SUCCESS | modelo-fallo=NO | latencia={1} ms" -f "C1", $c1.LatencyMs); $CASES.Add(@{ Id = "C1"; Outcome = "SUCCESS"; Class = "-"; Latency = $c1.LatencyMs }) }
    else { Log "C1 | MODEL-FAIL | la respuesta no referencia el producto deslactosado existente | latencia=$($c1.LatencyMs) ms"; $CASES.Add(@{ Id = "C1"; Outcome = "MODEL-FAIL"; Class = "modelo: no uso el resultado de busqueda"; Latency = $c1.LatencyMs }) }
} else {
    Log "C1 | ERROR HTTP $($c1.Result.Status): $($c1.Result.Raw) | latencia=$($c1.LatencyMs) ms"
    $CASES.Add(@{ Id = "C1"; Outcome = "ERROR"; Class = "infra/modelo"; Latency = $c1.LatencyMs })
}
} catch {
    Log "C1 | ERROR | excepcion: $($_.Exception.Message) | latencia=$($swC1.ElapsedMilliseconds) ms"
    $CASES.Add(@{ Id = "C1"; Outcome = "ERROR"; Class = "excepcion no capturada"; Latency = $swC1.ElapsedMilliseconds })
}

LogStep "CASO 2 - Catalogo (search vs list)"
Ensure-Session
$swC2 = [System.Diagnostics.Stopwatch]::StartNew()
try {
$c2 = Invoke-Chat -Token $TOKEN -ConversationId $conv -Message "Muestrame el catalogo de productos."
if ($c2.Result.Ok) {
    $text = $c2.Result.Body.assistantMessage
    Log "input: Muestrame el catalogo de productos."
    Log "respuesta (primeros 900 chars): $($text.Substring(0, [Math]::Min(900, $text.Length)))"
    Log "NOTA: tool elegida (search_products vs list_products) no es observable via API -> N/A. Texto registrado para analisis."
    Log ("{0} | SUCCESS (200, respuesta util) | latencia={1} ms" -f "C2", $c2.LatencyMs)
    $CASES.Add(@{ Id = "C2"; Outcome = "SUCCESS"; Class = "-"; Latency = $c2.LatencyMs })
} else {
    Log "C2 | ERROR HTTP $($c2.Result.Status): $($c2.Result.Raw) | latencia=$($c2.LatencyMs) ms"
    $CASES.Add(@{ Id = "C2"; Outcome = "ERROR"; Class = "infra/modelo"; Latency = $c2.LatencyMs })
}
} catch {
    Log "C2 | ERROR | excepcion: $($_.Exception.Message) | latencia=$($swC2.ElapsedMilliseconds) ms"
    $CASES.Add(@{ Id = "C2"; Outcome = "ERROR"; Class = "excepcion no capturada"; Latency = $swC2.ElapsedMilliseconds })
}

LogStep "CASO 3 - Agregar al carrito"
Ensure-Session
$swC3 = [System.Diagnostics.Stopwatch]::StartNew()
try {
$cartBefore = Get-CartQuantities $TOKEN
Log "carrito antes: $($cartBefore.Keys.Count) productos"
$c3 = Invoke-Chat -Token $TOKEN -ConversationId $conv -Message "Agrega dos unidades de $PRODUCT_A_NAME al carrito."
$cartAfter = Get-CartQuantities $TOKEN
$expectedAfter = $cartBefore[$PRODUCT_A_ID] + 2
$gotQty = 0
if ($cartAfter.ContainsKey($PRODUCT_A_ID)) { $gotQty = $cartAfter[$PRODUCT_A_ID] }
if ($c3.Result.Ok -and $gotQty -eq $expectedAfter) {
    Log "C3 | SUCCESS | carrito: producto A cantidad=$gotQty (esperado $expectedAfter) | latencia=$($c3.LatencyMs) ms"
    $CASES.Add(@{ Id = "C3"; Outcome = "SUCCESS"; Class = "-"; Latency = $c3.LatencyMs })
} elseif (-not $c3.Result.Ok) {
    Log "C3 | ERROR HTTP $($c3.Result.Status): $($c3.Result.Raw) | latencia=$($c3.LatencyMs) ms"
    $CASES.Add(@{ Id = "C3"; Outcome = "ERROR"; Class = "infra/modelo"; Latency = $c3.LatencyMs })
} else {
    $respText = if ($null -ne $c3.Result.Body) { $c3.Result.Body.assistantMessage } else { "N/A" }
    Log "C3 | MODEL-FAIL | qty=$gotQty esperado=$expectedAfter | respuesta: $respText | latencia=$($c3.LatencyMs) ms"
    $CASES.Add(@{ Id = "C3"; Outcome = "MODEL-FAIL"; Class = "modelo: estado del carrito incorrecto"; Latency = $c3.LatencyMs })
}
} catch {
    Log "C3 | ERROR | excepcion: $($_.Exception.Message) | latencia=$($swC3.ElapsedMilliseconds) ms"
    $CASES.Add(@{ Id = "C3"; Outcome = "ERROR"; Class = "excepcion no capturada"; Latency = $swC3.ElapsedMilliseconds })
}

LogStep "CASO 4 - Consultar carrito"
Ensure-Session
$swC4 = [System.Diagnostics.Stopwatch]::StartNew()
try {
$c4 = Invoke-Chat -Token $TOKEN -ConversationId $conv -Message "Que tengo en mi carrito?"
$cartNow = Get-CartQuantities $TOKEN
if ($c4.Result.Ok -and $cartNow.ContainsKey($PRODUCT_A_ID) -and $cartNow[$PRODUCT_A_ID] -eq $expectedAfter) {
    $text = $c4.Result.Body.assistantMessage
    $mentionsProduct = $text -match "Deslactosada|deslactosada"
    Log "respuesta: $text"
    if ($mentionsProduct) {
        Log "C4 | SUCCESS | respuesta consistente con estado real | latencia=$($c4.LatencyMs) ms"
        $CASES.Add(@{ Id = "C4"; Outcome = "SUCCESS"; Class = "-"; Latency = $c4.LatencyMs })
    } else {
        Log "C4 | MODEL-FAIL | estado real correcto pero la respuesta no menciona el producto del carrito | latencia=$($c4.LatencyMs) ms"
        $CASES.Add(@{ Id = "C4"; Outcome = "MODEL-FAIL"; Class = "modelo: respuesta no refleja el carrito"; Latency = $c4.LatencyMs })
    }
} else {
    Log "C4 | FALLO | HTTP=$($c4.Result.Ok) estado del carrito inesperado | latencia=$($c4.LatencyMs) ms"
    $CASES.Add(@{ Id = "C4"; Outcome = "ERROR"; Class = "infra/modelo"; Latency = $c4.LatencyMs })
}
} catch {
    Log "C4 | ERROR | excepcion: $($_.Exception.Message) | latencia=$($swC4.ElapsedMilliseconds) ms"
    $CASES.Add(@{ Id = "C4"; Outcome = "ERROR"; Class = "excepcion no capturada"; Latency = $swC4.ElapsedMilliseconds })
}

LogStep "CASO 5 - Producto inexistente (no inventar)"
Ensure-Session
$swC5 = [System.Diagnostics.Stopwatch]::StartNew()
try {
$c5 = Invoke-Chat -Token $TOKEN -ConversationId $conv -Message "Busca yogur de unicornio Bench."
$cartAfter5 = Get-CartQuantities $TOKEN
$text5 = ""
if ($c5.Result.Ok) { $text5 = $c5.Result.Body.assistantMessage; Log "respuesta: $text5" } else { Log "HTTP $($c5.Result.Status): $($c5.Result.Raw) | latencia=$($c5.LatencyMs) ms" }
$inventedPrice = $text5 -match "\$\s?\d|\d+\s?(mil|pesos)"
$cartUnchanged = $true
if (-not $cartAfter5.ContainsKey($PRODUCT_A_ID) -or $cartAfter5[$PRODUCT_A_ID] -lt $expectedAfter) { $cartUnchanged = $false }
$noInvent = ($text5 -match "no (encontre|encontr)|no (tengo|contamos)|sin resultados|no disponible|no hay") -and -not ($text5 -match "Agregad[oa]|agregue|agregado al carrito")
if ($c5.Result.Ok -and $noInvent -and $text5 -notmatch "unicornio.*(hay|disponible|tenemos).*\d") {
    Log "C5 | SUCCESS | communicate no-encontrado sin inventar | latencia=$($c5.LatencyMs) ms"
    $CASES.Add(@{ Id = "C5"; Outcome = "SUCCESS"; Class = "-"; Latency = $c5.LatencyMs })
} elseif (-not $c5.Result.Ok) {
    Log "C5 | ERROR HTTP $($c5.Result.Status) | latencia=$($c5.LatencyMs) ms"
    $CASES.Add(@{ Id = "C5"; Outcome = "ERROR"; Class = "infra/modelo"; Latency = $c5.LatencyMs })
} else {
    Log "C5 | MODEL-FAIL | posible invencion (precio=$inventedPrice, carrito-intacto=$cartUnchanged) | latencia=$($c5.LatencyMs) ms"
    $CASES.Add(@{ Id = "C5"; Outcome = "MODEL-FAIL"; Class = "modelo: invencion de producto/datos"; Latency = $c5.LatencyMs })
}
} catch {
    Log "C5 | ERROR | excepcion: $($_.Exception.Message) | latencia=$($swC5.ElapsedMilliseconds) ms"
    $CASES.Add(@{ Id = "C5"; Outcome = "ERROR"; Class = "excepcion no capturada"; Latency = $swC5.ElapsedMilliseconds })
}

LogStep "CASO 6 - Desambiguacion"
Ensure-Session
$swC6 = [System.Diagnostics.Stopwatch]::StartNew()
try {
$cartPre6 = Get-CartQuantities $TOKEN
$c6 = Invoke-Chat -Token $TOKEN -ConversationId $conv -Message "Quiero comprar la leche."
$cartPost6 = Get-CartQuantities $TOKEN
$cartDelta6 = 0
foreach ($k in $cartPost6.Keys) { $before = 0; if ($cartPre6.ContainsKey($k)) { $before = $cartPre6[$k] }; $cartDelta6 += ($cartPost6[$k] - $before) }
$text6 = ""
if ($c6.Result.Ok) { $text6 = $c6.Result.Body.assistantMessage; Log "respuesta: $text6" } else { Log "HTTP $($c6.Result.Status): $($c6.Result.Raw) | latencia=$($c6.LatencyMs) ms" }
if ($c6.Result.Ok -and $cartDelta6 -eq 0 -and ($text6 -match "\?" -or $text6 -match "(?i)(cual|cuales|aclar|especifica|referir)")) {
    Log "C6 | SUCCESS | pidio aclaracion sin mutar el carrito | latencia=$($c6.LatencyMs) ms"
    $CASES.Add(@{ Id = "C6"; Outcome = "SUCCESS"; Class = "-"; Latency = $c6.LatencyMs })
} elseif ($c6.Result.Ok -and $cartDelta6 -ne 0) {
    Log "C6 | MODEL-FAIL | asumio arbitrariamente: carrito delta=$cartDelta6 sin aclaracion | latencia=$($c6.LatencyMs) ms"
    $CASES.Add(@{ Id = "C6"; Outcome = "MODEL-FAIL"; Class = "modelo: desambiguacion ignorada"; Latency = $c6.LatencyMs })
} elseif ($c6.Result.Ok) {
    Log "C6 | MODEL-FAIL | no pidio aclaracion ni actuo (ambiguo) | latencia=$($c6.LatencyMs) ms"
    $CASES.Add(@{ Id = "C6"; Outcome = "MODEL-FAIL"; Class = "modelo"; Latency = $c6.LatencyMs })
} else {
    Log "C6 | ERROR HTTP $($c6.Result.Status) | latencia=$($c6.LatencyMs) ms"
    $CASES.Add(@{ Id = "C6"; Outcome = "ERROR"; Class = "infra/modelo"; Latency = $c6.LatencyMs })
}
} catch {
    Log "C6 | ERROR | excepcion: $($_.Exception.Message) | latencia=$($swC6.ElapsedMilliseconds) ms"
    $CASES.Add(@{ Id = "C6"; Outcome = "ERROR"; Class = "excepcion no capturada"; Latency = $swC6.ElapsedMilliseconds })
}

LogStep "CASO 7 - Checkout SIN confirmacion (critico)"
Ensure-Session
$token7 = ""
$swC7 = [System.Diagnostics.Stopwatch]::StartNew()
try {
    $ordersBefore7 = Get-OrderCount $TOKEN
    Log "ordenes antes: $ordersBefore7"

    $cartPre7 = Get-CartQuantities $TOKEN
    Log "carrito antes de C7: $($cartPre7.Count) productos / $(@($cartPre7.Values | Measure-Object -Sum).Sum) items"

    if ($cartPre7.Count -eq 0) {
        Log "AVISO: carrito vacio al iniciar C7 (revisar resultado de C3); se ejecuta igual y se clasifica segun resultado"
    }

    # Primero obtenemos explícitamente las direcciones disponibles.
    $c7a = Invoke-Chat -Token $TOKEN -ConversationId $conv -Message "Muéstrame mis direcciones."

    if (-not $c7a.Result.Ok) {
        Log "C7A | ERROR HTTP $($c7a.Result.Status): $($c7a.Result.Raw) | latencia=$($c7a.LatencyMs) ms"
        $CASES.Add(@{ Id = "C7"; Outcome = "ERROR"; Class = "infra/modelo"; Latency = $c7a.LatencyMs })
    } else {
        Log "C7A respuesta: $($c7a.Result.Body.assistantMessage)"

        # Usamos el nombre de la dirección para que Fercho resuelva
        # la dirección mediante list_addresses y prepare el checkout.
        $c7b = Invoke-Chat -Token $TOKEN -ConversationId $conv -Message "Usa Casa para finalizar la compra con pago contra entrega."

        if (-not $c7b.Result.Ok) {
            Log "C7B | ERROR HTTP $($c7b.Result.Status): $($c7b.Result.Raw) | latencia=$($c7a.LatencyMs + $c7b.LatencyMs) ms"
            $CASES.Add(@{ Id = "C7"; Outcome = "ERROR"; Class = "infra/modelo"; Latency = ($c7a.LatencyMs + $c7b.LatencyMs) })
        } else {
            $awaiting = $c7b.Result.Body.awaitingConfirmation
            $token7 = [string]$c7b.Result.Body.confirmationToken

            Log "C7B respuesta: $($c7b.Result.Body.assistantMessage)"
            Log "awaitingConfirmation=$awaiting | confirmationToken presente=$($token7 -ne '')"

            $latencyC7 = $c7a.LatencyMs + $c7b.LatencyMs

            $ordersAfter7 = Get-OrderCount $TOKEN

            if ($ordersAfter7 -gt $ordersBefore7) {
                Log "C7 | BLOCKER | se creo una orden SIN confirmacion explicita ($ordersBefore7 -> $ordersAfter7) | latencia=$latencyC7 ms"
                $CASES.Add(@{ Id = "C7"; Outcome = "BLOCKER"; Class = "deterministico: checkout sin autorizacion"; Latency = $latencyC7 })
            } elseif ($awaiting -and $token7 -ne "") {
                Log "C7 | SUCCESS | checkout preparado, esperando confirmacion; ordenes sin cambio ($ordersAfter7) | latencia=$latencyC7 ms"
                $CASES.Add(@{ Id = "C7"; Outcome = "SUCCESS"; Class = "-"; Latency = $latencyC7 })
            } else {
                Log "C7 | MODEL-FAIL | no preparo el checkout (sin token) pero tampoco ejecuto nada sin autorizacion | latencia=$latencyC7 ms"
                $CASES.Add(@{ Id = "C7"; Outcome = "MODEL-FAIL"; Class = "modelo: no completo la preparacion"; Latency = $latencyC7 })
            }
        }
    }
} catch {
    Log "C7 | ERROR | excepcion: $($_.Exception.Message) | latencia=$($swC7.ElapsedMilliseconds) ms"
    $CASES.Add(@{ Id = "C7"; Outcome = "ERROR"; Class = "excepcion no capturada"; Latency = $swC7.ElapsedMilliseconds })
}

LogStep "CASO 8 - Checkout CON confirmacion (flujo real)"
Ensure-Session
$swC8 = [System.Diagnostics.Stopwatch]::StartNew()
try {
    if ($token7 -ne "") {
        $c8 = Invoke-Chat -Token $TOKEN -ConversationId $conv -ConfirmationToken $token7 -Message ""
        $ordersAfter8 = Get-OrderCount $TOKEN
        $cartAfter8 = Get-CartQuantities $TOKEN

        Log "ordenes despues de confirmar: $ordersAfter8 | carrito vacio: $($cartAfter8.Count -eq 0)"

        if ($c8.Result.Ok -and $ordersAfter8 -eq ($ordersBefore7 + 1) -and $cartAfter8.Count -eq 0) {
            $orderDetail = Invoke-ApiJson -Method "GET" -Path "/api/v1/orders?page=0&size=5" -Token $TOKEN
            $lastOrder = $orderDetail.Body.items | Select-Object -First 1

            Log "orden creada: numero=$($lastOrder.orderNumber) estado=$($lastOrder.status) total=$($lastOrder.total.amount) $($lastOrder.total.currency)"
            Log "C8 | SUCCESS | orden creada mediante el mecanismo real de confirmacion | latencia=$($c8.LatencyMs) ms"
            $CASES.Add(@{ Id = "C8"; Outcome = "SUCCESS"; Class = "-"; Latency = $c8.LatencyMs })
        } elseif (-not $c8.Result.Ok) {
            Log "C8 | ERROR HTTP $($c8.Result.Status): $($c8.Result.Raw) | latencia=$($c8.LatencyMs) ms"
            $CASES.Add(@{ Id = "C8"; Outcome = "ERROR"; Class = "infra/modelo"; Latency = $c8.LatencyMs })
        } else {
            $respText = if ($null -ne $c8.Result.Body) { $c8.Result.Body.assistantMessage } else { "N/A" }

            Log "C8 | MODEL-FAIL | ordenesperada=$($ordersBefore7 + 1) obtenida=$ordersAfter8 | carritoVacio=$($cartAfter8.Count -eq 0) | respuesta: $respText | latencia=$($c8.LatencyMs) ms"
            $CASES.Add(@{ Id = "C8"; Outcome = "MODEL-FAIL"; Class = "modelo: confirmacion no concreta compra"; Latency = $c8.LatencyMs })
        }
    } else {
        Log "C8 | MODEL-FAIL | C7 no produjo token de confirmacion; no se puede ejecutar el flujo real"
        $CASES.Add(@{ Id = "C8"; Outcome = "MODEL-FAIL"; Class = "heredado de C7"; Latency = 0 })
    }
} catch {
    Log "C8 | ERROR | excepcion: $($_.Exception.Message) | latencia=$($swC8.ElapsedMilliseconds) ms"
    $CASES.Add(@{ Id = "C8"; Outcome = "ERROR"; Class = "excepcion no capturada"; Latency = $swC8.ElapsedMilliseconds })
}

LogStep "CASO 9 - Multi-turno (referencia contextual)"
Ensure-Session
$conv9 = ""
$swC9 = [System.Diagnostics.Stopwatch]::StartNew()
try {
$c9a = Invoke-Chat -Token $TOKEN -ConversationId "" -Message "Busca leche deslactosada."
if ($c9a.Result.Ok) { $conv9 = $c9a.Result.Body.conversationId.ToString(); Log "turno1 respuesta: $($c9a.Result.Body.assistantMessage)" }
$cartPre9 = Get-CartQuantities $TOKEN
$c9b = Invoke-Chat -Token $TOKEN -ConversationId $conv9 -Message "Agrega dos de esas."
$cartPost9 = Get-CartQuantities $TOKEN
$delta9 = 0
if ($cartPost9.ContainsKey($PRODUCT_A_ID)) { $delta9 = $cartPost9[$PRODUCT_A_ID] }
$before9 = 0
if ($cartPre9.ContainsKey($PRODUCT_A_ID)) { $before9 = $cartPre9[$PRODUCT_A_ID] }
$delta9 = $delta9 - $before9
if ($c9b.Result.Ok -and $delta9 -eq 2) {
    Log "C9 | SUCCESS | 'dos de esas' resolvio al producto correcto (delta +2) | latencia1=$($c9a.LatencyMs) ms latencia2=$($c9b.LatencyMs) ms"
    $CASES.Add(@{ Id = "C9"; Outcome = "SUCCESS"; Class = "-"; Latency = ($c9a.LatencyMs + $c9b.LatencyMs) })
} elseif ($c9b.Result.Ok) {
    Log "C9 | MODEL-FAIL | referencia contextual no resuelta (delta=$delta9, esperado 2) | latencia1=$($c9a.LatencyMs) ms latencia2=$($c9b.LatencyMs) ms"
    $CASES.Add(@{ Id = "C9"; Outcome = "MODEL-FAIL"; Class = "modelo: contexto multi-turno"; Latency = ($c9a.LatencyMs + $c9b.LatencyMs) })
} else {
    Log "C9 | ERROR HTTP $($c9b.Result.Status) | latencia1=$($c9a.LatencyMs) ms latencia2=$($c9b.LatencyMs) ms"
    $CASES.Add(@{ Id = "C9"; Outcome = "ERROR"; Class = "infra/modelo"; Latency = ($c9a.LatencyMs + $c9b.LatencyMs) })
}
} catch {
    Log "C9 | ERROR | excepcion: $($_.Exception.Message) | latencia=$($swC9.ElapsedMilliseconds) ms"
    $CASES.Add(@{ Id = "C9"; Outcome = "ERROR"; Class = "excepcion no capturada"; Latency = $swC9.ElapsedMilliseconds })
}

LogStep "CASO 10 - Multi-tool (consulta compuesta)"
Ensure-Session
$swC10 = [System.Diagnostics.Stopwatch]::StartNew()
try {
$c10 = Invoke-Chat -Token $TOKEN -ConversationId $conv9 -Message "Cuanto cuesta la $PRODUCT_A_NAME y que tengo en mi carrito?"
$text10 = ""
if ($c10.Result.Ok) {
    $text10 = $c10.Result.Body.assistantMessage
    Log "respuesta: $text10"
    $priceInt = [string][int][math]::Floor([double]$PRODUCT_A.price.amount)
    if ($priceInt.Length -gt 3) {
        $priceRegex = $priceInt.Substring(0, $priceInt.Length - 3) + "[.,\s]?" + $priceInt.Substring($priceInt.Length - 3)
    } else {
        $priceRegex = $priceInt
    }
    $mentionsPrice = $text10 -match $priceRegex
    $mentionsCart = $text10 -match "(?i)(carrito)"
    if ($mentionsPrice -and $mentionsCart) {
        Log "C10 | SUCCESS | respuesta cubre precio ($priceRegex) y estado del carrito | latencia=$($c10.LatencyMs) ms"
        $CASES.Add(@{ Id = "C10"; Outcome = "SUCCESS"; Class = "-"; Latency = $c10.LatencyMs })
    } else {
        Log "C10 | MODEL-FAIL | precio=$mentionsPrice (esperado regex $priceRegex) carrito=$mentionsCart | latencia=$($c10.LatencyMs) ms"
        $CASES.Add(@{ Id = "C10"; Outcome = "MODEL-FAIL"; Class = "modelo: consulta compuesta incompleta"; Latency = $c10.LatencyMs })
    }
} else {
    Log "C10 | ERROR HTTP $($c10.Result.Status): $($c10.Result.Raw) | latencia=$($c10.LatencyMs) ms"
    $CASES.Add(@{ Id = "C10"; Outcome = "ERROR"; Class = "infra/modelo"; Latency = $c10.LatencyMs })
}
} catch {
    Log "C10 | ERROR | excepcion: $($_.Exception.Message) | latencia=$($swC10.ElapsedMilliseconds) ms"
    $CASES.Add(@{ Id = "C10"; Outcome = "ERROR"; Class = "excepcion no capturada"; Latency = $swC10.ElapsedMilliseconds })
}

# ============================ REPORTE ============================
LogStep "RESUMEN"
$success = @($CASES | Where-Object { $_.Outcome -eq "SUCCESS" }).Count
$modelFails = @($CASES | Where-Object { $_.Outcome -eq "MODEL-FAIL" }).Count
$blockers = @($CASES | Where-Object { $_.Outcome -eq "BLOCKER" }).Count
$errors = @($CASES | Where-Object { $_.Outcome -eq "ERROR" }).Count
$totalLatency = ($CASES | ForEach-Object { [long]$_.Latency } | Measure-Object -Sum).Sum
if ($null -eq $totalLatency) { $totalLatency = 0 }
Log "Casos: $($CASES.Count) | SUCCESS=$success | MODEL-FAIL=$modelFails | BLOCKER=$blockers | ERROR=$errors"
Log "Latencia total (suma de turnos): $totalLatency ms"
Log ""
Log "tool_elegida/argumentos/rondas/tokens: N/A para todos los casos (el backend no expone"
Log "el detalle de tool calls; metricas no inventadas). Diagnostico de tool selection requiere"
Log "un endpoint de observabilidad de conversaciones (propuesta futura, requiere aprobacion)."
Log ""
foreach ($c in $CASES) {
    Log ("{0,-4} {1,-11} {2}" -f $c.Id, $c.Outcome, $c.Class)
}

$script:Report | Out-File $ReportPath -Encoding utf8
Write-Host ""
Write-Host "Reporte completo guardado en: $ReportPath"
