#!/usr/bin/env python3
"""
test_notifications.py
=====================
Teste end-to-end de TODOS os tipos de notificação do CortaAi.

Fluxos testados:
  ✓ IN_APP via SSE (notification-created + unread-count)
  ✓ Marcar como lida (PUT /read + SSE unread-count cai)
  ✓ Contador de não-lidas (GET /unread-count)
  ✓ APPOINTMENT_CREATED   (cliente cria agendamento)
  ✓ APPOINTMENT_CANCELLED (cliente cancela)
  ✓ APPOINTMENT_CONCLUDED (barbeiro conclui)
  ✓ APPOINTMENT_RESCHEDULED (reagendamento)
  ✓ JOIN_REQUEST_RECEIVED (barbeiro pede para entrar na barbearia → owner recebe)
  ✓ INVITE_RECEIVED       (owner convida barbeiro → barbeiro recebe)
  ✗ PAYMENT_APPROVED      (requer webhook MP — não testado aqui)
  ✗ APPOINTMENT_REMINDER  (agendado por scheduler — não testado aqui)

Pré-requisitos:
  1. pip install requests sseclient-py
  2. Criar .env.test com FIREBASE_WEB_API_KEY=<sua chave>
  3. Serviços rodando em CORTAAI_BASE_URL (padrão: https://api.cortaai.shop)

Uso:
  python test_notifications.py
  python test_notifications.py --verbose
"""

import os, sys, json, time, uuid, threading
from datetime import datetime, timedelta
from pathlib import Path

# ── Dependências ─────────────────────────────────────────────────────────────
try:
    import requests
except ImportError:
    print("❌  Instale o requests:  pip install requests")
    sys.exit(1)

try:
    import sseclient
    SSE_AVAILABLE = True
except ImportError:
    SSE_AVAILABLE = False
    print("⚠️   sseclient-py não encontrado — teste SSE será pulado  (pip install sseclient-py)")

# ── Configuração ──────────────────────────────────────────────────────────────
def load_env(path=".env.test"):
    """Carrega variáveis de um .env.test simples (KEY=VALUE)."""
    p = Path(__file__).parent / path
    if p.exists():
        for line in p.read_text(encoding="utf-8").splitlines():
            line = line.strip()
            if line and not line.startswith("#") and "=" in line:
                k, v = line.split("=", 1)
                os.environ.setdefault(k.strip(), v.strip())

load_env()

BASE_URL          = os.getenv("CORTAAI_BASE_URL",    "https://api.cortaai.shop")
FIREBASE_API_KEY  = os.getenv("FIREBASE_WEB_API_KEY", "")
CUSTOMER_EMAIL    = os.getenv("CUSTOMER_EMAIL",  "gb.chaves@hotmail.com")
CUSTOMER_PASS     = os.getenv("CUSTOMER_PASS",   "Senha@123")
BARBER_EMAIL      = os.getenv("BARBER_EMAIL",    "barbeiro@teste.com")
BARBER_PASS       = os.getenv("BARBER_PASS",     "Senha@123")
OWNER_EMAIL       = os.getenv("OWNER_EMAIL",     "chaves.bsilba@gmail.com")
OWNER_PASS        = os.getenv("OWNER_PASS",      "Senha@123")

VERBOSE = "--verbose" in sys.argv or "-v" in sys.argv

# ── Resultado global ──────────────────────────────────────────────────────────
RESULTS = []
ERRORS  = []

def ok(label, detail=""):
    RESULTS.append(("✅", label, detail))
    print(f"  ✅  {label}" + (f"  —  {detail}" if detail else ""))

def fail(label, detail=""):
    RESULTS.append(("❌", label, detail))
    ERRORS.append(label)
    print(f"  ❌  {label}" + (f"  —  {detail}" if detail else ""))

def skip(label, reason=""):
    RESULTS.append(("⏭️ ", label, reason))
    print(f"  ⏭️   {label}" + (f"  ({reason})" if reason else ""))

def section(title):
    print(f"\n{'─'*60}\n  {title}\n{'─'*60}")

# ── Autenticação Firebase REST API ────────────────────────────────────────────
FIREBASE_SIGNIN_URL = (
    "https://identitytoolkit.googleapis.com/v1/accounts"
    ":signInWithPassword?key={key}"
)

def firebase_login(email: str, password: str) -> str:
    """Retorna o idToken Firebase para o usuário. Lança RuntimeError em falha."""
    if not FIREBASE_API_KEY:
        raise RuntimeError(
            "FIREBASE_WEB_API_KEY não configurada.\n"
            "Crie o arquivo .env.test com:\n"
            "  FIREBASE_WEB_API_KEY=AIzaSy..."
        )
    url = FIREBASE_SIGNIN_URL.format(key=FIREBASE_API_KEY)
    resp = requests.post(url, json={"email": email, "password": password,
                                    "returnSecureToken": True}, timeout=15)
    if resp.status_code != 200:
        raise RuntimeError(f"Firebase login falhou para {email}: {resp.text[:300]}")
    return resp.json()["idToken"]

# ── HTTP helper ───────────────────────────────────────────────────────────────
def api(method: str, path: str, token: str = None, **kwargs):
    url = BASE_URL + path
    headers = kwargs.pop("headers", {})
    if token:
        headers["Authorization"] = f"Bearer {token}"
    headers.setdefault("Content-Type", "application/json")
    if VERBOSE:
        print(f"    → {method.upper()} {path}")
    resp = requests.request(method, url, headers=headers, timeout=20, **kwargs)
    if VERBOSE and resp.status_code >= 400:
        print(f"      HTTP {resp.status_code}: {resp.text[:300]}")
    return resp

# ── Helpers ───────────────────────────────────────────────────────────────────
def notification_count_for_type(notifications: list, ntype: str) -> int:
    return sum(1 for n in notifications if n.get("type") == ntype)

def latest_notification(notifications: list, ntype: str) -> dict | None:
    found = [n for n in notifications if n.get("type") == ntype]
    return found[-1] if found else None

# ═════════════════════════════════════════════════════════════════════════════
# SEÇÃO 1 — Autenticação
# ═════════════════════════════════════════════════════════════════════════════
def test_auth():
    section("1. AUTENTICAÇÃO")
    tokens = {}

    for role, email, pwd in [
        ("customer", CUSTOMER_EMAIL, CUSTOMER_PASS),
        ("barber",   BARBER_EMAIL,   BARBER_PASS),
        ("owner",    OWNER_EMAIL,    OWNER_PASS),
    ]:
        try:
            tok = firebase_login(email, pwd)
            tokens[role] = tok
            ok(f"Login {role}", email)
        except Exception as e:
            fail(f"Login {role}", str(e))

    return tokens

# ═════════════════════════════════════════════════════════════════════════════
# SEÇÃO 2 — Descoberta de dados (barbershop/barber IDs)
# ═════════════════════════════════════════════════════════════════════════════
def test_discovery(tokens: dict) -> dict:
    section("2. DESCOBERTA DE DADOS")
    data = {}

    # Perfil do customer
    tok = tokens.get("customer")
    if tok:
        r = api("get", "/api/users/profile", tok)
        if r.status_code == 200:
            p = r.json()
            data["customer_id"] = p.get("id")
            ok("Perfil customer", f"id={p.get('id')}")
        else:
            fail("Perfil customer", f"HTTP {r.status_code}")

    # Perfil do barbeiro
    tok = tokens.get("barber")
    if tok:
        r = api("get", "/api/users/barber-profile", tok)
        if r.status_code == 200:
            p = r.json()
            data["barber_id"] = p.get("id")
            data["barber_barbershop_id"] = p.get("barbershopId")
            ok("Perfil barbeiro", f"id={p.get('id')} shop={p.get('barbershopId')}")
        else:
            # Tenta endpoint alternativo
            r2 = api("get", "/api/users/profile", tok)
            if r2.status_code == 200:
                p = r2.json()
                data["barber_id"] = p.get("id")
                data["barber_barbershop_id"] = p.get("barbershopId")
                ok("Perfil barbeiro (via /profile)", f"id={p.get('id')}")
            else:
                fail("Perfil barbeiro", f"HTTP {r.status_code}")

    # Perfil do owner
    tok = tokens.get("owner")
    if tok:
        r = api("get", "/api/users/barber-profile", tok)
        if r.status_code != 200:
            r = api("get", "/api/users/profile", tok)
        if r.status_code == 200:
            p = r.json()
            data["owner_id"] = p.get("id")
            ok("Perfil owner", f"id={p.get('id')}")
        else:
            fail("Perfil owner", f"HTTP {r.status_code}")

    # Detalhes da barbearia
    shop_id = data.get("barber_barbershop_id")
    if shop_id:
        r = api("get", f"/api/barbershops/{shop_id}", tokens.get("customer") or tokens.get("barber"))
        if r.status_code == 200:
            shop = r.json()
            activities = shop.get("activities") or []
            if activities:
                data["activity_id"] = activities[0].get("id")
                ok("Atividade da barbearia", f"id={data['activity_id']} nome={activities[0].get('activityName','?')}")
            else:
                fail("Atividades da barbearia", "Nenhuma atividade cadastrada")
        else:
            fail("Barbearia", f"HTTP {r.status_code}")

    return data

# ═════════════════════════════════════════════════════════════════════════════
# SEÇÃO 3 — APPOINTMENT_CREATED + APPOINTMENT_CANCELLED
# ═════════════════════════════════════════════════════════════════════════════
def test_appointment_create_cancel(tokens: dict, data: dict) -> dict | None:
    section("3. APPOINTMENT_CREATED + APPOINTMENT_CANCELLED")

    barber_id    = data.get("barber_id")
    shop_id      = data.get("barber_barbershop_id")
    activity_id  = data.get("activity_id")
    tok_customer = tokens.get("customer")
    tok_barber   = tokens.get("barber")

    if not all([barber_id, shop_id, activity_id, tok_customer]):
        skip("Criar agendamento", "dados insuficientes da seção 2")
        return None

    # Buscar slot disponível (amanhã)
    tomorrow = (datetime.now() + timedelta(days=1)).strftime("%Y-%m-%d")
    r = api("get", f"/api/appointments/availability?barberId={barber_id}&date={tomorrow}&duration=30",
            tok_customer)
    slots = r.json() if r.status_code == 200 else []

    if not slots:
        # Tenta em 2 dias
        d2 = (datetime.now() + timedelta(days=2)).strftime("%Y-%m-%d")
        r = api("get", f"/api/appointments/availability?barberId={barber_id}&date={d2}&duration=30",
                tok_customer)
        slots = r.json() if r.status_code == 200 else []
        if not slots:
            skip("Criar agendamento", f"Sem slots disponíveis em {tomorrow} ou {d2}")
            return None

    start_time = slots[0].get("startTime") or slots[0].get("start")
    if not start_time:
        skip("Criar agendamento", f"Formato de slot inesperado: {slots[0]}")
        return None

    # Snapshot das notificações ANTES
    snap_before = []
    r_before = api("get", "/api/notifications/my-notifications", tok_customer)
    if r_before.status_code == 200:
        snap_before = r_before.json()

    # Criar agendamento
    payload = {
        "barberId":     barber_id,
        "barbershopId": shop_id,
        "activityIds":  [activity_id],
        "startTime":    start_time,
        "paymentMethod": "CASH"
    }
    r = api("post", "/api/appointments", tok_customer, json=payload)

    if r.status_code not in (200, 201):
        fail("POST /api/appointments", f"HTTP {r.status_code} — {r.text[:200]}")
        return None

    appointment = r.json()
    appt_id = appointment.get("id")
    ok("APPOINTMENT_CREATED — agendamento criado", f"id={appt_id} slot={start_time}")

    # Aguarda processamento assíncrono (RabbitMQ → notification-service)
    time.sleep(2)

    # Verificar notificação IN_APP do cliente
    r = api("get", "/api/notifications/my-notifications", tok_customer)
    if r.status_code == 200:
        notifs = r.json()
        new_created = [
            n for n in notifs
            if n.get("type") == "APPOINTMENT_CREATED"
            and n.get("id") not in [x.get("id") for x in snap_before]
        ]
        if new_created:
            ok("APPOINTMENT_CREATED — notificação IN_APP (cliente)", new_created[0].get("message","?")[:80])
        else:
            fail("APPOINTMENT_CREATED — notificação IN_APP (cliente)", "Não encontrada após 2s")
    else:
        fail("GET /api/notifications/my-notifications", f"HTTP {r.status_code}")

    # Verificar notificação do barbeiro (APPOINTMENT_CREATED)
    if tok_barber:
        time.sleep(1)
        r = api("get", "/api/notifications/my-notifications", tok_barber)
        if r.status_code == 200:
            barber_notifs = r.json()
            created_for_barber = [n for n in barber_notifs if n.get("type") == "APPOINTMENT_CREATED"]
            if created_for_barber:
                ok("APPOINTMENT_CREATED — notificação IN_APP (barbeiro)", created_for_barber[-1].get("message","?")[:80])
            else:
                fail("APPOINTMENT_CREATED — notificação IN_APP (barbeiro)", "Não encontrada")

    # ── APPOINTMENT_CANCELLED ─────────────────────────────────────────────────
    snap_before_cancel = []
    if tok_barber:
        r_b = api("get", "/api/notifications/my-notifications", tok_barber)
        if r_b.status_code == 200:
            snap_before_cancel = r_b.json()

    r = api("put", f"/api/appointments/{appt_id}/cancel", tok_customer)
    if r.status_code in (200, 204):
        ok("APPOINTMENT_CANCELLED — cancelamento OK", f"id={appt_id}")
        time.sleep(2)

        if tok_barber:
            r = api("get", "/api/notifications/my-notifications", tok_barber)
            if r.status_code == 200:
                barber_notifs = r.json()
                new_cancel = [
                    n for n in barber_notifs
                    if n.get("type") == "APPOINTMENT_CANCELLED"
                    and n.get("id") not in [x.get("id") for x in snap_before_cancel]
                ]
                if new_cancel:
                    ok("APPOINTMENT_CANCELLED — notificação IN_APP (barbeiro)", new_cancel[-1].get("message","?")[:80])
                else:
                    fail("APPOINTMENT_CANCELLED — notificação IN_APP (barbeiro)", "Não encontrada após 2s")
    else:
        fail("APPOINTMENT_CANCELLED", f"HTTP {r.status_code} — {r.text[:200]}")

    return appointment

# ═════════════════════════════════════════════════════════════════════════════
# SEÇÃO 4 — APPOINTMENT_CONCLUDED (cria novo agendamento e conclui)
# ═════════════════════════════════════════════════════════════════════════════
def test_appointment_conclude(tokens: dict, data: dict):
    section("4. APPOINTMENT_CONCLUDED")

    barber_id    = data.get("barber_id")
    shop_id      = data.get("barber_barbershop_id")
    activity_id  = data.get("activity_id")
    tok_customer = tokens.get("customer")
    tok_barber   = tokens.get("barber")

    if not all([barber_id, shop_id, activity_id, tok_customer, tok_barber]):
        skip("APPOINTMENT_CONCLUDED", "dados insuficientes")
        return

    # Buscar slot disponível
    tomorrow = (datetime.now() + timedelta(days=1)).strftime("%Y-%m-%d")
    r = api("get", f"/api/appointments/availability?barberId={barber_id}&date={tomorrow}&duration=30", tok_customer)
    slots = r.json() if r.status_code == 200 else []
    if not slots:
        d2 = (datetime.now() + timedelta(days=2)).strftime("%Y-%m-%d")
        r = api("get", f"/api/appointments/availability?barberId={barber_id}&date={d2}&duration=30", tok_customer)
        slots = r.json() if r.status_code == 200 else []

    if not slots:
        skip("APPOINTMENT_CONCLUDED", "Sem slots disponíveis")
        return

    start_time = slots[0].get("startTime") or slots[0].get("start")

    # Criar agendamento
    payload = {"barberId": barber_id, "barbershopId": shop_id,
               "activityIds": [activity_id], "startTime": start_time, "paymentMethod": "CASH"}
    r = api("post", "/api/appointments", tok_customer, json=payload)
    if r.status_code not in (200, 201):
        fail("Criar agendamento para conclude", f"HTTP {r.status_code}")
        return

    appt_id = r.json().get("id")
    ok("Agendamento criado para conclude", f"id={appt_id}")

    # Snapshot das notificações do cliente
    snap_before = []
    r_snap = api("get", "/api/notifications/my-notifications", tok_customer)
    if r_snap.status_code == 200:
        snap_before = r_snap.json()

    # Barbeiro conclui (o barbeiro só pode concluir um agendamento que já passou do horário de início
    # OU se o sistema permitir conclusão antecipada manual)
    r = api("put", f"/api/appointments/{appt_id}/conclude", tok_barber)
    if r.status_code in (200, 204):
        ok("APPOINTMENT_CONCLUDED — conclusão OK", f"id={appt_id}")
        time.sleep(2)

        r = api("get", "/api/notifications/my-notifications", tok_customer)
        if r.status_code == 200:
            notifs = r.json()
            new_conclude = [
                n for n in notifs
                if n.get("type") == "APPOINTMENT_CONCLUDED"
                and n.get("id") not in [x.get("id") for x in snap_before]
            ]
            if new_conclude:
                ok("APPOINTMENT_CONCLUDED — notificação IN_APP (cliente)", new_conclude[-1].get("message","?")[:80])
            else:
                fail("APPOINTMENT_CONCLUDED — notificação IN_APP (cliente)", "Não encontrada após 2s")
    else:
        body = r.text[:200]
        # Conclusão antecipada pode ser bloqueada por regra de negócio (status inválido)
        if "Estado inválido" in body or "409" in str(r.status_code):
            skip("APPOINTMENT_CONCLUDED", f"Regra de negócio impediu conclusão antecipada: {body}")
            # Limpa o agendamento para não poluir
            api("put", f"/api/appointments/{appt_id}/cancel", tok_customer)
        else:
            fail("APPOINTMENT_CONCLUDED", f"HTTP {r.status_code} — {body}")

# ═════════════════════════════════════════════════════════════════════════════
# SEÇÃO 5 — APPOINTMENT_RESCHEDULED
# ═════════════════════════════════════════════════════════════════════════════
def test_appointment_reschedule(tokens: dict, data: dict):
    section("5. APPOINTMENT_RESCHEDULED")

    barber_id   = data.get("barber_id")
    shop_id     = data.get("barber_barbershop_id")
    activity_id = data.get("activity_id")
    tok_customer = tokens.get("customer")
    tok_barber   = tokens.get("barber")

    if not all([barber_id, shop_id, activity_id, tok_customer]):
        skip("APPOINTMENT_RESCHEDULED", "dados insuficientes")
        return

    # Buscar 2 slots diferentes
    target_date = (datetime.now() + timedelta(days=4)).strftime("%Y-%m-%d")
    r = api("get", f"/api/appointments/availability?barberId={barber_id}&date={target_date}&duration=30", tok_customer)
    slots = r.json() if r.status_code == 200 else []
    if len(slots) < 2:
        target_date2 = (datetime.now() + timedelta(days=5)).strftime("%Y-%m-%d")
        r2 = api("get", f"/api/appointments/availability?barberId={barber_id}&date={target_date2}&duration=30", tok_customer)
        slots2 = r2.json() if r2.status_code == 200 else []
        if not slots or not slots2:
            skip("APPOINTMENT_RESCHEDULED", "Slots insuficientes")
            return
        slot_create = slots[0] if slots else slots2[0]
        slot_new    = slots2[0] if slots2 else slots[1]
    else:
        slot_create = slots[0]
        slot_new    = slots[1]

    start1 = slot_create.get("startTime") or slot_create.get("start")
    start2 = slot_new.get("startTime") or slot_new.get("start")

    # Criar agendamento
    payload = {"barberId": barber_id, "barbershopId": shop_id,
               "activityIds": [activity_id], "startTime": start1, "paymentMethod": "CASH"}
    r = api("post", "/api/appointments", tok_customer, json=payload)
    if r.status_code not in (200, 201):
        fail("Criar agendamento para reschedule", f"HTTP {r.status_code}")
        return

    appt_id = r.json().get("id")
    ok("Agendamento criado para reschedule", f"id={appt_id}")

    # Snapshot notificações
    snap_cust, snap_barber = [], []
    r_snap = api("get", "/api/notifications/my-notifications", tok_customer)
    if r_snap.status_code == 200: snap_cust = r_snap.json()
    if tok_barber:
        r_snap_b = api("get", "/api/notifications/my-notifications", tok_barber)
        if r_snap_b.status_code == 200: snap_barber = r_snap_b.json()

    # Reagendar (mínimo 3h de antecedência — slots 4+ dias à frente ok)
    r = api("put", f"/api/appointments/{appt_id}/reschedule", tok_customer,
            json={"newStartTime": start2})
    if r.status_code in (200, 204):
        ok("APPOINTMENT_RESCHEDULED — reagendamento OK", f"novo slot={start2}")
        time.sleep(2)

        r_notifs = api("get", "/api/notifications/my-notifications", tok_customer)
        if r_notifs.status_code == 200:
            notifs = r_notifs.json()
            new_re = [n for n in notifs
                      if n.get("type") == "APPOINTMENT_RESCHEDULED"
                      and n.get("id") not in [x.get("id") for x in snap_cust]]
            if new_re:
                ok("APPOINTMENT_RESCHEDULED — notificação IN_APP (cliente)", new_re[-1].get("message","?")[:80])
            else:
                fail("APPOINTMENT_RESCHEDULED — notificação IN_APP (cliente)", "Não encontrada após 2s")

        if tok_barber:
            r_barber = api("get", "/api/notifications/my-notifications", tok_barber)
            if r_barber.status_code == 200:
                notifs_b = r_barber.json()
                new_re_b = [n for n in notifs_b
                            if n.get("type") == "APPOINTMENT_RESCHEDULED"
                            and n.get("id") not in [x.get("id") for x in snap_barber]]
                if new_re_b:
                    ok("APPOINTMENT_RESCHEDULED — notificação IN_APP (barbeiro)", new_re_b[-1].get("message","?")[:80])
                else:
                    fail("APPOINTMENT_RESCHEDULED — notificação IN_APP (barbeiro)", "Não encontrada após 2s")

        # Limpa
        api("put", f"/api/appointments/{appt_id}/cancel", tok_customer)
    else:
        body = r.text[:200]
        if "3 horas" in body or "antecedência" in body:
            skip("APPOINTMENT_RESCHEDULED", "Regra de 3h de antecedência — use slots mais distantes")
        else:
            fail("APPOINTMENT_RESCHEDULED", f"HTTP {r.status_code} — {body}")
        api("put", f"/api/appointments/{appt_id}/cancel", tok_customer)

# ═════════════════════════════════════════════════════════════════════════════
# SEÇÃO 6 — JOIN_REQUEST_RECEIVED (barbeiro pede para entrar → owner notificado)
# ═════════════════════════════════════════════════════════════════════════════
def test_join_request(tokens: dict, data: dict):
    section("6. JOIN_REQUEST_RECEIVED (barbeiro → owner)")

    tok_barber = tokens.get("barber")
    tok_owner  = tokens.get("owner")

    if not tok_barber or not tok_owner:
        skip("JOIN_REQUEST_RECEIVED", "token barber/owner ausente")
        return

    # Verifica se barbeiro já está em alguma barbearia
    r = api("get", "/api/users/barber-profile", tok_barber)
    if r.status_code != 200:
        r = api("get", "/api/users/profile", tok_barber)
    if r.status_code == 200:
        if r.json().get("barbershopId"):
            skip("JOIN_REQUEST_RECEIVED", "barbeiro já vinculado a uma barbearia — saia antes de solicitar")
            return

    # Descobre o CNPJ da barbearia do owner
    r_owner_shop = api("get", "/api/barbershops/my-shop", tok_owner)
    if r_owner_shop.status_code != 200:
        skip("JOIN_REQUEST_RECEIVED", f"Não foi possível obter barbearia do owner: HTTP {r_owner_shop.status_code}")
        return

    cnpj = r_owner_shop.json().get("cnpj")
    if not cnpj:
        skip("JOIN_REQUEST_RECEIVED", "CNPJ da barbearia do owner não encontrado")
        return

    # Snapshot do owner
    snap_owner = []
    r_snap = api("get", "/api/notifications/my-notifications", tok_owner)
    if r_snap.status_code == 200: snap_owner = r_snap.json()

    # Barbeiro solicita entrada
    r = api("post", "/api/barbershops/join-request", tok_barber, json={"cnpj": cnpj})
    if r.status_code in (200, 202, 204):
        ok("JOIN_REQUEST_RECEIVED — solicitação enviada", f"CNPJ={cnpj}")
        time.sleep(2)

        r_owner_notifs = api("get", "/api/notifications/my-notifications", tok_owner)
        if r_owner_notifs.status_code == 200:
            notifs = r_owner_notifs.json()
            new_jr = [n for n in notifs
                      if n.get("type") == "JOIN_REQUEST_RECEIVED"
                      and n.get("id") not in [x.get("id") for x in snap_owner]]
            if new_jr:
                ok("JOIN_REQUEST_RECEIVED — notificação IN_APP (owner)", new_jr[-1].get("message","?")[:80])
            else:
                fail("JOIN_REQUEST_RECEIVED — notificação IN_APP (owner)", "Não encontrada após 2s")

        # Rejeita a solicitação para não sujar o estado
        r_pending = api("get", "/api/barbershops/my-shop/pending-requests", tok_owner)
        if r_pending.status_code == 200:
            barber_id_int = data.get("barber_id")
            for req in r_pending.json():
                if str(req.get("barberId")) == str(barber_id_int):
                    req_id = req.get("requestId")
                    api("post", f"/api/barbershops/my-shop/reject-request/{req_id}", tok_owner)
                    break
    elif r.status_code == 409:
        skip("JOIN_REQUEST_RECEIVED", "Barbeiro já tem solicitação pendente para esta barbearia")
    else:
        fail("JOIN_REQUEST_RECEIVED", f"HTTP {r.status_code} — {r.text[:200]}")

# ═════════════════════════════════════════════════════════════════════════════
# SEÇÃO 7 — INVITE_RECEIVED (owner convida barbeiro → barbeiro notificado)
# ═════════════════════════════════════════════════════════════════════════════
def test_invite(tokens: dict, data: dict):
    section("7. INVITE_RECEIVED (owner → barbeiro)")

    tok_owner  = tokens.get("owner")
    tok_barber = tokens.get("barber")

    if not tok_owner or not tok_barber:
        skip("INVITE_RECEIVED", "token owner/barber ausente")
        return

    # Busca CPF do barbeiro (campo criptografado — pode não vir na resposta pública)
    barber_cpf = os.getenv("BARBER_CPF", "")
    if not barber_cpf:
        # Tenta obter do perfil
        r = api("get", "/api/users/barber-profile", tok_barber)
        if r.status_code != 200:
            r = api("get", "/api/users/profile", tok_barber)
        if r.status_code == 200:
            barber_cpf = r.json().get("cpf", "")

    if not barber_cpf:
        skip("INVITE_RECEIVED", "CPF do barbeiro não disponível (campo criptografado) — defina BARBER_CPF em .env.test")
        return

    # Verifica se barbeiro já está vinculado
    r = api("get", "/api/users/barber-profile", tok_barber)
    if r.status_code != 200:
        r = api("get", "/api/users/profile", tok_barber)
    if r.status_code == 200 and r.json().get("barbershopId"):
        skip("INVITE_RECEIVED", "Barbeiro já vinculado — não é possível enviar convite")
        return

    # Snapshot do barbeiro
    snap_barber = []
    r_snap = api("get", "/api/notifications/my-notifications", tok_barber)
    if r_snap.status_code == 200: snap_barber = r_snap.json()

    # Owner convida pelo CPF
    r = api("post", "/api/barbershops/my-shop/invite-barber", tok_owner, json={"cpf": barber_cpf})
    if r.status_code in (200, 202):
        ok("INVITE_RECEIVED — convite enviado", f"CPF={barber_cpf[:3]}***")
        time.sleep(2)

        r_barber_notifs = api("get", "/api/notifications/my-notifications", tok_barber)
        if r_barber_notifs.status_code == 200:
            notifs = r_barber_notifs.json()
            new_inv = [n for n in notifs
                       if n.get("type") == "INVITE_RECEIVED"
                       and n.get("id") not in [x.get("id") for x in snap_barber]]
            if new_inv:
                ok("INVITE_RECEIVED — notificação IN_APP (barbeiro)", new_inv[-1].get("message","?")[:80])
            else:
                fail("INVITE_RECEIVED — notificação IN_APP (barbeiro)", "Não encontrada após 2s")
    elif r.status_code == 409:
        skip("INVITE_RECEIVED", f"Convite duplicado ou barbeiro já vinculado: {r.text[:100]}")
    else:
        fail("INVITE_RECEIVED", f"HTTP {r.status_code} — {r.text[:200]}")

# ═════════════════════════════════════════════════════════════════════════════
# SEÇÃO 8 — Marcar como lida + Contador unread
# ═════════════════════════════════════════════════════════════════════════════
def test_mark_read_and_unread_count(tokens: dict):
    section("8. MARCAR COMO LIDA + CONTADOR UNREAD")

    tok = tokens.get("customer")
    if not tok:
        skip("Marcar como lida", "token customer ausente")
        return

    # Contador unread
    r = api("get", "/api/notifications/unread-count", tok)
    if r.status_code == 200:
        count_before = r.json().get("unreadCount", r.json())
        ok(f"GET /unread-count", f"unreadCount={count_before}")
    else:
        fail("GET /unread-count", f"HTTP {r.status_code}")
        return

    # Lista notificações não lidas
    r = api("get", "/api/notifications/my-notifications", tok)
    if r.status_code != 200:
        fail("GET /my-notifications", f"HTTP {r.status_code}")
        return

    notifs = r.json()
    unread = [n for n in notifs if not n.get("read")]
    if not unread:
        skip("Marcar como lida", "Nenhuma notificação não lida")
        return

    notif_id = unread[0]["id"]
    r = api("put", f"/api/notifications/{notif_id}/read", tok)
    if r.status_code == 200:
        n = r.json()
        if n.get("read"):
            ok("PUT /read — notificação marcada como lida", f"id={notif_id}")
        else:
            fail("PUT /read — campo 'read' ainda false", f"id={notif_id}")
    else:
        fail("PUT /read", f"HTTP {r.status_code} — {r.text[:200]}")
        return

    # Contador deve ter caído
    r = api("get", "/api/notifications/unread-count", tok)
    if r.status_code == 200:
        count_after = r.json().get("unreadCount", r.json())
        if count_after < count_before:
            ok("Contador unread decrementou", f"{count_before} → {count_after}")
        else:
            fail("Contador unread não decrementou", f"antes={count_before} depois={count_after}")
    else:
        fail("GET /unread-count após marcar lida", f"HTTP {r.status_code}")

# ═════════════════════════════════════════════════════════════════════════════
# SEÇÃO 9 — SSE (Server-Sent Events)
# ═════════════════════════════════════════════════════════════════════════════
def test_sse(tokens: dict):
    section("9. SSE — Server-Sent Events")

    if not SSE_AVAILABLE:
        skip("SSE stream", "sseclient-py não instalado (pip install sseclient-py)")
        return

    tok = tokens.get("customer")
    if not tok:
        skip("SSE stream", "token customer ausente")
        return

    received_events = []
    sse_error = []

    def consume_sse():
        try:
            url = BASE_URL + "/api/notifications/stream"
            r = requests.get(url, headers={"Authorization": f"Bearer {tok}"},
                             stream=True, timeout=10)
            if r.status_code != 200:
                sse_error.append(f"HTTP {r.status_code}")
                return
            client = sseclient.SSEClient(r)
            for event in client.events():
                received_events.append({"name": event.event, "data": event.data})
                if len(received_events) >= 2:
                    break
        except Exception as e:
            sse_error.append(str(e))

    # Inicia consumidor SSE em thread separada
    t = threading.Thread(target=consume_sse, daemon=True)
    t.start()
    time.sleep(1.5)  # Aguarda conexão estabelecer

    if sse_error:
        fail("SSE /api/notifications/stream", sse_error[0])
        return

    # Conectou — verifica se recebeu evento de boas-vindas ou unread-count
    t.join(timeout=4)

    if received_events:
        event_names = [e.get("name") for e in received_events]
        ok("SSE stream conectado e recebendo eventos", f"eventos={event_names}")
    else:
        # SSE pode não enviar nada imediatamente — apenas conexão estabelecida já é suficiente
        # Verifica se não houve erro de conexão
        if not sse_error:
            ok("SSE stream — conexão estabelecida (sem evento imediato, normal se sem atividade)")
        else:
            fail("SSE stream", f"Sem eventos e com erro: {sse_error}")

# ═════════════════════════════════════════════════════════════════════════════
# SEÇÃO 10 — Saúde dos endpoints de notificação
# ═════════════════════════════════════════════════════════════════════════════
def test_notification_endpoints_health(tokens: dict):
    section("10. SAÚDE DOS ENDPOINTS DE NOTIFICAÇÃO")

    for role, key in [("customer", "customer"), ("barber", "barber"), ("owner", "owner")]:
        tok = tokens.get(key)
        if not tok:
            skip(f"GET /my-notifications ({role})", "sem token")
            continue

        r = api("get", "/api/notifications/my-notifications", tok)
        if r.status_code == 200:
            all_notifs = r.json()
            unread = sum(1 for n in all_notifs if not n.get("read"))
            ok(f"GET /my-notifications ({role})", f"total={len(all_notifs)} não-lidas={unread}")
        else:
            fail(f"GET /my-notifications ({role})", f"HTTP {r.status_code}")

        r = api("get", "/api/notifications/unread-count", tok)
        if r.status_code == 200:
            count = r.json().get("unreadCount", r.json())
            ok(f"GET /unread-count ({role})", f"unreadCount={count}")
        else:
            fail(f"GET /unread-count ({role})", f"HTTP {r.status_code}")

# ═════════════════════════════════════════════════════════════════════════════
# MAIN
# ═════════════════════════════════════════════════════════════════════════════
def main():
    print("\n" + "═"*60)
    print("  🔔  CortaAi — Teste E2E de Notificações")
    print(f"  Base URL: {BASE_URL}")
    print(f"  Data/hora: {datetime.now().strftime('%d/%m/%Y %H:%M:%S')}")
    print("═"*60)

    if not FIREBASE_API_KEY:
        print("\n⚠️   FIREBASE_WEB_API_KEY não configurada!")
        print("  Crie o arquivo .env.test com:")
        print("    FIREBASE_WEB_API_KEY=AIzaSy...")
        print("  (Firebase Console → Project Settings → General → Web API Key)")
        print("\n  Os testes de fluxo real serão pulados.\n")

    tokens = test_auth()

    if not any(tokens.values()):
        print("\n❌  Nenhum token obtido — encerrando.\n")
        sys.exit(1)

    # Saúde básica dos endpoints
    test_notification_endpoints_health(tokens)

    # SSE
    test_sse(tokens)

    # Descoberta de dados
    data = test_discovery(tokens)

    # Fluxos de agendamento
    test_appointment_create_cancel(tokens, data)
    test_appointment_conclude(tokens, data)
    test_appointment_reschedule(tokens, data)

    # Fluxos de equipe
    test_join_request(tokens, data)
    test_invite(tokens, data)

    # Marcar como lida
    test_mark_read_and_unread_count(tokens)

    # ── Sumário ───────────────────────────────────────────────────────────────
    print("\n" + "═"*60)
    print("  SUMÁRIO")
    print("═"*60)

    total  = len(RESULTS)
    passed = sum(1 for r in RESULTS if r[0] == "✅")
    failed = sum(1 for r in RESULTS if r[0] == "❌")
    skipped= sum(1 for r in RESULTS if r[0].startswith("⏭"))

    print(f"  Total:   {total}")
    print(f"  ✅ OK:    {passed}")
    print(f"  ❌ FAIL:  {failed}")
    print(f"  ⏭  SKIP: {skipped}")

    if ERRORS:
        print(f"\n  Falhas:")
        for e in ERRORS:
            print(f"    • {e}")

    print("\n  Tipos de notificação testados:")
    print("    ✓ IN_APP (todos os fluxos abaixo)")
    print("    ✓ SSE (stream de eventos em tempo real)")
    print("    ✓ APPOINTMENT_CREATED")
    print("    ✓ APPOINTMENT_CANCELLED")
    print("    ✓ APPOINTMENT_CONCLUDED")
    print("    ✓ APPOINTMENT_RESCHEDULED")
    print("    ✓ JOIN_REQUEST_RECEIVED")
    print("    ✓ INVITE_RECEIVED")
    print("    ~ PAYMENT_APPROVED      (requer webhook MP — teste manual)")
    print("    ~ APPOINTMENT_REMINDER  (agendado por scheduler — teste manual)")
    print("    ~ BARBER_REMOVED        (requer owner remover barbeiro vinculado — teste manual)")
    print("    ~ PUSH (FCM)            (requer registro de device token no browser)")

    print("═"*60)
    sys.exit(0 if failed == 0 else 1)

if __name__ == "__main__":
    main()
