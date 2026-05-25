#!/usr/bin/env python3
"""
Teste de integracao real das IAs configuradas no CortaAi (Gustave).
Valida a cadeia de fallback: Gemini -> Groq -> OpenRouter -> Cohere
Usa apenas stdlib (urllib) -- sem dependencias externas.

Uso:
  export GEMINI_API_KEY="..."
  export GROQ_API_KEY="..."
  export OPENROUTER_API_KEY="..."
  export COHERE_API_KEY="..."
  python test_ai_providers.py
"""

import urllib.request
import urllib.error
import json
import os
import sys
import time

GEMINI_API_KEY     = os.environ.get("GEMINI_API_KEY", "")
GEMINI_URL         = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent"
GROQ_API_KEY       = os.environ.get("GROQ_API_KEY", "")
GROQ_URL           = "https://api.groq.com/openai/v1/chat/completions"
GROQ_MODEL         = "llama-3.3-70b-versatile"
OPENROUTER_API_KEY = os.environ.get("OPENROUTER_API_KEY", "")
OPENROUTER_URL     = "https://openrouter.ai/api/v1/chat/completions"
OPENROUTER_MODEL   = "openai/gpt-oss-20b:free"
COHERE_API_KEY     = os.environ.get("COHERE_API_KEY", "")
COHERE_URL         = "https://api.cohere.com/v2/chat"
COHERE_MODEL       = "command-a-03-2025"
PROMPT_TEST = "Responda em uma frase curta: qual e a capital do Brasil?"
TIMEOUT     = 20
OK   = "\033[92m[OK]"
FAIL = "\033[91m[FALHOU]"
RST  = "\033[0m"
results = {}

def http_post(url, payload, headers):
    data = json.dumps(payload).encode("utf-8")
    req  = urllib.request.Request(url, data=data, headers=headers, method="POST")
    with urllib.request.urlopen(req, timeout=TIMEOUT) as resp:
        return json.loads(resp.read().decode("utf-8"))

def test_gemini():
    print("\n--- Gemini 2.0 Flash (primario) ---")
    url     = f"{GEMINI_URL}?key={GEMINI_API_KEY}"
    payload = {"contents": [{"parts": [{"text": PROMPT_TEST}]}]}
    headers = {"Content-Type": "application/json"}
    try:
        t0   = time.time()
        data = http_post(url, payload, headers)
        lat  = round(time.time() - t0, 2)
        text = data["candidates"][0]["content"]["parts"][0]["text"].strip()
        print(f"{OK} Gemini OK ({lat}s){RST}")
        print(f"   Resposta: {text[:120]}")
        results["gemini"] = {"status": "OK", "latency_s": lat, "response": text}
    except Exception as e:
        print(f"{FAIL} Gemini FALHOU{RST}: {e}")
        results["gemini"] = {"status": "FAIL", "error": str(e)}

def test_groq():
    print("\n--- Groq / Llama 3.3-70b (backup 1) ---")
    payload = {"model": GROQ_MODEL, "messages": [{"role": "user", "content": PROMPT_TEST}], "max_tokens": 128}
    headers = {"Authorization": f"Bearer {GROQ_API_KEY}", "Content-Type": "application/json"}
    try:
        t0   = time.time()
        data = http_post(GROQ_URL, payload, headers)
        lat  = round(time.time() - t0, 2)
        text = data["choices"][0]["message"]["content"].strip()
        print(f"{OK} Groq OK ({lat}s){RST}")
        print(f"   Resposta: {text[:120]}")
        results["groq"] = {"status": "OK", "latency_s": lat, "response": text}
    except Exception as e:
        print(f"{FAIL} Groq FALHOU{RST}: {e}")
        results["groq"] = {"status": "FAIL", "error": str(e)}

def test_openrouter():
    print("\n--- OpenRouter / Llama 3.1-8b-instruct:free (backup 2) ---")
    payload = {"model": OPENROUTER_MODEL, "messages": [{"role": "user", "content": PROMPT_TEST}], "max_tokens": 128}
    headers = {"Authorization": f"Bearer {OPENROUTER_API_KEY}", "Content-Type": "application/json", "HTTP-Referer": "https://cortaai.shop", "X-Title": "CortaAi - gustave"}
    try:
        t0   = time.time()
        data = http_post(OPENROUTER_URL, payload, headers)
        lat  = round(time.time() - t0, 2)
        text = data["choices"][0]["message"]["content"].strip()
        print(f"{OK} OpenRouter OK ({lat}s){RST}")
        print(f"   Resposta: {text[:120]}")
        results["openrouter"] = {"status": "OK", "latency_s": lat, "response": text}
    except Exception as e:
        print(f"{FAIL} OpenRouter FALHOU{RST}: {e}")
        results["openrouter"] = {"status": "FAIL", "error": str(e)}

def test_cohere():
    print("\n--- Cohere command-a-03-2025 (backup 3 -- ultimo fallback) ---")
    payload = {"model": COHERE_MODEL, "messages": [{"role": "user", "content": PROMPT_TEST}]}
    headers = {"Authorization": f"Bearer {COHERE_API_KEY}", "Content-Type": "application/json"}
    try:
        t0   = time.time()
        data = http_post(COHERE_URL, payload, headers)
        lat  = round(time.time() - t0, 2)
        text = data["message"]["content"][0]["text"].strip()
        print(f"{OK} Cohere OK ({lat}s){RST}")
        print(f"   Resposta: {text[:120]}")
        results["cohere"] = {"status": "OK", "latency_s": lat, "response": text}
    except Exception as e:
        print(f"{FAIL} Cohere FALHOU{RST}: {e}")
        results["cohere"] = {"status": "FAIL", "error": str(e)}

def print_summary():
    print("\n" + "=" * 72)
    print("  RESUMO -- IAs de Backup (Gustave / CortaAi)")
    print("=" * 72)
    labels = {"gemini": "Gemini 2.0 Flash    (primario)", "groq": "Groq Llama 3.3-70b  (backup 1)", "openrouter": "OpenRouter Llama 8b  (backup 2)", "cohere": "Cohere command-a     (backup 3)"}
    ok_count = 0
    first_ok = None
    for key in ["gemini", "groq", "openrouter", "cohere"]:
        r    = results.get(key, {"status": "NAO TESTADO"})
        icon = OK if r["status"] == "OK" else FAIL
        lat  = f"  {r['latency_s']}s" if "latency_s" in r else ""
        print(f"  {icon} {labels[key]}{lat}{RST}")
        if r["status"] == "OK":
            ok_count += 1
            if not first_ok:
                first_ok = key
    print("=" * 72)
    print(f"\n  {ok_count}/4 provedores operacionais")
    if first_ok:
        print(f"  Provedor ativo na cadeia: {first_ok.upper()}")
    else:
        print(f"  {FAIL} TODOS OS PROVEDORES FALHARAM{RST}")
    return ok_count

if __name__ == "__main__":
    print("=" * 72)
    print("  CortaAi -- Teste de Integracao dos Provedores de IA (Gustave)")
    print("=" * 72)
    print(f"Prompt de teste: \"{PROMPT_TEST}\"")
    test_gemini()
    test_groq()
    test_openrouter()
    test_cohere()
    ok = print_summary()
    sys.exit(0 if ok > 0 else 1)
