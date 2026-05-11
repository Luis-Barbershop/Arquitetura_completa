import urllib.request, json

tests = [
    {"label": "CONSOLIDATED estoque Guilherme", "uid": "g6XudOnuxmOaz8I3jztj9kqTzfh1", "type": "OWNER",  "mode": "CONSOLIDATED", "msg": "como esta meu estoque?"},
    {"label": "CONSOLIDATED financeiro Guilherme", "uid": "g6XudOnuxmOaz8I3jztj9kqTzfh1", "type": "OWNER",  "mode": "CONSOLIDATED", "msg": "qual meu faturamento?"},
    {"label": "PREVIEW agenda Ana",               "uid": "ek7McZFsQxSO01nmwrI8OfIsrsC2", "type": "OWNER",  "mode": "PREVIEW",       "msg": "quais agendamentos tenho hoje?"},
]

for t in tests:
    print(f"\n=== {t['label']} ===")
    data = json.dumps({"message": t["msg"], "mode": t["mode"]}).encode()
    req = urllib.request.Request(
        "http://schedule-service:8083/api/schedule/ai/chat",
        data=data,
        headers={
            "Content-Type": "application/json",
            "X-User-UID":  t["uid"],
            "X-User-Type": t["type"],
        }
    )
    try:
        r = urllib.request.urlopen(req, timeout=60)
        body = json.loads(r.read().decode())
        print("HTTP:", r.status)
        print("SOURCE:", body.get("source"))
        print("MODE:", body.get("mode"))
        print("MESSAGE:", body.get("message", "")[:600])
    except urllib.error.HTTPError as e:
        print("HTTP ERROR:", e.code)
        print(e.read().decode()[:500])
    except Exception as ex:
        print("ERRO:", ex)
