#!/bin/sh
# Dump diário do banco em /backups (pasta ./backups do projeto). Um arquivo por dia; guarda N dias.
set -eu
RETENCAO="${BACKUP_RETENCAO_DIAS:-30}"

while true; do
  ARQUIVO="/backups/bolso-$(date +%F).sql.gz"
  TMP="/backups/.bolso-em-andamento.sql"
  # dump num arquivo antes de compactar: num pipe, uma falha do pg_dump passaria despercebida
  if pg_dump --no-owner --no-privileges -f "$TMP" && gzip -c "$TMP" > "$ARQUIVO.tmp"; then
    mv "$ARQUIVO.tmp" "$ARQUIVO"
    echo "$(date '+%F %T') backup ok: $ARQUIVO"
  else
    rm -f "$ARQUIVO.tmp"
    echo "$(date '+%F %T') backup FALHOU" >&2
  fi
  rm -f "$TMP"
  # -mtime +N apaga com N+1 dias completos; N-1 mantém exatamente RETENCAO arquivos
  find /backups -name 'bolso-*.sql.gz' -mtime +"$((RETENCAO - 1))" -delete
  sleep 86400
done
