#!/usr/bin/env bash
# Publica a versao atual do projeto no servidor da AWS (rode no seu Mac, na pasta do projeto):
#   bash deploy/atualizar.sh
# Os dados do banco e o .env do servidor sao mantidos.
set -euo pipefail
cd "$(dirname "$0")/.."

SERVIDOR="${SERVIDOR:-ubuntu@18.118.76.238}"
CHAVE="${CHAVE:-$HOME/.ssh/barberagenda_ed25519}"
SSH=(ssh -i "$CHAVE" -o BatchMode=yes "$SERVIDOR")

bash deploy/empacotar.sh
echo ">> Enviando para $SERVIDOR..."
scp -i "$CHAVE" -o BatchMode=yes -q barberagenda.tar.gz "$SERVIDOR":~/
echo ">> Recompilando e reiniciando o site..."
"${SSH[@]}" 'tar -xzf ~/barberagenda.tar.gz -C ~/barberagenda 2>/dev/null; cd ~/barberagenda && sudo docker compose up -d --build 2>&1 | tail -3 && sudo docker image prune -f >/dev/null'
sleep 15
curl -s -L -o /dev/null -w "Site respondeu HTTP %{http_code}\n" "https://meubarberagenda.duckdns.org/"
