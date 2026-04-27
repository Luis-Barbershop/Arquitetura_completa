# 🔐 Variáveis de Ambiente — Produção (ZimaOS)

## Como usar

1. Copie o bloco abaixo para o arquivo `.env` na raiz do projeto no servidor
2. Substitua cada `👉 TROCAR:` pelo valor real indicado
3. **NUNCA** commite o `.env` no Git (ele já está no `.gitignore`)

---

## 📋 Arquivo .env completo

```bash
# ==============================================================
# CortaAí — Variáveis de Ambiente (PRODUÇÃO)
# ==============================================================
# Arquivo: /DATA/cortaai/repo/.env
# ==============================================================


# ══════════════════════════════════════════════════════════════
# 🗄️  MYSQL
# ══════════════════════════════════════════════════════════════
# 👉 TROCAR: Crie uma senha FORTE para o root do MySQL.
#    Mínimo 16 caracteres, misture letras, números e símbolos.
#    Exemplo: gerar com: openssl rand -base64 24
#    ⚠️  Se o MySQL já foi criado com outra senha, use a mesma!
#    Mudar aqui NÃO muda a senha no banco, só quebra a conexão.

MYSQL_ROOT_PASSWORD=👉 TROCAR: SuaSenhaForteAqui123!@#
DB_USERNAME=root
DB_PASSWORD=👉 TROCAR: MesmoValorDe_MYSQL_ROOT_PASSWORD


# ══════════════════════════════════════════════════════════════
# 🐰  RABBITMQ
# ══════════════════════════════════════════════════════════════
# 👉 TROCAR: Crie um usuário e senha para o RabbitMQ.
#    NÃO use "guest/guest" em produção (é inseguro).
#    ⚠️  Se o RabbitMQ já foi criado com guest/guest, você
#    precisa deletar o volume e recriar para mudar a senha:
#    docker compose down -v rabbitmq && docker compose up -d rabbitmq

RABBITMQ_USER=👉 TROCAR: cortaai_mq
RABBITMQ_PASS=👉 TROCAR: SenhaForteRabbitMQ456!@#


# ══════════════════════════════════════════════════════════════
# 🔥  FIREBASE AUTHENTICATION
# ══════════════════════════════════════════════════════════════
# 👉 TROCAR: Cole o JSON completo da Service Account do Firebase.
#
#    Como obter:
#    1. Acesse: https://console.firebase.google.com
#    2. Selecione seu projeto
#    3. ⚙️ Project Settings → Service Accounts
#    4. Clique "Generate new private key"
#    5. Abra o JSON baixado
#    6. Cole TODO o conteúdo numa ÚNICA LINHA abaixo
#
#    ⚠️  O JSON deve ficar em UMA LINHA SÓ, sem quebras.
#    Exemplo de como deve ficar:
#    FIREBASE_SERVICE_ACCOUNT_JSON={"type":"service_account","project_id":"cortaai-xxxxx","private_key_id":"abc123","private_key":"-----BEGIN PRIVATE KEY-----\nMIIE...\n-----END PRIVATE KEY-----\n","client_email":"firebase-adminsdk@cortaai-xxxxx.iam.gserviceaccount.com",...}

FIREBASE_SERVICE_ACCOUNT_JSON=👉 TROCAR: {"type":"service_account","project_id":"...cole aqui o JSON completo..."}


# ══════════════════════════════════════════════════════════════
# 💳  MERCADO PAGO (Payment Service)
# ══════════════════════════════════════════════════════════════
# 👉 TROCAR: Access Token do Mercado Pago.
#
#    Como obter:
#    1. Acesse: https://www.mercadopago.com.br/developers
#    2. Suas integrações → Sua aplicação
#    3. Credenciais de Produção → Access Token
#    ⚠️  Use o token de PRODUÇÃO, não o de teste!

MP_ACCESS_TOKEN=👉 TROCAR: APP_USR-xxxxxxxxxxxx-xxxxxx-xxxxxxxxxxxxxxxx-xxxxxxxxx

# 👉 TROCAR: credenciais OAuth de marketplace (fluxo "Vincular Mercado Pago").
#
#    Como obter:
#    1. Suas integrações → Sua aplicação
#    2. Credenciais de Produção → Client ID / Client Secret
#    3. Em OAuth, cadastre exatamente a URL de redirecionamento abaixo
#
#    ⚠️ O MP valida correspondência exata do redirect_uri.
#    ⚠️ Para produção do CortaAi, mantenha domínio https público (sem localhost).

MP_CLIENT_ID=👉 TROCAR: 1234567890123456
MP_CLIENT_SECRET=👉 TROCAR: APP_USR-xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
MP_REDIRECT_URI=👉 TROCAR: https://api.cortaai.shop/api/payments/mp-callback
MP_AUTH_BASE_URL=https://auth.mercadopago.com.br


# ══════════════════════════════════════════════════════════════
# ☁️  CLOUDINARY (Upload de fotos de barbearias e perfis)
# ══════════════════════════════════════════════════════════════
# 👉 TROCAR: Dados da conta Cloudinary.
#
#    Como obter:
#    1. Acesse: https://console.cloudinary.com
#    2. Dashboard → aparece Cloud Name, API Key e API Secret
#    3. Copie os 3 valores abaixo

CLOUDINARY_CLOUD_NAME=👉 TROCAR: seu_cloud_name
CLOUDINARY_API_KEY=👉 TROCAR: 123456789012345
CLOUDINARY_API_SECRET=👉 TROCAR: AbCdEfGhIjKlMnOpQrStUvWx


# ══════════════════════════════════════════════════════════════
# 📧  EMAIL / SMTP (Notification Service)
# ══════════════════════════════════════════════════════════════
# 👉 TROCAR: Configuração de email para envio de notificações.
#
#    Para Gmail:
#    1. Ative a Verificação em 2 Etapas na sua conta Google
#    2. Acesse: https://myaccount.google.com/apppasswords
#    3. Gere uma "Senha de App" para "Outro (nome personalizado)"
#    4. Use essa senha de 16 caracteres no MAIL_PASSWORD
#    ⚠️  NÃO use sua senha real do Gmail, use a Senha de App!
#
#    Para outros provedores, ajuste MAIL_HOST e MAIL_PORT:
#    - Outlook: smtp.office365.com / 587
#    - Yahoo:   smtp.mail.yahoo.com / 587
#    - Custom:  seu.smtp.server.com / 587

MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=👉 TROCAR: seuemail@gmail.com
MAIL_PASSWORD=👉 TROCAR: abcd efgh ijkl mnop
NOTIFICATION_FROM_EMAIL=👉 TROCAR: noreply@cortaai.com.br
```

---

## 🛡️ Checklist de Segurança

| Item | Status | O que fazer |
|------|--------|-------------|
| Senha MySQL ≥ 16 caracteres | ⬜ | `openssl rand -base64 24` |
| RabbitMQ **NÃO** é guest/guest | ⬜ | Trocar user e pass |
| Firebase JSON é de produção | ⬜ | Gerar no Firebase Console |
| Mercado Pago token é de PRODUÇÃO | ⬜ | Não usar token de sandbox |
| Gmail usa Senha de App (não senha real) | ⬜ | Gerar em myaccount.google.com |
| Arquivo `.env` NÃO está no Git | ⬜ | Verificar `.gitignore` |
| Permissões do `.env` no servidor | ⬜ | `chmod 600 .env` |

---

## 🔄 Quais serviços usam cada variável

```
MYSQL_ROOT_PASSWORD ──────► db (MySQL container)
DB_USERNAME ──────────────► user, barbershop, schedule, payment, notification, product
DB_PASSWORD ──────────────► user, barbershop, schedule, payment, notification, product

RABBITMQ_USER ────────────► rabbitmq, barbershop, schedule, payment, notification, product
RABBITMQ_PASS ────────────► rabbitmq, barbershop, schedule, payment, notification, product

FIREBASE_SERVICE_ACCOUNT_JSON ──► gateway, user-service

CLOUDINARY_CLOUD_NAME ────► user-service, barbershop-service
CLOUDINARY_API_KEY ───────► user-service, barbershop-service
CLOUDINARY_API_SECRET ────► user-service, barbershop-service

MP_ACCESS_TOKEN ──────────► payment-service

MAIL_HOST ────────────────► notification-service
MAIL_PORT ────────────────► notification-service
MAIL_USERNAME ────────────► notification-service
MAIL_PASSWORD ────────────► notification-service
NOTIFICATION_FROM_EMAIL ──► notification-service
```

---

## ⚡ Comando rápido para gerar senhas seguras

```bash
# No terminal do servidor (Linux):
echo "MySQL:    $(openssl rand -base64 24)"
echo "RabbitMQ: $(openssl rand -base64 16)"
```
