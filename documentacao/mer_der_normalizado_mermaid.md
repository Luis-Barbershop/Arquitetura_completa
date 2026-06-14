# MER e DER Normalizados - CortaAi

Este arquivo traz a mesma modelagem normalizada gerada em Draw.io, agora em Mermaid.

- `PK`: chave primaria
- `FK`: chave estrangeira fisica dentro do mesmo banco/microsservico
- `UK`: chave unica
- `REF`: referencia logica por UUID entre bancos/microsservicos

## MER Normalizado

```mermaid
erDiagram
    USUARIO {
        uuid id
        string nome
        string email
        string telefone
        string cpf
        string autenticacao
    }

    CLIENTE {
        uuid usuario_id
        string preferencias
    }

    BARBEIRO {
        uuid usuario_id
        boolean dono
        boolean atua_como_barbeiro
        string conta_pagamento
    }

    BARBEARIA {
        uuid id
        uuid dono_id
        string nome
        string cnpj
        string endereco
        string localizacao
    }

    SERVICO {
        uuid id
        uuid barbearia_id
        string nome
        decimal preco
        int duracao_minutos
    }

    FAVORITO_BARBEARIA {
        uuid cliente_id
        uuid barbearia_id
    }

    SOLICITACAO_ENTRADA {
        uuid id
        uuid barbeiro_id
        uuid barbearia_id
        string status
        string tipo
    }

    AVALIACAO_BARBEARIA {
        uuid id
        uuid cliente_id
        uuid barbearia_id
        int nota
        string comentario
    }

    DESTAQUE_BARBEARIA {
        uuid id
        uuid barbearia_id
        string imagem
    }

    ATIVIDADE_ATRIBUIDA {
        uuid barbeiro_id
        uuid servico_id
    }

    JORNADA_BARBEIRO {
        uuid id
        uuid barbeiro_id
        string dia_semana
        time inicio
        time fim
    }

    BLOQUEIO_AGENDA {
        uuid id
        uuid barbeiro_id
        datetime inicio
        datetime fim
        string motivo
    }

    AGENDAMENTO {
        uuid id
        uuid cliente_id
        uuid barbeiro_id
        uuid barbearia_id
        datetime inicio
        datetime fim
        string status
    }

    ATIVIDADE_AGENDAMENTO {
        uuid id
        uuid agendamento_id
        uuid servico_id
        decimal preco_contratado
        int duracao_minutos
    }

    TRANSACAO {
        uuid id
        uuid agendamento_id
        decimal valor
        decimal taxas
        string metodo
        string status
    }

    REGRA_COMISSAO {
        uuid id
        uuid barbearia_id
        uuid barbeiro_id
        uuid servico_id
        decimal percentual
    }

    DESPESA_FIXA {
        uuid id
        uuid barbearia_id
        string categoria
        decimal valor
        int mes
        int ano
    }

    KPI_DIARIO {
        uuid id
        uuid barbearia_id
        date data
        decimal receita_aprovada
        int qtd_transacoes
    }

    CATEGORIA_PRODUTO {
        uuid id
        uuid barbearia_id
        string nome
    }

    PRODUTO {
        uuid id
        uuid barbearia_id
        uuid categoria_id
        string nome
        decimal preco
        int estoque
    }

    MOVIMENTACAO_ESTOQUE {
        uuid id
        uuid produto_id
        string tipo
        int quantidade
        string motivo
    }

    NOTIFICACAO {
        uuid id
        uuid usuario_id
        string tipo
        string canal
        boolean lida
    }

    TOKEN_DISPOSITIVO {
        uuid id
        uuid usuario_id
        string plataforma
        string token
        boolean ativo
    }

    USUARIO ||--o| CLIENTE : "possui perfil"
    USUARIO ||--o| BARBEIRO : "possui perfil"
    BARBEIRO ||--o| BARBEARIA : "pode ser dono"
    BARBEARIA ||--o{ SERVICO : "oferece"
    CLIENTE ||--o{ FAVORITO_BARBEARIA : "favorita"
    BARBEARIA ||--o{ FAVORITO_BARBEARIA : "e favoritada"
    BARBEIRO ||--o{ SOLICITACAO_ENTRADA : "participa"
    BARBEARIA ||--o{ SOLICITACAO_ENTRADA : "recebe"
    CLIENTE ||--o{ AVALIACAO_BARBEARIA : "faz"
    BARBEARIA ||--o{ AVALIACAO_BARBEARIA : "recebe"
    BARBEARIA ||--o{ DESTAQUE_BARBEARIA : "possui"
    BARBEIRO ||--o{ ATIVIDADE_ATRIBUIDA : "executa"
    SERVICO ||--o{ ATIVIDADE_ATRIBUIDA : "pode ser executado"
    BARBEIRO ||--o{ JORNADA_BARBEIRO : "define"
    BARBEIRO ||--o{ BLOQUEIO_AGENDA : "possui"
    CLIENTE ||--o{ AGENDAMENTO : "realiza"
    BARBEIRO ||--o{ AGENDAMENTO : "atende"
    BARBEARIA ||--o{ AGENDAMENTO : "sedia"
    AGENDAMENTO ||--o{ ATIVIDADE_AGENDAMENTO : "inclui"
    SERVICO ||--o{ ATIVIDADE_AGENDAMENTO : "origina"
    AGENDAMENTO ||--o| TRANSACAO : "gera"
    BARBEARIA ||--o{ REGRA_COMISSAO : "configura"
    BARBEIRO ||--o{ REGRA_COMISSAO : "recebe"
    SERVICO ||--o{ REGRA_COMISSAO : "baseia"
    BARBEARIA ||--o{ DESPESA_FIXA : "possui"
    BARBEARIA ||--o{ KPI_DIARIO : "consolida"
    BARBEARIA ||--o{ CATEGORIA_PRODUTO : "define"
    CATEGORIA_PRODUTO ||--o{ PRODUTO : "classifica"
    BARBEARIA ||--o{ PRODUTO : "mantem"
    PRODUTO ||--o{ MOVIMENTACAO_ESTOQUE : "movimenta"
    USUARIO ||--o{ NOTIFICACAO : "recebe"
    USUARIO ||--o{ TOKEN_DISPOSITIVO : "usa"
```

## DER Normalizado

O DER completo tambem esta separado em [`der_normalizado.mmd`](der_normalizado.mmd), para evitar que este Markdown fique dificil de editar.

```mermaid
erDiagram
    USERS {
        uuid id PK
        varchar_70 name
        varchar_128 tell UK
        varchar_256 email UK
        varchar_64 email_hash UK
        varchar_128 document_cpf UK
        varchar_255 password
        varchar_128 firebase_uid UK
        varchar_30 auth_provider
        varchar_20 role
        varchar_255 image_url
        varchar_128 birth_date
        datetime date_created
        datetime last_updated
    }

    CUSTOMERS {
        uuid user_id PK,FK
        longtext onboarding_progress_json
    }

    BARBERS {
        uuid user_id PK,FK
        boolean is_owner
        boolean act_as_barber
        uuid barbershop_id "REF BARBERSHOPS"
        time work_start_time
        time work_end_time
        text mp_access_token
        text mp_refresh_token
        varchar_256 mp_user_id
        varchar_100 mp_public_key
    }

    CUSTOMER_FAVORITE_BARBERSHOPS {
        uuid customer_id PK,FK
        uuid barbershop_id PK "REF BARBERSHOPS"
    }

    BARBER_ASSIGNED_ACTIVITIES {
        uuid barber_id PK,FK
        uuid activity_id PK "REF ACTIVITIES"
    }

    BARBER_WORK_BLOCKS {
        uuid id PK
        uuid barber_id FK
        varchar_10 day_of_week
        time start_time
        time end_time
    }

    BARBERSHOPS {
        uuid id PK
        uuid owner_id "REF BARBERS"
        varchar_255 name
        varchar_128 cnpj UK
        varchar_255 address
        varchar_255 logo_url
        varchar_255 banner_url
        double latitude
        double longitude
        datetime date_created
        datetime last_updated
    }

    ACTIVITIES {
        uuid id PK
        uuid barbershop_id FK
        varchar_255 activity_name
        decimal_10_2 price
        int duration_minutes
        varchar_255 image_url
        datetime date_created
        datetime last_updated
    }

    BARBERSHOP_JOIN_REQUESTS {
        uuid id PK
        uuid barber_id "REF BARBERS"
        uuid barbershop_id FK
        varchar_50 status
        varchar_20 request_type
        datetime date_created
    }

    BARBERSHOP_HIGHLIGHTS {
        uuid id PK
        uuid barbershop_id FK
        varchar_255 image_url
        varchar_255 image_url_public_id
    }

    BARBERSHOP_REVIEWS {
        uuid id PK
        uuid customer_id "REF CUSTOMERS"
        uuid barbershop_id FK
        int rating
        varchar_500 comment
        datetime created_at
    }

    BARBER_COMMISSION_RULES {
        uuid id PK
        uuid barbershop_id "REF BARBERSHOPS"
        uuid barber_id "REF BARBERS"
        uuid activity_id FK
        decimal_5_2 percentage
        datetime created_at
    }

    FIXED_EXPENSES {
        uuid id PK
        uuid barbershop_id "REF BARBERSHOPS"
        varchar_30 category
        varchar_80 custom_name
        decimal_10_2 amount
        int month
        int year
        boolean recurring_monthly
        datetime created_at
    }

    APPOINTMENTS {
        uuid id PK
        uuid customer_id "REF CUSTOMERS"
        uuid barber_id "REF BARBERS"
        uuid barbershop_id "REF BARBERSHOPS"
        datetime start_time
        datetime end_time
        decimal_10_2 total_price
        varchar_50 status
        datetime date_created
        datetime last_updated
    }

    APPOINTMENT_ACTIVITIES {
        uuid id PK
        uuid appointment_id FK
        uuid activity_id "REF ACTIVITIES"
        decimal_10_2 price_at_booking
        int duration_minutes
    }

    BARBER_BLOCKS {
        uuid id PK
        uuid barber_id "REF BARBERS"
        datetime start_time
        datetime end_time
        varchar_255 reason
        datetime date_created
    }

    TRANSACTIONS {
        uuid id PK
        uuid appointment_id "REF APPOINTMENTS"
        decimal_10_2 amount
        decimal_10_2 gross_amount
        decimal_10_2 net_amount
        decimal_10_2 mp_fee_amount
        decimal_10_2 platform_fee_amount
        varchar_30 payment_method
        varchar_50 status
        varchar mp_preference_id UK
        varchar mp_payment_id
        text checkout_url
        datetime created_at
        datetime updated_at
    }

    WEBHOOK_LOGS {
        uuid id PK
        varchar mp_resource_id UK
        varchar event_type
        text raw_payload
        boolean processed
        datetime received_at
    }

    DASHBOARD_KPI_DAILY {
        uuid id PK
        uuid barbershop_id "REF BARBERSHOPS"
        date reference_date
        decimal_12_2 approved_revenue
        int approved_transactions_count
        datetime updated_at
    }

    PRODUCT_CATEGORIES {
        uuid id PK
        uuid barbershop_id "REF BARBERSHOPS"
        varchar_80 name
        datetime created_at
    }

    PRODUCTS {
        uuid id PK
        uuid barbershop_id "REF BARBERSHOPS"
        uuid category_id FK
        varchar name
        text description
        decimal price
        int stock_quantity
        int min_stock_quantity
        varchar image_url
        boolean active
        datetime created_at
        datetime updated_at
    }

    STOCK_MOVEMENTS {
        uuid id PK
        uuid product_id FK
        varchar type
        int quantity
        decimal_10_2 unit_sale_price
        varchar_500 notes
        varchar reason
        datetime created_at
    }

    NOTIFICATIONS {
        uuid id PK
        uuid user_id "REF USERS"
        varchar type
        varchar title
        text message
        varchar channel
        boolean is_read
        datetime created_at
    }

    DEVICE_TOKENS {
        uuid id PK
        uuid user_id "REF USERS"
        varchar platform
        varchar_512 token UK
        boolean active
        datetime created_at
        datetime updated_at
    }

    USERS ||--o| CUSTOMERS : "perfil"
    USERS ||--o| BARBERS : "perfil"
    CUSTOMERS ||--o{ CUSTOMER_FAVORITE_BARBERSHOPS : "favorita"
    BARBERS ||--o{ BARBER_ASSIGNED_ACTIVITIES : "executa"
    BARBERS ||--o{ BARBER_WORK_BLOCKS : "define jornada"
    BARBERS ||..o| BARBERSHOPS : "REF dono"
    CUSTOMER_FAVORITE_BARBERSHOPS }o..|| BARBERSHOPS : "REF"
    BARBER_ASSIGNED_ACTIVITIES }o..|| ACTIVITIES : "REF"
    BARBERSHOPS ||--o{ ACTIVITIES : "oferece"
    BARBERSHOPS ||--o{ BARBERSHOP_JOIN_REQUESTS : "recebe"
    BARBERS ||..o{ BARBERSHOP_JOIN_REQUESTS : "REF participa"
    BARBERSHOPS ||--o{ BARBERSHOP_HIGHLIGHTS : "possui"
    BARBERSHOPS ||--o{ BARBERSHOP_REVIEWS : "recebe"
    CUSTOMERS ||..o{ BARBERSHOP_REVIEWS : "REF avalia"
    ACTIVITIES ||--o{ BARBER_COMMISSION_RULES : "parametriza"
    BARBERSHOPS ||..o{ BARBER_COMMISSION_RULES : "REF configura"
    BARBERS ||..o{ BARBER_COMMISSION_RULES : "REF recebe"
    BARBERSHOPS ||..o{ FIXED_EXPENSES : "REF possui"
    CUSTOMERS ||..o{ APPOINTMENTS : "REF agenda"
    BARBERS ||..o{ APPOINTMENTS : "REF atende"
    BARBERSHOPS ||..o{ APPOINTMENTS : "REF sedia"
    APPOINTMENTS ||--o{ APPOINTMENT_ACTIVITIES : "inclui"
    ACTIVITIES ||..o{ APPOINTMENT_ACTIVITIES : "REF origina"
    BARBERS ||..o{ BARBER_BLOCKS : "REF bloqueia"
    APPOINTMENTS ||..o| TRANSACTIONS : "REF gera"
    BARBERSHOPS ||..o{ DASHBOARD_KPI_DAILY : "REF consolida"
    BARBERSHOPS ||..o{ PRODUCT_CATEGORIES : "REF define"
    PRODUCT_CATEGORIES ||--o{ PRODUCTS : "classifica"
    BARBERSHOPS ||..o{ PRODUCTS : "REF mantem"
    PRODUCTS ||--o{ STOCK_MOVEMENTS : "movimenta"
    USERS ||..o{ NOTIFICATIONS : "REF recebe"
    USERS ||..o{ DEVICE_TOKENS : "REF usa"
```
