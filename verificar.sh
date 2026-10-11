#!/usr/bin/env bash
# Verifica o projeto inteiro com um comando só: o que "Antes de dar uma tarefa como pronta" pede no CLAUDE.md.
#
#   ./verificar.sh              front (testes, lint, typecheck, build) e back (mvnw verify)
#   ./verificar.sh --so-front   só o front (rápido)
#   ./verificar.sh --so-back    só o back (precisa do Docker aberto: Testcontainers)
#   ./verificar.sh --docker     tudo e, se passar, reconstrói e sobe os contêineres (docker compose up -d --build)
#
# Para no primeiro passo que falhar e mostra o resumo. Funciona no bash do MSYS2 (Windows), no Linux e no CI
# do GitHub, que chama este mesmo script.

set -uo pipefail

raiz="$(cd "$(dirname "$0")" && pwd)"
front=1
back=1
docker=0

for arg in "$@"; do
  case "$arg" in
    --so-front) back=0 ;;
    --so-back) front=0 ;;
    --docker) docker=1 ;;
    -h | --help)
      sed -n '2,10p' "$0"
      exit 0
      ;;
    *)
      echo "Opção desconhecida: $arg (use --help)" >&2
      exit 2
      ;;
  esac
done

# Cores só num terminal de verdade (no CI e em arquivo de log, texto puro)
if [ -t 1 ]; then
  verde=$'\033[32m' vermelho=$'\033[31m' negrito=$'\033[1m' normal=$'\033[0m'
else
  verde='' vermelho='' negrito='' normal=''
fi

resumo=()
inicio_total=$SECONDS

# passo "nome" pasta comando...
passo() {
  local nome="$1" pasta="$2"
  shift 2
  echo
  echo "${negrito}▶ ${nome}${normal}"
  local inicio=$SECONDS
  if (cd "$raiz/$pasta" && "$@"); then
    resumo+=("${verde}✓${normal} ${nome} ($((SECONDS - inicio)) s)")
  else
    resumo+=("${vermelho}✗ ${nome} ($((SECONDS - inicio)) s)${normal}")
    terminar 1
  fi
}

terminar() {
  echo
  echo "${negrito}Resumo${normal} ($((SECONDS - inicio_total)) s)"
  printf '  %s\n' "${resumo[@]}"
  if [ "$1" -ne 0 ]; then
    echo "${vermelho}${negrito}Falhou.${normal} A saída do passo com ✗ está acima."
  else
    echo "${verde}${negrito}Tudo verde.${normal}"
  fi
  exit "$1"
}

# No Windows (MSYS2/Git Bash) o Maven Wrapper roda pelo .cmd; no resto, pelo script.
maven() {
  case "$(uname -s)" in
    MINGW* | MSYS* | CYGWIN*) cmd //c mvnw.cmd -B "$@" ;;
    *) ./mvnw -B "$@" ;;
  esac
}

if [ "$front" -eq 1 ]; then
  # npm ci só quando falta node_modules ou o package-lock.json mudou desde a última instalação
  if [ ! -d "$raiz/frontend/node_modules" ] || [ "$raiz/frontend/package-lock.json" -nt "$raiz/frontend/node_modules/.package-lock.json" ]; then
    passo "Front: instalar pacotes (npm ci)" frontend npm ci --no-audit --no-fund
  fi
  passo "Front: testes" frontend npm test
  passo "Front: lint" frontend npm run lint
  passo "Front: typecheck" frontend npm run typecheck
  passo "Front: build" frontend npx vite build --logLevel warn
fi

if [ "$back" -eq 1 ]; then
  passo "Back: mvnw verify (unidade, arquitetura e integração)" backend maven verify
fi

if [ "$docker" -eq 1 ]; then
  passo "Docker: reconstruir e subir (docker compose up -d --build)" . docker compose up -d --build
fi

terminar 0
