# React + Vite

This template provides a minimal setup to get React working in Vite with HMR and some ESLint rules.

Currently, two official plugins are available:

- [@vitejs/plugin-react](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react) uses [Babel](https://babeljs.io/) (or [oxc](https://oxc.rs) when used in [rolldown-vite](https://vite.dev/guide/rolldown)) for Fast Refresh
- [@vitejs/plugin-react-swc](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react-swc) uses [SWC](https://swc.rs/) for Fast Refresh

## React Compiler

The React Compiler is not enabled on this template because of its impact on dev & build performances. To add it, see [this documentation](https://react.dev/learn/react-compiler/installation).

## Expanding the ESLint configuration

If you are developing a production application, we recommend using TypeScript with type-aware lint rules enabled. Check out the [TS template](https://github.com/vitejs/vite/tree/main/packages/create-vite/template-react-ts) for information on how to integrate TypeScript and [`typescript-eslint`](https://typescript-eslint.io) in your project.

## PWA (fase inicial)

- O registro do Service Worker está protegido por flag.
- Para habilitar, defina `VITE_ENABLE_PWA=true` no ambiente.
- Quando a flag não está ativa, o frontend mantém o comportamento padrão sem registrar SW.

### Runtime caching (F4-03)

- App Shell (`/`, `index.html`, manifest e ícones PWA): **cache-first**.
- Assets estáticos (`js`, `css`, imagens e fontes): **cache-first**.
- Endpoints transacionais (`/api/auth`, `/api/appointments`, `/api/payments`): **network-first**.
- Em falha de rede para endpoint transacional, o SW retorna `503` com payload de erro explícito (sem confirmação offline de operação).

### Fallback visual offline (F4-04)

- Telas críticas mostram estado visual de erro (`.ca-state--error`) quando recebem `503 OFFLINE_TRANSACIONAL_UNAVAILABLE`.
- Coberto nesta fase:
	- `AgendamentoPage`
	- `BarberManualBookingPage`
	- `MeusAgendamentosPage`
- Objetivo: comunicar indisponibilidade transacional offline sem criar sensação de sucesso da operação.
