#!/bin/bash

# SCRIPT DE DEPLOY PARA O FRONTEND REACT NATIVE (EXPO WEB)

set -e # Para o script se qualquer comando falhar

echo ">>> Iniciando deploy do Frontend..."

# 1. Navega para o diretório do projeto
cd /home/ec2-user/Arquitetura_completa/meuApp

# 2. Garante que está na branch correta e puxa as últimas atualizações
echo ">>> Atualizando código da branch 'frontEndDev'..."
git checkout frontEndDev
git pull origin frontEndDev

# 3. Instala/atualiza as dependências do projeto
echo ">>> Instalando dependências (npm install)..."
npm install

# 4. Gera a build de produção (arquivos estáticos)
# O comando 'export' cria uma pasta 'dist' com a versão final do site
echo ">>> Gerando build de produção com 'npx expo export'..."
npx expo export -p web

# 5. Prepara o diretório do servidor web e copia os arquivos da build
echo ">>> Copiando arquivos para o diretório do Nginx..."
# Cria o diretório se não existir
sudo mkdir -p /var/www/html/meu-app
# Limpa o diretório antigo
sudo rm -rf /var/www/html/meu-app/*
# Copia os novos arquivos da build
sudo cp -R dist/* /var/www/html/meu-app/

# 6. Reinicia o Nginx para aplicar as mudanças (se necessário)
# O Nginx não precisa ser reiniciado para arquivos estáticos, mas é uma boa prática
sudo systemctl reload nginx

echo ""
echo ">>> Deploy do Frontend finalizado com sucesso!"
