package ifsp.edu.projeto.cortaai.barbershopservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Geocodifica endereços via Nominatim (OpenStreetMap).
 * Tentativas em cascata:
 *   1. Endereço limpo (sem complemento, sem UF, sem CEP)
 *   2. Só rua + número + cidade (remove bairro não mapeado no OSM)
 *   3. Apenas o CEP
 */
@Service
public class GeocodingService {

    private static final Logger log = LoggerFactory.getLogger(GeocodingService.class);
    private static final String NOMINATIM = "https://nominatim.openstreetmap.org/search";

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final ObjectMapper mapper = new ObjectMapper();

    public record Coords(double lat, double lng) {}

    /**
     * Tenta geocodificar o endereço. Retorna null silenciosamente em caso de falha
     * para não bloquear o fluxo de cadastro/atualização da barbearia.
     */
    public Coords geocode(String address) {
        if (address == null || address.isBlank()) return null;
        try {
            String cleaned = clean(address);

            Coords r1 = query(cleaned);
            if (r1 != null) return r1;

            // Tentativa 2: rua + número + cidade (sem bairro)
            String[] parts = cleaned.split(",");
            if (parts.length >= 2) {
                String city = parts[parts.length - 1].trim();
                String streetNum = parts[0].trim() + (parts.length > 1 ? ", " + parts[1].trim() : "");
                Coords r2 = query(streetNum + ", " + city);
                if (r2 != null) return r2;
            }

            // Tentativa 3: só o CEP
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("\\b(\\d{5})-?(\\d{3})\\b")
                    .matcher(address);
            if (m.find()) {
                Coords r3 = query(m.group(1) + "-" + m.group(2) + ", Brasil");
                if (r3 != null) return r3;
            }

            log.warn("Geocoding: nenhuma tentativa resolveu o endereço '{}'", address);
        } catch (Exception e) {
            log.warn("Geocoding falhou para '{}': {}", address, e.getMessage());
        }
        return null;
    }

    // ---------- helpers ----------

    private String clean(String address) {
        return address
                .replaceAll("(?i)CEP:\\s*[\\d-]+", "")
                .replaceAll("\\([^)]*\\)", "")
                .replaceAll("(?i)\\b(casa|apto?\\s*\\d*|apart\\w*|bloco\\s*\\w+|andar\\s*\\d+)\\b", "")
                .replaceAll("\\s*-\\s*[A-Z]{2}\\b", "")
                .replaceAll(",\\s*,", ",")
                .replaceAll(",\\s*$", "")
                .trim();
    }

    private Coords query(String q) throws Exception {
        String url = NOMINATIM + "?q=" + URLEncoder.encode(q, StandardCharsets.UTF_8)
                + "&format=json&limit=1&countrycodes=br";
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept-Language", "pt-BR")
                .header("User-Agent", "CortaAi/1.0")
                .timeout(Duration.ofSeconds(8))
                .GET()
                .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        JsonNode root = mapper.readTree(resp.body());
        if (root.isArray() && root.size() > 0) {
            JsonNode first = root.get(0);
            return new Coords(first.get("lat").asDouble(), first.get("lon").asDouble());
        }
        return null;
    }
}
