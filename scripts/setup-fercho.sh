#!/usr/bin/env bash
#
# SuperFercho - setup de Fercho AI (100% local) con Ollama.
# Idempotente y no destructivo: verifica prerequisitos, detecta/arranca Ollama,
# descarga el modelo segun el hardware, crea el modelo "fercho" con contexto
# ampliado, escribe la configuracion local en .env (sin pisar valores no vacios)
# y ejecuta un probe de tool calling.
#
# Nunca lee credenciales de OpenCode, nunca descarga ni copia API keys y nunca
# elimina procesos ni archivos del usuario.
#
# Uso:
#   ./scripts/setup-fercho.sh
#   ./scripts/setup-fercho.sh --model granite4.1:8b
set -uo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OLLAMA_BASE="http://localhost:11434"
NUM_CTX=16384
FERCHO_MODEL="fercho"
MODEL_OVERRIDE=""

SUMMARY_OK=()
SUMMARY_WARN=()

log_step() { printf '\n== %s\n' "$1"; }
log_ok()   { printf '  [OK] %s\n' "$1"; SUMMARY_OK+=("$1"); }
log_warn() { printf '  [AVISO] %s\n' "$1"; SUMMARY_WARN+=("$1"); }
fail()     { printf '\nERROR: %s\nSetup abortado. No se modifico nada que requiera deshacer.\n' "$1" >&2; exit 1; }

http_status() {
  local url="$1" timeout="${2:-3}"
  curl -s -o /dev/null -w '%{http_code}' --max-time "$timeout" "$url" 2>/dev/null || echo "000"
}

# --- Parseo de argumentos -------------------------------------------------
while [[ $# -gt 0 ]]; do
  case "$1" in
    --model) [[ $# -ge 2 ]] || fail "--model requiere un valor"; MODEL_OVERRIDE="$2"; shift 2 ;;
    -h|--help) grep '^#' "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
    *) fail "Argumento desconocido: $1 (usa --model <tag>)" ;;
  esac
done

printf 'SuperFercho - setup de Fercho AI (Ollama local)\nRepositorio: %s\n' "$ROOT"

# --- 1-3. Prerequisitos de la aplicacion (no fatales para el setup de IA) -
log_step "Prerequisitos de la aplicacion SuperFercho"

JAVA_OK=0
if command -v java >/dev/null 2>&1; then
  if java -version 2>&1 | grep -q 'version "21'; then JAVA_OK=1; fi
fi
DOCKER_OK=0
if command -v docker >/dev/null 2>&1 && docker info --format '{{.ServerVersion}}' >/dev/null 2>&1; then
  DOCKER_OK=1
fi
NODE_OK=0
if command -v node >/dev/null 2>&1; then
  if command -v corepack >/dev/null 2>&1; then NODE_OK=1; fi
fi

[[ $JAVA_OK -eq 1 ]] && log_ok "Java 21 disponible" \
  || log_warn "Java 21 NO disponible. Instala JDK 21 (https://adoptium.net). La app no levantara sin esto, pero el setup de IA puede continuar."
[[ $DOCKER_OK -eq 1 ]] && log_ok "Docker disponible" \
  || log_warn "Docker NO disponible. Instala Docker Desktop (https://www.docker.com/products/docker-desktop). La app no levantara sin esto, pero el setup de IA puede continuar."
[[ $NODE_OK -eq 1 ]] && log_ok "Node/corepack disponibles" \
  || log_warn "Node.js 20.9+ / corepack NO disponible (https://nodejs.org). La app no levantara sin esto, pero el setup de IA puede continuar."

# --- 4. Ollama instalado ---------------------------------------------------
log_step "Ollama"
command -v ollama >/dev/null 2>&1 || {
  printf '  Ollama no esta instalado.\n\n  Instalalo con una de estas opciones:\n' >&2
  printf '    macOS   : brew install ollama            (o https://ollama.com/download)\n' >&2
  printf '    Linux   : curl -fsSL https://ollama.com/install.sh | sh\n' >&2
  printf '    Windows : winget install Ollama.Ollama   (o https://ollama.com/download)\n\n' >&2
  printf '  Luego vuelve a ejecutar este script.\n' >&2
  fail "Ollama no esta instalado."
}
log_ok "Ollama instalado ($(command -v ollama))"

# --- 5-6. Ollama ejecutandose ----------------------------------------------
STATUS=$(http_status "$OLLAMA_BASE/api/version" 3)
if [[ "$STATUS" != "200" ]]; then
  log_step "Ollama esta instalado pero no responde; intentando arrancarlo"
  if command -v lsof >/dev/null 2>&1 && lsof -iTCP:11434 -sTCP:LISTEN >/dev/null 2>&1; then
    fail "El puerto 11434 esta ocupado por un proceso que no responde como Ollama. Revisa 'lsof -iTCP:11434 -sTCP:LISTEN'; este script no detiene procesos."
  fi
  case "$(uname -s)" in
    Darwin) open -a Ollama || true ;;
    *) nohup ollama serve >/dev/null 2>&1 & ;;
  esac
  printf '  Esperando respuesta de Ollama...\n'
  DEADLINE=$((SECONDS + 30))
  while (( SECONDS < DEADLINE )); do
    STATUS=$(http_status "$OLLAMA_BASE/api/version" 3)
    [[ "$STATUS" == "200" ]] && break
    sleep 2
  done
  [[ "$STATUS" == "200" ]] || fail "Ollama no quedo disponible en $OLLAMA_BASE tras 30 segundos. Inicialo manualmente y vuelve a ejecutar."
fi
OLLAMA_VERSION=$(curl -s --max-time 5 "$OLLAMA_BASE/api/version" | sed -E 's/.*"version"[[:space:]]*:[[:space:]]*"([^"]+)".*/\1/')
log_ok "Servidor Ollama activo en $OLLAMA_BASE (version ${OLLAMA_VERSION:-desconocida})"

# --- 7. Hardware -------------------------------------------------------------
log_step "Hardware detectado"
OS=$(uname -s)
RAM_GB=0
if [[ "$OS" == "Darwin" ]]; then
  RAM_BYTES=$(sysctl -n hw.memsize 2>/dev/null || echo 0)
  RAM_GB=$(( RAM_BYTES / (1024 * 1024 * 1024) ))
else
  RAM_KB=$(awk '/MemTotal/ {print $2}' /proc/meminfo 2>/dev/null || echo 0)
  RAM_GB=$(( RAM_KB / 1024 ))
fi
if (( RAM_GB > 0 )); then
  printf '  RAM total: %s GB\n' "$RAM_GB"
else
  printf '  RAM total: no se pudo determinar\n'
fi
if command -v nvidia-smi >/dev/null 2>&1; then
  VRAM=$(nvidia-smi --query-gpu=memory.total --format=csv,noheader,nounits 2>/dev/null | head -1)
  [[ -n "$VRAM" ]] && printf '  GPU NVIDIA detectada, VRAM ~%s MB\n' "$VRAM"
else
  printf '  GPU dedicada: no detectada via nvidia-smi (inferencia por CPU si aplica)\n'
fi

# --- 8. Seleccion de modelo --------------------------------------------------
log_step "Seleccion de modelo"
if [[ -n "$MODEL_OVERRIDE" ]]; then
  BASE_MODEL="$MODEL_OVERRIDE"
  printf '  Modelo seleccionado manualmente: %s\n' "$BASE_MODEL"
  if (( RAM_GB > 0 && RAM_GB < 16 )) && [[ "$BASE_MODEL" == gpt-oss:20b* ]]; then
    log_warn "Elegiste $BASE_MODEL con menos de 16 GB de RAM. Puede quedarse sin memoria o ser muy lento. Considera granite4.1:8b."
  fi
elif (( RAM_GB >= 16 )); then
  BASE_MODEL="gpt-oss:20b"
  printf '  RAM %s GB >= 16 GB -> candidato: %s (Apache 2.0, tool calling nativo)\n' "$RAM_GB" "$BASE_MODEL"
elif (( RAM_GB > 0 )); then
  BASE_MODEL="granite4.1:8b"
  printf '  RAM %s GB < 16 GB -> candidato ligero: %s (Apache 2.0, multilingue)\n' "$RAM_GB" "$BASE_MODEL"
  log_warn "Con menos de 16 GB la calidad y velocidad del razonamiento seran menores."
else
  BASE_MODEL="granite4.1:8b"
  log_warn "No se pudo detectar la RAM; se usa el candidato ligero $BASE_MODEL. Usa --model para elegir otro."
fi
printf '  Nota: ningun modelo esta aprobado de antemano; el probe y el benchmark deciden.\n'

case "$BASE_MODEL" in
  gpt-oss:20b) NEED_GB=16 ;;
  granite4.1:8b) NEED_GB=8 ;;
  *) NEED_GB=25; log_warn "No conocemos el tamano de '$BASE_MODEL'; exigimos 25 GB libres por seguridad." ;;
esac

# --- 9. Espacio en disco -------------------------------------------------------
log_step "Espacio en disco"
MODELS_DIR="${OLLAMA_MODELS:-$HOME/.ollama}"
MODELS_DIR="${MODELS_DIR%%$'\n'*}"
DISK_PATH="$MODELS_DIR"
[[ -d "$DISK_PATH" ]] || DISK_PATH="$(dirname "$MODELS_DIR")"
[[ -d "$DISK_PATH" ]] || DISK_PATH="$HOME"
FREE_KB=$(df -Pk "$DISK_PATH" | awk 'NR==2 {print $4}')
if [[ -n "$FREE_KB" ]]; then
  FREE_GB=$(( FREE_KB / (1024 * 1024) ))
  printf '  Unidad de modelos de Ollama (%s): %s GB libres; se requieren ~%s GB\n' "$DISK_PATH" "$FREE_GB" "$NEED_GB"
  (( FREE_GB >= NEED_GB )) || fail "Espacio insuficiente en $DISK_PATH ($FREE_GB GB libres; se requieren ~$NEED_GB GB para $BASE_MODEL). Libera espacio y vuelve a ejecutar."
  log_ok "Espacio suficiente en $DISK_PATH"
else
  log_warn "No se pudo verificar el espacio en $DISK_PATH. Continuando; ollama pull validara por su cuenta."
fi

# --- 10. Descarga del modelo si falta -------------------------------------------
log_step "Modelo base de Ollama"
if curl -s --max-time 10 "$OLLAMA_BASE/api/tags" | grep -q "\"name\"[[:space:]]*:[[:space:]]*\"$BASE_MODEL\""; then
  log_ok "$BASE_MODEL ya esta descargado"
else
  printf '  Descargando %s (puede tardar; el pull se puede reanudar re-ejecutando el script)...\n' "$BASE_MODEL"
  ollama pull "$BASE_MODEL" || fail "ollama pull $BASE_MODEL fallo. Revisa tu conexion y vuelve a ejecutar el script (continua donde quedo)."
  log_ok "$BASE_MODEL descargado"
fi

# --- 11-12. Modelfile y modelo derivado "fercho" ---------------------------------
log_step "Modelo derivado '$FERCHO_MODEL' (contexto $NUM_CTX tokens)"
NEEDS_CREATE=1
CURRENT_MODELFILE=$(ollama show "$FERCHO_MODEL" --modelfile 2>/dev/null || true)
if printf '%s' "$CURRENT_MODELFILE" | grep -q "FROM $BASE_MODEL" && \
   printf '%s' "$CURRENT_MODELFILE" | grep -Eq "num_ctx[[:space:]]+$NUM_CTX"; then
  NEEDS_CREATE=0
  log_ok "'$FERCHO_MODEL' ya existe con la configuracion esperada"
fi
if (( NEEDS_CREATE )); then
  MODELFILE="$(mktemp "${TMPDIR:-/tmp}/Modelfile.fercho.XXXXXX")"
  {
    printf 'FROM %s\n' "$BASE_MODEL"
    printf 'PARAMETER num_ctx %s\n' "$NUM_CTX"
  } > "$MODELFILE"
  printf '  Contenido del Modelfile:\n'
  sed 's/^/    /' "$MODELFILE"
  ollama create "$FERCHO_MODEL" -f "$MODELFILE" || { rm -f "$MODELFILE"; fail "ollama create $FERCHO_MODEL fallo."; }
  rm -f "$MODELFILE"
  log_ok "'$FERCHO_MODEL' creado desde $BASE_MODEL con num_ctx $NUM_CTX"
fi

# --- 13. Merge seguro del .env ----------------------------------------------------
log_step "Configuracion local (.env)"
ENV_FILE="$ROOT/.env"
ENV_EXAMPLE="$ROOT/.env.example"
if [[ ! -f "$ENV_FILE" ]]; then
  [[ -f "$ENV_EXAMPLE" ]] || fail "No existe .env ni .env.example en el repositorio."
  cp "$ENV_EXAMPLE" "$ENV_FILE"
  printf '  Se creo .env a partir de .env.example\n'
fi

set_env_value() {
  local key="$1" value="$2" file="$3" tmp
  if grep -Eq "^${key}=" "$file"; then
    tmp="$(mktemp "${TMPDIR:-/tmp}/env.XXXXXX")"
    sed "s|^${key}=.*|${key}=${value}|" "$file" > "$tmp" && mv "$tmp" "$file"
  else
    printf '%s=%s\n' "$key" "$value" >> "$file"
  fi
}

PENDING=0
declare -a PENDING_KEYS=()
check_env_key() {
  local key="$1"
  local current
  current=$(grep -E "^${key}=" "$ENV_FILE" | tail -1 | cut -d= -f2- | tr -d '"' | tr -d "'" | xargs 2>/dev/null || true)
  if [[ -z "$current" ]]; then
    PENDING=$((PENDING+1)); PENDING_KEYS+=("$key")
  fi
}
check_env_key "SUPERFERCHO_OPENAI_CHAT_URL"
check_env_key "SUPERFERCHO_OPENAI_CHAT_MODEL"
check_env_key "OPENAI_API_KEY"
check_env_key "SUPERFERCHO_OPENAI_CHAT_READ_TIMEOUT"

if (( PENDING > 0 )); then
  BACKUP="$ROOT/.env.bak-$(date +%Y%m%d-%H%M%S)"
  cp "$ENV_FILE" "$BACKUP"
  printf '  Respaldo de .env creado: %s\n' "$(basename "$BACKUP")"
  for KEY in "${PENDING_KEYS[@]}"; do
    case "$KEY" in
      SUPERFERCHO_OPENAI_CHAT_URL)          set_env_value "$KEY" "$OLLAMA_BASE/v1/chat/completions" "$ENV_FILE" ;;
      SUPERFERCHO_OPENAI_CHAT_MODEL)        set_env_value "$KEY" "$FERCHO_MODEL" "$ENV_FILE" ;;
      OPENAI_API_KEY)                       set_env_value "$KEY" "ollama" "$ENV_FILE" ;;
      SUPERFERCHO_OPENAI_CHAT_READ_TIMEOUT) set_env_value "$KEY" "300s" "$ENV_FILE" ;;
    esac
    log_ok "$KEY configurado (estaba vacio o ausente)"
  done
else
  log_ok ".env ya contiene la configuracion de Fercho AI (valores existentes no se tocan)"
fi
printf '  NOTA: OPENAI_API_KEY=ollama es un valor dummy requerido por el adapter actual;\n'
printf '        los endpoints locales lo ignoran. NO es una credencial de OpenAI ni genera cobros.\n'

# --- 14. Probe de tool calling -------------------------------------------------------
log_step "Probe de tool calling (modelo '$FERCHO_MODEL')"
printf '  Puede tardar varios minutos en CPU la primera vez (carga del modelo)...\n'
PROBE_RESPONSE=$(curl -s --max-time 300 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ollama" \
  -d '{
    "model": "'"$FERCHO_MODEL"'",
    "messages": [{"role": "user", "content": "What is the weather in Bogota right now? Use the provided tool."}],
    "tools": [{
      "type": "function",
      "function": {
        "name": "get_weather_probe",
        "description": "Get the current weather for a city",
        "parameters": {
          "type": "object",
          "properties": {"city": {"type": "string", "description": "City name"}},
          "required": ["city"]
        }
      }
    }]
  }' "$OLLAMA_BASE/v1/chat/completions") || {
  printf '  Respuesta del servidor: %s\n' "$PROBE_RESPONSE"
  fail "El probe de tool calling no pudo completarse contra $OLLAMA_BASE/v1/chat/completions. Verifica que Ollama siga activo y vuelve a ejecutar el script."
}

if printf '%s' "$PROBE_RESPONSE" | grep -q '"tool_calls"'; then
  TOOL_NAME=$(printf '%s' "$PROBE_RESPONSE" | grep -o '"name"[[:space:]]*:[[:space:]]*"[^"]*"' | head -1 | sed 's/.*: *"//; s/"$//')
  log_ok "El modelo respondio con un tool call ($TOOL_NAME)"
else
  printf '  Respuesta recibida (primeras lineas):\n'
  printf '%s\n' "$PROBE_RESPONSE" | head -c 1200 | sed 's/^/    /'
  printf '\n'
  log_warn "El modelo '$FERCHO_MODEL' NO devolvio tool_calls en el probe."
  printf '\n  Este modelo NO pasa la prueba de tool calling, requisito de Fercho.\n' >&2
  printf '  Opciones:\n' >&2
  printf '    1. Ejecuta de nuevo con otro modelo: ./scripts/setup-fercho.sh --model granite4.1:8b\n' >&2
  printf '    2. O con: ./scripts/setup-fercho.sh --model gpt-oss:20b (requiere ~16 GB de RAM)\n' >&2
  fail "El modelo no pasó la prueba de tool calling."
fi

# --- 15. Resumen ------------------------------------------------------------------------
log_step "Resumen del setup"
for MSG in ${SUMMARY_OK[@]+"${SUMMARY_OK[@]}"};   do printf '  [OK] %s\n' "$MSG"; done
for MSG in ${SUMMARY_WARN[@]+"${SUMMARY_WARN[@]}"}; do printf '  [AVISO] %s\n' "$MSG"; done
printf '\nFercho AI quedo configurado en modo LOCAL (modelo '"'"'%s'"'"' = %s, num_ctx %s).\n' "$FERCHO_MODEL" "$BASE_MODEL" "$NUM_CTX"
printf '\nSiguientes pasos:\n'
printf '  1. Levanta la aplicacion:  ./scripts/dev.sh (o el equivalente de tu sistema)\n'
printf '  2. Registrate como cliente y habla con Fercho en la ruta /assistant\n'
printf '  3. (Opcional) Benchmark: scripts/fercho-bench.sh cuando este disponible\n'
printf '\nRecuerda: OpenCode es solo una herramienta de desarrollo; nunca es parte del runtime de SuperFercho.\n'
