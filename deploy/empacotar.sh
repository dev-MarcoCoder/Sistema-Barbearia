#!/usr/bin/env bash
# Gera barberagenda.tar.gz (no seu Mac) para enviar ao servidor.
# Nao inclui senhas nem a chave da API (db.properties, ia.properties, .env).
#   bash deploy/empacotar.sh
set -euo pipefail
cd "$(dirname "$0")/.."
COPYFILE_DISABLE=1 tar -czf barberagenda.tar.gz \
  --exclude='./build' --exclude='./dist' --exclude='./nbproject/private' \
  --exclude='./.git' --exclude='./.env' --exclude='*.war' --exclude='.DS_Store' \
  --exclude='./web/WEB-INF/db.properties' --exclude='./web/WEB-INF/ia.properties' \
  --exclude='./barberagenda.tar.gz' --exclude='./docs' \
  .
echo "Gerado: $(pwd)/barberagenda.tar.gz ($(du -h barberagenda.tar.gz | cut -f1))"
