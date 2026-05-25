import Container_Barbericons from "./Container_Barbericons"
import Styles from "./CSS/Barbershops.module.css"
import { getBarbershops, getShopServices } from "../../../services/barbershopService"
import { getMyRecentBarbershopIds } from "../../../services/appointmentService"
import { useEffect, useState, useMemo, useRef } from "react"
import { toast } from "react-toastify"

const PAGE_SIZE = 9;

const SORT_OPTIONS = [
  { key: 'recent',   label: 'Últimas barbearias' },
  { key: 'location', label: 'Localização'         },
  { key: 'az',       label: 'A–Z'                 },
];

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

function Barbershops({ searchTerm, favoriteIds = [], onToggleFavorite }) {
  const [barbershops, setBarbershops]   = useState([]);
  const [loading, setLoading]           = useState(true);
  const [sortKey, setSortKey]           = useState('recent');
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const [recentIds, setRecentIds]       = useState([]);
  const [page, setPage]                 = useState(1);
  const [userLocation, setUserLocation] = useState(
    () => JSON.parse(sessionStorage.getItem('userLocation') || 'null')
  );
  const dropdownRef = useRef(null);

  // Carrega todas as barbearias uma única vez — ordenação é feita client-side
  useEffect(() => {
    const fetchAll = async () => {
      setLoading(true);
      try {
        const shops = await getBarbershops();
        const shopsWithServices = await Promise.all(
          shops.map(async (shop) => {
            const services = await getShopServices(shop.id);
            return { ...shop, services };
          })
        );
        setBarbershops(shopsWithServices);
      } finally {
        setLoading(false);
      }
    };
    fetchAll();
  }, []);

  // IDs de barbearias do histórico de agendamentos do cliente (mais recentes primeiro)
  useEffect(() => {
    getMyRecentBarbershopIds().then(setRecentIds).catch(() => {});
  }, []);

  // Fecha dropdown ao clicar fora
  useEffect(() => {
    const handler = (e) => {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target)) {
        setDropdownOpen(false);
      }
    };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, []);

  const handleSortSelect = (key) => {
    setDropdownOpen(false);
    setPage(1);

    if (key === 'location') {
      if (userLocation) {
        setSortKey('location');
        return;
      }
      navigator.geolocation.getCurrentPosition(
        ({ coords }) => {
          const loc = { lat: coords.latitude, lng: coords.longitude };
          sessionStorage.setItem('userLocation', JSON.stringify(loc));
          setUserLocation(loc);
          setSortKey('location');
          toast.success('Localização obtida! Exibindo barbearias mais próximas.');
        },
        () => toast.warn('Permissão de localização negada. Não foi possível ordenar por distância.')
      );
      return;
    }

    setSortKey(key);
  };

  const sorted = useMemo(() => {
    let list = [...barbershops];

    if (searchTerm) {
      const term = searchTerm.toLowerCase();
      list = list.filter((s) => s.name.toLowerCase().includes(term));
    }

    if (sortKey === 'az') {
      list.sort((a, b) => a.name.localeCompare(b.name, 'pt-BR'));

    } else if (sortKey === 'location') {
      if (userLocation) {
        list.sort((a, b) => {
          const dA = a.latitude != null && a.longitude != null
            ? haversineKm(userLocation.lat, userLocation.lng, a.latitude, a.longitude)
            : Infinity;
          const dB = b.latitude != null && b.longitude != null
            ? haversineKm(userLocation.lat, userLocation.lng, b.latitude, b.longitude)
            : Infinity;
          return dA - dB;
        });
      }

    } else {
      // 'recent' — agendadas mais recentemente primeiro; sem histórico mantém ordem original
      const hasHistory = recentIds.length > 0;
      if (hasHistory) {
        list.sort((a, b) => {
          const iA = recentIds.indexOf(a.id);
          const iB = recentIds.indexOf(b.id);
          if (iA === -1 && iB === -1) return a.name.localeCompare(b.name, 'pt-BR');
          if (iA === -1) return 1;
          if (iB === -1) return -1;
          return iA - iB;
        });
      }
    }

    return list;
  }, [barbershops, searchTerm, sortKey, userLocation, recentIds]);

  const paginated = sorted.slice(0, page * PAGE_SIZE);
  const hasMore   = paginated.length < sorted.length;

  const skeletons = Array.from({ length: 6 });

  return (
    <div>
      {/* Barra de ordenação */}
      <div className={Styles.sort_bar}>
        <div className={Styles.sort_dropdown_wrap} ref={dropdownRef}>
          <button
            className={Styles.sort_button}
            onClick={() => setDropdownOpen((v) => !v)}
            aria-haspopup="listbox"
            aria-expanded={dropdownOpen}
          >
            <span className={Styles.sort_icon}>↕</span>
            Ordenar
            <span className={`${Styles.sort_chevron} ${dropdownOpen ? Styles.sort_chevron_open : ''}`}>▾</span>
          </button>

          {dropdownOpen && (
            <ul className={Styles.sort_menu} role="listbox">
              {SORT_OPTIONS.map((opt) => (
                <li
                  key={opt.key}
                  className={`${Styles.sort_item} ${sortKey === opt.key ? Styles.sort_item_active : ''}`}
                  onClick={() => handleSortSelect(opt.key)}
                  onKeyDown={(e) => e.key === 'Enter' && handleSortSelect(opt.key)}
                  role="option"
                  tabIndex={0}
                  aria-selected={sortKey === opt.key}
                >
                  {opt.label}
                  {sortKey === opt.key && <span className={Styles.sort_check}>✓</span>}
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>

      {/* Grid de barbearias */}
      <div className={Styles.barbershops_container}>
        {loading ? (
          skeletons.map((_, i) => (
            <div key={i} className={Styles.skeleton_card}>
              <div className={Styles.skeleton_thumb} />
              <div className={Styles.skeleton_body}>
                <div className={`${Styles.skeleton_line} ${Styles.skeleton_line_medium}`} />
                <div className={`${Styles.skeleton_line} ${Styles.skeleton_line_long}`} />
                <div className={`${Styles.skeleton_line} ${Styles.skeleton_line_short}`} />
              </div>
            </div>
          ))
        ) : paginated.length > 0 ? (
          paginated.map((shop) => {
            let distanceKm = shop.distanceKm ?? null;
            if (sortKey === 'location' && userLocation && shop.latitude != null && shop.longitude != null) {
              distanceKm = haversineKm(userLocation.lat, userLocation.lng, shop.latitude, shop.longitude);
            }
            return (
              <Container_Barbericons
                key={shop.id}
                name={shop.name}
                address={shop.address}
                image={shop.logoUrl || "./barbershop.jpg"}
                id={shop.id}
                isFavorite={favoriteIds.includes(shop.id)}
                onToggleFavorite={onToggleFavorite}
                rating={shop.averageRating}
                reviewsCount={shop.reviewsCount}
                services={shop.services || []}
                distanceKm={distanceKm}
              />
            );
          })
        ) : (
          <p className={Styles.empty_message}>Nenhuma barbearia encontrada.</p>
        )}
      </div>

      {/* Carregar mais */}
      {!loading && hasMore && (
        <div className={Styles.pagination_wrap}>
          <button className={Styles.load_more_btn} onClick={() => setPage((p) => p + 1)}>
            Carregar mais
          </button>
        </div>
      )}
    </div>
  );
}

export default Barbershops
