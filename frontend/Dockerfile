# ======== STAGE 1: Build ========
FROM node:20-alpine AS build
WORKDIR /app

COPY package.json package-lock.json* ./
# Limpa cache corrompido e instala com ci (reproduzível via lock file)
RUN npm cache clean --force && npm ci --prefer-offline || npm ci

COPY . .

# Vite injeta variáveis de ambiente no build — recebe via build arg
ARG VITE_API_BASE_URL
ARG VITE_ENABLE_PWA
ARG VITE_ENABLE_PUSH
ARG VITE_FIREBASE_VAPID_KEY
ARG VITE_FIREBASE_API_KEY
ARG VITE_FIREBASE_AUTH_DOMAIN
ARG VITE_FIREBASE_PROJECT_ID
ARG VITE_FIREBASE_STORAGE_BUCKET
ARG VITE_FIREBASE_MESSAGING_SENDER_ID
ARG VITE_FIREBASE_APP_ID
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
ENV VITE_ENABLE_PWA=$VITE_ENABLE_PWA
ENV VITE_ENABLE_PUSH=$VITE_ENABLE_PUSH
ENV VITE_FIREBASE_VAPID_KEY=$VITE_FIREBASE_VAPID_KEY
ENV VITE_FIREBASE_API_KEY=$VITE_FIREBASE_API_KEY
ENV VITE_FIREBASE_AUTH_DOMAIN=$VITE_FIREBASE_AUTH_DOMAIN
ENV VITE_FIREBASE_PROJECT_ID=$VITE_FIREBASE_PROJECT_ID
ENV VITE_FIREBASE_STORAGE_BUCKET=$VITE_FIREBASE_STORAGE_BUCKET
ENV VITE_FIREBASE_MESSAGING_SENDER_ID=$VITE_FIREBASE_MESSAGING_SENDER_ID
ENV VITE_FIREBASE_APP_ID=$VITE_FIREBASE_APP_ID

RUN npm run build

# ======== STAGE 2: Serve com Nginx ========
FROM nginx:alpine
# Silencia os scripts de entrypoint do nginx (checksums, reconfig automático)
# Evita: "10-listen-on-ipv6-by-default.sh: info: Getting the checksum..."
ENV NGINX_ENTRYPOINT_QUIET_LOGS=1
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
