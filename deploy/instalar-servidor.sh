#!/usr/bin/env bash
# Prepara um servidor Ubuntu 24.04 novo (AWS Lightsail ou EC2) para rodar o BarberAgenda.
# Rode UMA vez, dentro do servidor, na pasta do projeto:
#   sudo bash deploy/instalar-servidor.sh
set -euo pipefail

if [ "$(id -u)" -ne 0 ]; then
  echo "Rode com sudo: sudo bash deploy/instalar-servidor.sh" >&2
  exit 1
fi

echo ">> Atualizando o sistema e instalando o Docker..."
apt-get update -y
DEBIAN_FRONTEND=noninteractive apt-get install -y docker.io docker-compose-v2 docker-buildx
systemctl enable --now docker
usermod -aG docker "${SUDO_USER:-ubuntu}" || true

# Memoria extra em disco (swap), para o servidor pequeno nao travar ao compilar
if ! swapon --show | grep -q /swapfile; then
  echo ">> Criando 2 GB de swap..."
  fallocate -l 2G /swapfile
  chmod 600 /swapfile
  mkswap /swapfile
  swapon /swapfile
  echo '/swapfile none swap sw 0 0' >> /etc/fstab
fi

timedatectl set-timezone America/Sao_Paulo || true

echo
echo "Pronto! Agora:"
echo "  1) cp .env.example .env && nano .env      (preencha as senhas e a chave da IA)"
echo "  2) sudo docker compose up -d --build"
