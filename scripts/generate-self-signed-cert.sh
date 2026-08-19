#!/bin/bash
# ============================================================
# Gera certificado SSL auto-assinado para o Nexus Core
# Uso: ./generate-self-signed-cert.sh
# ============================================================
# O certificado gerado é para desenvolvimento/primeiro deploy.
# Em produção, substitua por um certificado real (Let's Encrypt, etc.)
# ============================================================

CERT_DIR="./certs"
CERT_FILE="$CERT_DIR/nexus.crt"
KEY_FILE="$CERT_DIR/nexus.key"

# Criar diretório se não existir
mkdir -p "$CERT_DIR"

# Gerar certificado auto-assinado (válido por 365 dias)
openssl req -x509 -nodes \
  -days 365 \
  -newkey rsa:2048 \
  -keyout "$KEY_FILE" \
  -out "$CERT_FILE" \
  -subj "/C=BR/ST=Estado/L=Cidade/O=Vaultra/OU=Nexus/CN=localhost" \
  2>/dev/null

if [ $? -eq 0 ]; then
  echo "╔══════════════════════════════════════════════════════════╗"
  echo "║  Certificado SSL auto-assinado gerado com sucesso!      ║"
  echo "╠══════════════════════════════════════════════════════════╣"
  echo "║  Certificado: $CERT_FILE"
  echo "║  Chave:       $KEY_FILE"
  echo "║                                                          ║"
  echo "║  ATENÇÃO: Este certificado é para desenvolvimento.       ║"
  echo "║  Em produção, use um certificado real (Let's Encrypt).   ║"
  echo "╚══════════════════════════════════════════════════════════╝"
else
  echo "ERRO: Falha ao gerar certificado. Verifique se o OpenSSL está instalado."
  exit 1
fi
