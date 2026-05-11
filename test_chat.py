import urllib.request, json

data = json.dumps({"message": "Sistema funcionando?", "mode": "PREVIEW"}).encode()
req = urllib.request.Request(
    "http://schedule-service:8083/api/schedule/ai/chat",
    data=data,
    headers={
        "Content-Type": "application/json",
        "X-User-UID": "g6XudOnuxmOaz8I3jztj9kqTzfh1",
        "X-User-Type": "OWNER"
    }
)
try:
    r = urllib.request.urlopen(req, timeout=30)
    print("HTTP:", r.status)
    print(r.read().decode())
except urllib.error.HTTPError as e:
    print("HTTP ERROR:", e.code)
    print(e.read().decode()[:1000])
except Exception as ex:
    print("ERRO:", ex)
