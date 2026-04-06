# Configuração de DNS (SPF / DKIM / DMARC) e Firebase Auth Domain

> **Domínio de produção:** `cortaai.shop`  
> **Subdomínio do app:** `web.cortaai.shop`  
> **Data:** Abril 2026

---

## 1. Firebase — Authorized Domains

O Firebase Authentication bloqueia redirecionamentos OAuth (Google Sign-In, links de e-mail, etc.) para domínios não autorizados.

### Passo a passo

1. Acesse o [Firebase Console](https://console.firebase.google.com).
2. Selecione o projeto **CortaAi**.
3. Navegue até **Authentication → Settings → Authorized domains**.
4. Clique em **Add domain** e adicione:
   - `web.cortaai.shop`
   - `cortaai.shop` (raiz, caso seja usada)
5. Salve.

> **Nota:** `localhost` já está autorizado por padrão para desenvolvimento local.

### Impacto

Sem essa configuração, o fluxo de login com Google e os links de e-mail (verificação, redefinição de senha) falharão com o erro:
```
auth/unauthorized-continue-uri
```

---

## 2. SPF (Sender Policy Framework)

O SPF autoriza quais servidores de e-mail podem enviar mensagens em nome do domínio `cortaai.shop`.

### Registro DNS a criar

| Campo | Valor |
|-------|-------|
| **Tipo** | `TXT` |
| **Host / Nome** | `@` (raiz do domínio) |
| **Valor** | `v=spf1 include:_spf.google.com ~all` |
| **TTL** | 3600 |

> Substitua `_spf.google.com` pelo include do seu provedor SMTP caso não seja Google Workspace:
> - **SendGrid:** `include:sendgrid.net`
> - **Amazon SES:** `include:amazonses.com`
> - **Firebase/Google:** `include:_spf.google.com`

### Verificação

```bash
nslookup -type=TXT cortaai.shop
# ou
dig TXT cortaai.shop +short
```

---

## 3. DKIM (DomainKeys Identified Mail)

O DKIM assina criptograficamente os e-mails enviados para provar que não foram adulterados.

### Obter a chave DKIM

A chave é gerada pelo seu provedor de e-mail:

#### Google Workspace
1. Acesse [admin.google.com](https://admin.google.com).
2. Vá em **Apps → Google Workspace → Gmail → Autenticação de e-mail**.
3. Selecione o domínio `cortaai.shop` e clique em **Gerar novo registro**.
4. Copie o valor TXT gerado (começa com `v=DKIM1; k=rsa; p=...`).

#### SendGrid
1. Em **Settings → Sender Authentication → Authenticate Your Domain**.
2. Siga o wizard e copie os 2 registros CNAME gerados.

### Registro DNS a criar (exemplo Google)

| Campo | Valor |
|-------|-------|
| **Tipo** | `TXT` |
| **Host / Nome** | `google._domainkey.cortaai.shop` |
| **Valor** | `v=DKIM1; k=rsa; p=MIIBIjANBgkq...` (chave gerada) |
| **TTL** | 3600 |

> O prefixo `google` é o **seletor** — pode variar por provedor (ex.: `s1`, `smtp`, `sendgrid`).

### Verificação

```bash
dig TXT google._domainkey.cortaai.shop +short
```

---

## 4. DMARC (Domain-based Message Authentication, Reporting & Conformance)

O DMARC instrui os servidores receptores sobre o que fazer com e-mails que falham SPF/DKIM, e envia relatórios de abuso.

### Registro DNS a criar

| Campo | Valor |
|-------|-------|
| **Tipo** | `TXT` |
| **Host / Nome** | `_dmarc.cortaai.shop` |
| **Valor** | `v=DMARC1; p=none; rua=mailto:dmarc@cortaai.shop; ruf=mailto:dmarc@cortaai.shop; fo=1` |
| **TTL** | 3600 |

### Política recomendada por fase

| Fase | `p=` | Descrição |
|------|------|-----------|
| **Monitoramento inicial** | `none` | Apenas coleta relatórios, não bloqueia nada |
| **Quarentena** | `quarantine` | E-mails suspeitos vão para spam |
| **Rejeição total** | `reject` | E-mails não autenticados são descartados |

> **Recomendação:** comece com `p=none` por 2–4 semanas, analise os relatórios em `dmarc@cortaai.shop` e então evolua para `quarantine` ou `reject`.

### Verificação

```bash
dig TXT _dmarc.cortaai.shop +short
```

---

## 5. Resumo dos registros DNS

| Tipo | Host | Valor |
|------|------|-------|
| `TXT` | `@` | `v=spf1 include:_spf.google.com ~all` |
| `TXT` | `google._domainkey` | `v=DKIM1; k=rsa; p=<CHAVE_GERADA>` |
| `TXT` | `_dmarc` | `v=DMARC1; p=none; rua=mailto:dmarc@cortaai.shop` |

---

## 6. Ferramentas de validação

| Ferramenta | URL | Uso |
|------------|-----|-----|
| MXToolbox | https://mxtoolbox.com/SuperTool.aspx | SPF, DKIM, DMARC lookup |
| Mail-Tester | https://www.mail-tester.com | Teste de spam score completo |
| Google Admin Toolbox | https://toolbox.googleapps.com/apps/checkmx | Diagnóstico de DNS de e-mail |
| DMARC Analyzer | https://www.dmarcanalyzer.com | Análise de relatórios DMARC |

---

## 7. Checklist final

- [ ] Domínio `web.cortaai.shop` adicionado nos **Authorized Domains** do Firebase
- [ ] Registro SPF (`TXT @`) criado no painel DNS do registrador
- [ ] Chave DKIM gerada pelo provedor de e-mail e registro `TXT` criado
- [ ] Registro DMARC (`TXT _dmarc`) criado com `p=none` inicial
- [ ] Propagação DNS verificada (pode levar até 48h)
- [ ] Teste de envio de e-mail com score ≥ 9/10 no mail-tester.com
- [ ] Após 2 semanas: evoluir DMARC para `p=quarantine` ou `p=reject`
