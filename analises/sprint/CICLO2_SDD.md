# CortaAi — SDD Ciclo 2 · Sprints 5–7
> Branch: `feature/migracao-microservicos` · 08/05/2026  
> **Auto-contido** — tudo para implementar os 5 épicos está aqui.  
> Ciclo 1 (fixes + features core): `CICLO1_SDD.md`

---

## Épicos

| Sprint | ID | Título | Serviços | Esforço |
|---|---|---|---|---|
| 5 | E-13 | Export PDF de relatórios | frontend | M |
| 5–6 | E-18 | Chat IA "gustave" (Gemini + Groq fallback) | schedule-service, frontend | XL |
| 6 | E-19 | Geolocalização — filtro por distância na home do cliente | barbershop-service, frontend | L |
| 6 | E-20 | Mini mapa OSM + link Google Maps na página de detalhes | barbershop-service, frontend | L |
| 7 | E-24 | Upload banner somente na página de detalhes da barbearia | barbershop-service, frontend | S |

> M 4–8h · L 8–16h · XL > 16h

---

## ADRs

| ADR | Decisão resumida |
|---|---|
| ADR-12 | Mini mapa: `react-leaflet` + OpenStreetMap (sem API key). Geocoding: Nominatim (gratuito). Link externo: `https://maps.google.com/?q=LAT,LNG`. |
| ADR-13 | Chat IA: Gemini 1.5 Flash → Groq LLaMA 3.3 → mensagem de fallback. Endpoint no `schedule-service`. Assistente se chama "gustave". Dois modos: Previsão (agenda futura) e Consolidado (já atendidos). |
| ADR-14 | Geolocalização: se usuário negar, manter último filtro ativo sem pedir novamente. Posição em `sessionStorage` (não `localStorage`). |
| ADR-15 | PDF: `@react-pdf/renderer` client-side. Modal com período. Sem endpoint de backend. |

---

## Migrations pendentes (confirmar antes de executar)

```sql
-- E-19/E-20: coordenadas geográficas na barbearia
ALTER TABLE barbershops ADD COLUMN latitude DOUBLE NULL;
ALTER TABLE barbershops ADD COLUMN longitude DOUBLE NULL;
```

---

## Sprint 5 — PDF + Chat IA backend

### E-13 · Export PDF de relatórios

**Instalar:** `npm install @react-pdf/renderer`

**Novo componente:** `src/components/Dashboard/ReportPDF.jsx`

```jsx
import { Document, Page, Text, View, Image, StyleSheet } from '@react-pdf/renderer';
import logoImg from '../../assets/logo.png';

const styles = StyleSheet.create({
  page:    { padding: 40, fontFamily: 'Helvetica', fontSize: 10, color: '#222' },
  header:  { flexDirection: 'row', alignItems: 'center', marginBottom: 24, borderBottom: '2pt solid #1a1a2e', paddingBottom: 12 },
  logo:    { width: 60, height: 60, marginRight: 16 },
  title:   { fontSize: 18, fontWeight: 'bold', color: '#1a1a2e' },
  section: { marginTop: 20 },
  sectionTitle: { fontSize: 12, fontWeight: 'bold', marginBottom: 8 },
  row:     { flexDirection: 'row', borderBottom: '0.5pt solid #eee', paddingVertical: 6 },
  cell:    { flex: 1, paddingHorizontal: 4 },
  footer:  { position: 'absolute', bottom: 30, left: 40, right: 40, textAlign: 'center', color: '#999', fontSize: 8 },
});

export function ReportPDF({ barbershopName, period, sections }) {
  return (
    <Document>
      <Page size="A4" style={styles.page}>
        <View style={styles.header}>
          <Image src={logoImg} style={styles.logo} />
          <View>
            <Text style={styles.title}>CortaAi — Relatório</Text>
            <Text style={{ fontSize: 10, color: '#666' }}>{barbershopName}</Text>
            <Text style={{ fontSize: 10, color: '#666' }}>Período: {period.start} a {period.end}</Text>
          </View>
        </View>
        {sections.map((s, i) => (
          <View key={i} style={styles.section}>
            <Text style={styles.sectionTitle}>{s.title}</Text>
            {s.rows.map((row, j) => (
              <View key={j} style={styles.row}>
                {row.map((cell, k) => <Text key={k} style={styles.cell}>{cell}</Text>)}
              </View>
            ))}
          </View>
        ))}
        <Text style={styles.footer}
          render={({ pageNumber, totalPages }) =>
            `Gerado em ${new Date().toLocaleString('pt-BR')} · Página ${pageNumber} de ${totalPages} · CortaAi`
          } fixed />
      </Page>
    </Document>
  );
}
```

**Modal de período:** `src/components/Dashboard/ExportPDFModal.jsx`

```jsx
import { PDFDownloadLink } from '@react-pdf/renderer';

export function ExportPDFModal({ barbershopName, analyticsData, onClose }) {
  const [start, setStart] = useState('');
  const [end,   setEnd]   = useState('');
  const sections = buildPDFSections(filterAnalyticsByPeriod(analyticsData, start, end));

  return (
    <div className={styles.overlay}>
      <div className={styles.modal}>
        <h3>Exportar Relatório</h3>
        <label>De: <input type="date" value={start} onChange={e => setStart(e.target.value)} /></label>
        <label>Até: <input type="date" value={end}   onChange={e => setEnd(e.target.value)}   /></label>
        {start && end && (
          <PDFDownloadLink
            document={<ReportPDF barbershopName={barbershopName} period={{ start, end }} sections={sections} />}
            fileName={`cortaai-relatorio-${start}-${end}.pdf`}
            className={styles.downloadBtn}
          >
            {({ loading }) => loading ? 'Gerando PDF...' : '⬇ Baixar PDF'}
          </PDFDownloadLink>
        )}
        <button onClick={onClose}>Fechar</button>
      </div>
    </div>
  );
}
```

Integrar em `BarberDashboardPage.jsx`:
```jsx
<button onClick={() => setShowExportModal(true)}>📄 Exportar PDF</button>
{showExportModal && <ExportPDFModal ... onClose={() => setShowExportModal(false)} />}
```

**Aceite:** PDF gerado client-side com logo, nome da barbearia, período e dados filtrados.

---

### E-18 · Chat IA "gustave"

Ver **ADR-13**.

#### Backend — `schedule-service`

**`POST /api/schedule/ai/chat`**

```java
@RestController
@RequestMapping("/api/schedule/ai")
public class AiChatController {
    @PostMapping("/chat")
    public ResponseEntity<AiChatResponseDTO> chat(
        @RequestHeader("X-User-Id") String userUid,
        @RequestHeader("X-User-Role") String role,
        @Valid @RequestBody AiChatRequestDTO request) {
        return ResponseEntity.ok(aiChatService.chat(userUid, role, request));
    }
}
```

**DTOs:**
```java
public record AiChatRequestDTO(
    @NotBlank String message,
    @NotNull AiChatMode mode  // PREVIEW | CONSOLIDATED
) {}

public enum AiChatMode { PREVIEW, CONSOLIDATED }

public record AiChatResponseDTO(String message, String source, AiChatMode mode) {}
// source: "gemini" | "groq" | "fallback"
```

**`AiChatServiceImpl.java`:**
```java
@Service @Slf4j
public class AiChatServiceImpl implements AiChatService {

    public AiChatResponseDTO chat(String userUid, String role, AiChatRequestDTO req) {
        String context = buildContext(userUid, role, req.mode());
        String prompt  = buildPrompt(context, req.message());
        try {
            return new AiChatResponseDTO(callGemini(prompt), "gemini", req.mode());
        } catch (Exception e) {
            log.warn("Gemini indisponível: {}", e.getMessage());
            try {
                return new AiChatResponseDTO(callGroq(prompt), "groq", req.mode());
            } catch (Exception e2) {
                log.error("Groq indisponível: {}", e2.getMessage());
                return new AiChatResponseDTO(
                    "Desculpe, o gustave está temporariamente indisponível. Tente novamente em alguns instantes.",
                    "fallback", req.mode()
                );
            }
        }
    }

    private String buildContext(String userUid, String role, AiChatMode mode) {
        boolean isOwner = isOwner(userUid); // lookup barbershop owner
        if (mode == AiChatMode.PREVIEW) {
            // Agendamentos futuros — owner: toda equipe; colaborador: próprios
            List<Appointment> upcoming = isOwner
                ? appointmentRepository.findUpcomingByBarbershop(getBarbershopId(userUid))
                : appointmentRepository.findUpcomingByBarberUid(userUid);
            return formatPreviewContext(upcoming);
        } else { // CONSOLIDATED
            // Agendamentos COMPLETED/CONCLUDED dos últimos 30 dias
            List<Appointment> done = isOwner
                ? appointmentRepository.findCompletedByBarbershop(getBarbershopId(userUid), last30days())
                : appointmentRepository.findCompletedByBarberUid(userUid, last30days());
            return formatConsolidatedContext(done);
        }
    }

    private String buildPrompt(String context, String message) {
        return """
            Você é o gustave, assistente de inteligência artificial do CortaAi.
            Seja direto, útil e use linguagem informal mas profissional.
            Responda sempre em português brasileiro.
            Nunca informe dados pessoais de clientes além do primeiro nome.

            Dados disponíveis:
            %s

            Pergunta: %s
            """.formatted(context, message);
    }
}
```

**`application.yml` — schedule-service:**
```yaml
ai:
  gemini:
    api-key: ${GEMINI_API_KEY}
    model: gemini-1.5-flash
    url: https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent
  groq:
    api-key: ${GROQ_API_KEY}
    model: llama-3.3-70b-versatile
    url: https://api.groq.com/openai/v1/chat/completions
```

**Variáveis de ambiente:** adicionar ao `docker-compose.yml` e `.env`:
```
GEMINI_API_KEY=...  # Google AI Studio — tier gratuito
GROQ_API_KEY=...    # groq.com — tier gratuito
```

#### Frontend

**Novo service:** `src/services/gustaveService.js`
```js
import api from './api';
export const sendMessage = (message, mode) =>
  api.post('/schedule/ai/chat', { message, mode });
```

**Novo componente flutuante:** `src/components/GustaveChat/GustaveChat.jsx`

```
Layout:
• Botão fixo bottom-right → expande para janela de chat
• Header: avatar + "gustave" + indicador online/offline
• Toggle: [Previsão] [Consolidado]
• Histórico de mensagens (sessão apenas, não persiste)
• Input + botão enviar
• Indicador "digitando..." enquanto aguarda
```

```jsx
// Visível apenas para barbeiros/owners logados (não para clientes)
const userRole = localStorage.getItem('userRole');
if (userRole !== 'ROLE_BARBER') return null;
```

**Aceite:** Toggle Previsão/Consolidado. Respostas Gemini com fallback Groq. Colaborador vê apenas própria agenda/receita. Owner vê equipe + barbearia. Assistente sempre se identifica como "gustave".

---

## Sprint 6 — Geolocalização + Mini Mapa

### E-19 · Geolocalização — filtro por distância

Ver **ADR-14**. Migration: campos `latitude/longitude` em `barbershops` (acima).

#### Backend — `barbershop-service`

**`GET /api/barbershops?lat=&lng=&radiusKm=10`**

```java
@GetMapping
public ResponseEntity<List<BarbershopSummaryDTO>> list(
    @RequestParam(required = false) Double lat,
    @RequestParam(required = false) Double lng,
    @RequestParam(defaultValue = "10") Double radiusKm) {

    if (lat != null && lng != null)
        return ResponseEntity.ok(barbershopService.findByProximity(lat, lng, radiusKm));
    return ResponseEntity.ok(barbershopService.findAll());
}
```

Query nativa com Haversine (MySQL):
```sql
SELECT *, (6371 * acos(
    cos(radians(:lat)) * cos(radians(latitude)) *
    cos(radians(longitude) - radians(:lng)) +
    sin(radians(:lat)) * sin(radians(latitude))
)) AS distance_km
FROM barbershops
WHERE latitude IS NOT NULL
HAVING distance_km <= :radiusKm
ORDER BY distance_km
```

**`BarbershopSummaryDTO`** — adicionar campo:
```java
public record BarbershopSummaryDTO(
    UUID id, String name, String address,
    Double lat, Double lng,
    Double distanceKm,  // null quando filtro não aplicado
    String logoUrl, Double rating
) {}
```

#### Frontend

```jsx
// Posição em sessionStorage (não persiste entre sessões — ADR-14)
const [userLocation, setUserLocation] = useState(
  JSON.parse(sessionStorage.getItem('userLocation') || 'null')
);

const requestLocation = () => {
  navigator.geolocation.getCurrentPosition(
    (pos) => {
      const loc = { lat: pos.coords.latitude, lng: pos.coords.longitude };
      sessionStorage.setItem('userLocation', JSON.stringify(loc));
      setUserLocation(loc);
    },
    () => toast.info('Localização não disponível. Exibindo todas as barbearias.')
    // Se negar: manter estado anterior, não pedir novamente
  );
};
```

**Service — `barbershopService.js`:**
```js
export const getBarbershops = ({ lat, lng, radiusKm = 10 } = {}) =>
  api.get('/barbershops', { params: lat ? { lat, lng, radiusKm } : {} });
```

Botão "📍 Usar minha localização" → chama `requestLocation()`.  
Distância exibida no card: `"2.4 km"`.

---

### E-20 · Mini mapa OSM + link Google Maps

Ver **ADR-12**. Migration acima.

**Instalar:**
```bash
npm install react-leaflet leaflet
# Copiar assets para /public/leaflet/:
# marker-icon.png, marker-icon-2x.png, marker-shadow.png
# (de node_modules/leaflet/dist/images/)
```

#### Backend — `barbershop-service`

**`Barbershop.java`** — adicionar campos:
```java
@Column(nullable = true) private Double latitude;
@Column(nullable = true) private Double longitude;
```

**`BarbershopUpdateRequestDTO`** — adicionar:
```java
public record BarbershopUpdateRequestDTO(
    String name, String address,
    Double latitude, Double longitude,  // ← novos
    String phone, String description
) {}
```

Geocoding é feito no **frontend** ao salvar endereço (Nominatim). Backend apenas persiste os valores recebidos.

#### Frontend

**Geocoding ao salvar endereço** — em `barbershopService.js`:
```js
export const geocodeAddress = async (address) => {
  const res = await fetch(
    `https://nominatim.openstreetmap.org/search?q=${encodeURIComponent(address)}&format=json&limit=1`,
    { headers: { 'Accept-Language': 'pt-BR' } }
  );
  const data = await res.json();
  if (!data.length) return null;
  return { lat: parseFloat(data[0].lat), lng: parseFloat(data[0].lon) };
};
```

Ao salvar barbearia:
```js
const coords = await geocodeAddress(formData.address);
await updateBarbershop({ ...formData, latitude: coords?.lat ?? null, longitude: coords?.lng ?? null });
```

**Novo componente:** `src/components/BarbershopMapSection/BarbershopMapSection.jsx`

```jsx
import { MapContainer, TileLayer, Marker, Popup } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';
import L from 'leaflet';
import styles from './BarbershopMapSection.module.css';

// Fix ícone padrão do Leaflet com bundlers:
delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: '/leaflet/marker-icon-2x.png',
  iconUrl:       '/leaflet/marker-icon.png',
  shadowUrl:     '/leaflet/marker-shadow.png',
});

export function BarbershopMapSection({ lat, lng, name, address }) {
  if (!lat || !lng) return null;

  return (
    <section className={styles.wrap}>
      <MapContainer center={[lat, lng]} zoom={15} className={styles.map}
        scrollWheelZoom={false} dragging={false} zoomControl={false}>
        <TileLayer
          attribution='© <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />
        <Marker position={[lat, lng]}>
          <Popup>{name}<br />{address}</Popup>
        </Marker>
      </MapContainer>
      <a href={`https://maps.google.com/?q=${lat},${lng}`}
         target="_blank" rel="noopener noreferrer"
         className={styles.mapsLink}>
        📍 Abrir no Google Maps
      </a>
    </section>
  );
}
```

```css
/* BarbershopMapSection.module.css */
.map      { height: 200px; width: 100%; border-radius: 12px; margin-bottom: 8px; }
.mapsLink { display: inline-flex; align-items: center; gap: 6px;
            color: var(--color-primary); font-size: 14px; text-decoration: none; }
```

Integrar na página de detalhes `/barbearia/:id`:
```jsx
<BarbershopMapSection lat={shop.lat} lng={shop.lng} name={shop.name} address={shop.address} />
```

**Aceite:** Mapa exibido quando lat/lng disponíveis. Scroll desabilitado (não interfere com página). Marker com popup. Link Google Maps em nova aba. Sem lat/lng → mapa não exibido (graceful degradation).

---

## Sprint 7 — Banner + polish

### E-24 · Upload banner somente na página de detalhes

**Contexto:** Upload de banner removido de `BarberProfilePage.jsx`. Banner editável apenas na página pública de detalhes da barbearia.

#### Frontend — `BarberProfilePage.jsx`

Remover:
- Seção de upload de banner
- State e handlers relacionados ao banner
- Manter apenas upload de logo

#### Frontend — página de detalhes da barbearia

Botão de edição do banner visível apenas para o owner autenticado:

```jsx
const isOwner = localStorage.getItem('isOwner') === 'true';
const currentUserId = localStorage.getItem('userId');
const isBarbershopOwner = isOwner && shop.ownerFirebaseUid === currentUserId;

{isBarbershopOwner && (
  <button className={styles.editBannerBtn} onClick={() => setEditingBanner(true)}>
    ✏️ Editar banner
  </button>
)}
```

Ao clicar: abre `CropImageModal` (E-14, Ciclo 1) com `aspect={16/9}`:

```jsx
{editingBanner && (
  <CropImageModal
    imageSrc={selectedBannerSrc}
    aspect={16 / 9}
    onConfirm={handleBannerUpload}
    onCancel={() => setEditingBanner(false)}
  />
)}
```

**Aceite:** Banner não editável em `BarberProfilePage`. Editável na página de detalhes (owner only). Crop 16:9.
