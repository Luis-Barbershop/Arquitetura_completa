// Teste manual do geocoding e sort por localização
// node test_geocoding.mjs

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

async function nominatim(query) {
  const url = `https://nominatim.openstreetmap.org/search?q=${encodeURIComponent(query)}&format=json&limit=1&countrycodes=br`;
  const res = await fetch(url, {
    headers: { 'Accept-Language': 'pt-BR', 'User-Agent': 'CortaAi/1.0' },
  });
  const data = await res.json();
  if (!data.length) return null;
  return { lat: parseFloat(data[0].lat), lng: parseFloat(data[0].lon), display: data[0].display_name };
}

async function geocodeAddress(address) {
  if (!address) return null;

  const cleaned = address
    .replace(/CEP:\s*[\d-]+/gi, '')
    .replace(/\([^)]*\)/g, '')
    .replace(/\b(casa|apto?\s*\d*|apart\w*|bloco\s*\w+|andar\s*\d+)\b/gi, '')
    .replace(/,\s*,/g, ',')
    .replace(/,\s*$/, '')
    .trim();

  await sleep(1100); // Nominatim 1 req/s
  const r1 = await nominatim(cleaned);
  if (r1) return { ...r1, via: 'endereço limpo' };

  const cepMatch = address.match(/\b(\d{5})-?(\d{3})\b/);
  if (cepMatch) {
    await sleep(1100);
    const r2 = await nominatim(`${cepMatch[1]}-${cepMatch[2]}, Brasil`);
    if (r2) return { ...r2, via: 'CEP' };
  }

  return null;
}

function haversineKm(lat1, lon1, lat2, lon2) {
  const R = 6371;
  const dLat = (lat2 - lat1) * Math.PI / 180;
  const dLon = (lon2 - lon1) * Math.PI / 180;
  const a =
    Math.sin(dLat / 2) ** 2 +
    Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
    Math.sin(dLon / 2) ** 2;
  return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

// ─── Dados do banco (copiados da consulta MySQL) ───────────────────────────────
const barbershops = [
  { name: 'barbearia mais ou menos',  address: 'Rua Domingos José Sapienza, 262, casa, Vila Amélia (Zona Norte), São Paulo - SP, CEP: 02618-000' },
  { name: 'barbearia teste lary',     address: 'Rua Figueira da Barbária, 28, apt14, Jardim Brasília (Zona Leste), São Paulo - SP, CEP: 03583-090' },
  { name: 'CORTES FODAS',             address: 'Rua Professor Arnaldo João Semeraro, 465, Jardim Santa Emília, São Paulo - SP, CEP: 04184-000' },
  { name: 'Barbearia One',            address: 'Rua Santo Antônio do Aventureiro, 1, Jardim Popular, São Paulo - SP, CEP: 03671-030' },
  { name: 'Prime SP - Teste Renan',   address: 'Alameda Ministro Rocha Azevedo, 9999, Cerqueira César, São Paulo - SP, CEP: 01410-000' },
];

// ─── Localização do usuário (CEP 04184-000) ────────────────────────────────────
const USER_CEP = '04184-000';

async function main() {
  console.log('=== Geocodificando localização do usuário (CEP 04184-000) ===\n');
  await sleep(1100);
  let userCoords = await nominatim(`${USER_CEP}, Brasil`);
  if (!userCoords) {
    console.log('  CEP puro falhou, tentando bairro...');
    await sleep(1100);
    userCoords = await nominatim('Jardim Santa Emília, São Paulo, SP, Brasil');
  }
  if (userCoords) {
    console.log(`  ✅ Usuário em: ${userCoords.lat.toFixed(6)}, ${userCoords.lng.toFixed(6)}`);
    console.log(`     (${userCoords.display})\n`);
  } else {
    console.log('  ❌ Não foi possível geocodificar o CEP do usuário\n');
  }

  console.log('=== Geocodificando barbearias ===\n');
  const enriched = [];
  for (const shop of barbershops) {
    const coords = await geocodeAddress(shop.address);
    if (coords) {
      console.log(`  ✅ ${shop.name}`);
      console.log(`     lat=${coords.lat.toFixed(6)}, lng=${coords.lng.toFixed(6)} [via ${coords.via}]`);
      console.log(`     → ${coords.display}`);
    } else {
      console.log(`  ❌ ${shop.name} — geocoding falhou`);
    }
    enriched.push({ ...shop, latitude: coords?.lat ?? null, longitude: coords?.lng ?? null });
    console.log();
  }

  if (!userCoords) {
    console.log('\n⚠️  Sem coordenadas do usuário — impossível testar sort por distância.');
    return;
  }

  console.log('=== Sort por distância do CEP 04184-000 ===\n');
  const sorted = [...enriched].sort((a, b) => {
    const dA = a.latitude != null && a.longitude != null
      ? haversineKm(userCoords.lat, userCoords.lng, a.latitude, a.longitude)
      : Infinity;
    const dB = b.latitude != null && b.longitude != null
      ? haversineKm(userCoords.lat, userCoords.lng, b.latitude, b.longitude)
      : Infinity;
    return dA - dB;
  });

  sorted.forEach((s, i) => {
    const dist = s.latitude != null
      ? `${haversineKm(userCoords.lat, userCoords.lng, s.latitude, s.longitude).toFixed(2)} km`
      : '(sem coords — fica no fim)';
    console.log(`  ${i + 1}º ${s.name.padEnd(30)} → ${dist}`);
  });
}

main().catch(console.error);
